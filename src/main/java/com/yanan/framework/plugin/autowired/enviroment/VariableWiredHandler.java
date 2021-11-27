package com.yanan.framework.plugin.autowired.enviroment;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.text.ParseException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import com.yanan.framework.plugin.Environment;
import com.yanan.framework.plugin.PlugsFactory;
import com.yanan.framework.plugin.annotations.Register;
import com.yanan.framework.plugin.annotations.Support;
import com.yanan.framework.plugin.autowired.property.PropertyManager;
import com.yanan.framework.plugin.definition.RegisterDefinition;
import com.yanan.framework.plugin.handler.FieldHandler;
import com.yanan.framework.plugin.handler.HandlerSet;
import com.yanan.framework.plugin.handler.InstanceHandler;
import com.yanan.framework.plugin.handler.InvokeHandler;
import com.yanan.framework.plugin.handler.MethodHandler;
import com.yanan.utils.reflect.ParameterUtils;
import com.yanan.utils.reflect.ReflectUtils;
import com.yanan.utils.reflect.TypeToken;
import com.yanan.utils.string.PathMatcher;
import com.yanan.utils.string.PathMatcher.Token;
import com.yanan.utils.string.StringUtil;

@Support(Variable.class)
@Register(attribute = "*", description = "环境变量的属性的注入")
public class VariableWiredHandler implements InvokeHandler, InstanceHandler, FieldHandler {
	Environment environment;

	public VariableWiredHandler() {
		environment = Environment.getEnviroment();
	}

	private Logger log = LoggerFactory.getLogger(VariableWiredHandler.class);

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

	@Override
	public void before(MethodHandler methodHandler) {
		Parameter[] parameters = methodHandler.getMethod().getParameters();
		Object[] arguments = methodHandler.getParameters();
		for (int i = 0; i < parameters.length; i++) {
			Parameter parameter = parameters[i];
			arguments[i] = getParameterValue(parameter, arguments[i],methodHandler.getMethod()
					.getDeclaringClass().getName()+"."+methodHandler.getMethod().getName());
		}
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

	private Object parseVaiable(Class<?> type, Object value, AnnotatedElement annotations) throws ParseException {
		Adapter adapter = annotations.getAnnotation(Adapter.class);
		String attr;
		String suffix;
		Object tempValue = value;
		if(value == null)
			throw new VariableAutowiredFailedException("variable not found!");
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
				log.debug("resouce adapter "+resourceAdapter+" process value ["+value+"] result is null at"+annotations);;
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

	@Override
	public void preparedField(RegisterDefinition registerDefinition, Object proxy, Object target, HandlerSet handlerSet,
			Field field) {
		Variable variable = handlerSet.getAnnotation(Variable.class);
		String name = null;
		Object value = null;
		name = variable.value();
		if (name.equals(""))
			name = field.getName();
		try {
			value = getVariable(name, variable);
			value = parseVaiable(field.getType(), value, field);
			if (value == null && variable.required()) {
				throw new VariableAutowiredFailedException("the required variable '" + name + "' value is null");
			}
			ReflectUtils.setFieldValue(field, target, value);
		} catch (Exception e) {
			if (variable.required())
				throw new VariableAutowiredFailedException(
						"failed to autowired parameter ! variable name \"" + name + "\" value is "+value+"\r\nat class : "
								+ registerDefinition.getRegisterClass().getName() + "\r\nat field : " + field.getName(),
						e);
		}
	}

	@Override
	public void before(RegisterDefinition registerDefinition, Class<?> plugClass, Constructor<?> constructor,
			Object... args) {
		Parameter[] parameters = constructor.getParameters();
		for (int i = 0; i < parameters.length; i++) {
			Parameter parameter = parameters[i];
			args[i] = getParameterValue(parameter, args[i],registerDefinition.getRegisterClass().getName()+"."+constructor.getName());
		}
	}
	@Override
	public void after(RegisterDefinition registerDefinition, Class<?> plugClass, Constructor<?> constructor,
			Object proxyObject, Object... args) {
		System.err.println("llll");
	}

	private Object getParameterValue(Parameter parameter, Object arg, String location) {
		Variable variable = parameter.getAnnotation(Variable.class);
		if (variable == null)
			return arg;

		String name = variable.value();
		if (name.equals("")) {
			name = parameter.getName();
		}
		try {
			arg = getVariable(name, variable);
			arg = parseVaiable(parameter.getType(), arg, parameter);
			if (arg == null && variable.required()) {
				throw new VariableAutowiredFailedException("the required variable '" + name + "' value is null");
			}
		} catch (Exception e) {
			if (variable.required())
				throw new VariableAutowiredFailedException(
						"failed to autowired parameter ! variable name \"" + name
						+ "\",\r\nat : " + location
						+ "\r\nat parameter : " + parameter.getName(), e);
			else {
				log.error(
						"Error to process variable ! \r\nat : " + location
								+ parameter.getName() + "\r\nat variable : " + name + "\r\nat variable value : " + arg,
						e);

			}
		}
		return arg;
	}
}