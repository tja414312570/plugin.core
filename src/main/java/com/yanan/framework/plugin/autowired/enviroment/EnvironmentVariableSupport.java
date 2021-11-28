package com.yanan.framework.plugin.autowired.enviroment;

import java.lang.reflect.AnnotatedElement;
import java.text.ParseException;
import java.util.List;

import org.slf4j.Logger;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import com.yanan.framework.plugin.Environment;
import com.yanan.framework.plugin.PlugsFactory;
import com.yanan.framework.plugin.annotations.Register;
import com.yanan.framework.plugin.annotations.Service;
import com.yanan.framework.plugin.autowired.property.PropertyManager;
import com.yanan.utils.reflect.ParameterUtils;
import com.yanan.utils.reflect.ReflectUtils;
import com.yanan.utils.reflect.TypeToken;
import com.yanan.utils.string.PathMatcher;
import com.yanan.utils.string.PathMatcher.Token;
import com.yanan.utils.string.StringUtil;

@Register
public class EnvironmentVariableSupport implements VariableSupporter{
	Environment environment;
	@Service
	private Logger logger ;
	public EnvironmentVariableSupport() {
		environment = Environment.getEnviroment();
	}
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getVariable(String name, Class<T> targetType,Variable variable, AnnotatedElement annotation) {
		try {
			Object value = getVariable(name, variable);
			value = parseVaiable(targetType, value, annotation);
			return (T) value;
		} catch (Exception e) {
			throw new RuntimeException("failed to process config value ["+name+"]"+annotation,e);
		}
	}
	public Object getVariableValue(String name) {
		Object value = environment.getVariable(name);
		if (value == null) {
			Config config = environment.getConfigure();
			config.allowKeyNull();
			if (!config.isList(name)) {
				value = config.getValue(name);
			} else {
				value = config.getValueList(name);
			}
		}
		if (value == null) {
			value = PropertyManager.getInstance().getProperty(name);
		}
		return value;
	}
	public Object getVariable(String name, Variable variable) {
		Object value = null;
		List<Token> tokens = PathMatcher.getPathMatcher(name).getTokens();
		if(tokens.size()>1) {
			for(Token token : tokens) {
				if(token.getType() == Token.TYPE_VARIABLE) {
					Object tempValue = getVariableValue(token.getName());
					if (tempValue == null && (!variable.required() || !StringUtil.equals(variable.defaultValue(), ""))) {
						value = variable.defaultValue();
						break;
					}
					if(tempValue instanceof ConfigValue)
						tempValue = ((ConfigValue)tempValue).unwrapped();
					value = value == null? tempValue:value+String.valueOf(tempValue);
				}else {
					value = value == null? token.getToken():value+token.getToken();
				}
			}
		}else {
			Token token = tokens.get(0);
			value = getVariableValue(token.getType() == Token.TYPE_VARIABLE?token.getName():name);
			if (value == null && (!variable.required() || !StringUtil.equals(variable.defaultValue(), ""))) {
				value = variable.defaultValue();
			}
		}
		return value;
	}
	
	public Object parseVaiable(Class<?> type, Object value, AnnotatedElement annotations) throws ParseException {
		Adapter adapter = annotations.getAnnotation(Adapter.class);
		String attr;
		String suffix;
		Object tempValue = value;
		if(value == null)
			return null;
		if (adapter == null) {
			suffix = "_" + type.getSimpleName();
			attr = value.getClass().getSimpleName() + suffix;
		} else {
			suffix = "_" + adapter.target()[0].getSimpleName();
			attr = adapter.input()[0].getSimpleName() + suffix;
		}
		ResourceAdapter<Object, Object> resourceAdapter = PlugsFactory
				.getPluginsInstanceByAttributeStrictAllowNull(new TypeToken<ResourceAdapter<Object, Object>>() {
				}.getTypeClass(), attr);
		if (resourceAdapter == null && value instanceof ConfigValue) {
			resourceAdapter = PlugsFactory
					.getPluginsInstanceByAttributeStrictAllowNull(new TypeToken<ResourceAdapter<Object, Object>>() {
					}.getTypeClass(), ((ConfigValue) value).unwrapped().getClass().getSimpleName() + suffix);
			tempValue = ((ConfigValue) tempValue).unwrapped();
		}
		if (resourceAdapter != null) {
			tempValue = resourceAdapter.parse(tempValue);
			if(tempValue == null)
				logger.debug("resouce adapter "+resourceAdapter+" process value ["+value+"] result is null at"+annotations);;
			return tempValue;
		}
		if (ReflectUtils.implementsOf(value.getClass(), ConfigValue.class)) {
			value = parseConfigType(type, value);
		}
		if (value.getClass().equals(String.class)) {
			value = type.isArray() ? ParameterUtils.parseBaseTypeArray(type, ((String) value).split(","), null)
					: ParameterUtils.parseBaseType(type, ((String) value), null);
		}
		return value;
	}
	public static Object parseConfigType(Class<?> type, Object configValue) {
		if (type.isArray()) {
			Class<?> baseType = ReflectUtils.getArrayType(type);
			if (ReflectUtils.implementsOf(configValue.getClass(), ConfigList.class)) {
				ConfigList configList = (ConfigList) configValue;
				if (baseType.equals(String.class) || baseType.equals(int.class) || baseType.equals(long.class)
						|| baseType.equals(boolean.class) || baseType.equals(double.class)) {
					return configList.unwrapped();
				}
			} else if (((ConfigValue) configValue).valueType().equals(ConfigValueType.STRING)) {
				String[] strValues = ((String) ((ConfigValue) configValue).unwrapped()).split(",");
				Object values;
				try {
					values = ParameterUtils.parseBaseTypeArray(type, strValues, null);
					return values;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}
		if (type.equals(String.class) || type.equals(int.class) || type.equals(long.class) || type.equals(short.class)
				|| type.equals(boolean.class) || type.equals(double.class)) {
			return ((ConfigValue) configValue).unwrapped();
		}
		return configValue;
	}

}
