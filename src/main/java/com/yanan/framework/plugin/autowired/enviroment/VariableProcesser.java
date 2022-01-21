package com.yanan.framework.plugin.autowired.enviroment;

import java.lang.ref.WeakReference;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import com.yanan.framework.plugin.PlugsFactory;
import com.yanan.utils.reflect.ParameterUtils;
import com.yanan.utils.reflect.ReflectUtils;
import com.yanan.utils.reflect.TypeToken;
import com.yanan.utils.string.PathMatcher;
import com.yanan.utils.string.PathMatcher.Token;
import com.yanan.utils.string.StringUtil;

public class VariableProcesser {
	static class VaribleMapper{
		private Map<Field,List<WeakReference<Object>>> mapping = new HashMap<>();
		private String name;
		public void addMapper(Field field,Object instance) {
			List<WeakReference<Object>> list = mapping.get(field);
			if(list == null) {
				list = new ArrayList<>();
				mapping.put(field, list);
			}
			list.add(new WeakReference<Object>(instance));
		}
		public void update(Object value) {
			if(mapping.isEmpty()) {
				map.remove(name);
				return;
			}
			Iterator<Entry<Field, List<WeakReference<Object>>>> iterator = mapping.entrySet().iterator();
			while(iterator.hasNext()) {
				Entry<Field, List<WeakReference<Object>>> entry = iterator.next();
				Field field =entry.getKey();
				List<WeakReference<Object>> list = entry.getValue();
				Object realValue = parseVaiable(field.getType(), value, field);
				for (Iterator<WeakReference<Object>> citerator = list.iterator(); citerator.hasNext();) {
					WeakReference<Object> phantomReference = citerator.next();
					Object instance;
					if((instance = phantomReference.get()) == null) {
						citerator.remove();
						continue;
					}
					
					try {
						ReflectUtils.setFieldValue(field, instance, realValue);
					} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
						throw new VariableAutowiredFailedException(e);
					}
				}
				if(list.isEmpty()) {
					iterator.remove();
				}
			}
			if(mapping.isEmpty()) {
				map.remove(name);
				return;
			}
		}
	}
	private static Map<String,VaribleMapper> map = new HashMap<>();
	
	public synchronized static void addToUpdateMap(String name,Object instace,Field field,Variable variable) {
		VaribleMapper varialbMapper = map.get(name);
		if(varialbMapper == null) {
			varialbMapper = new VaribleMapper();
			varialbMapper.name = name;
			map.put(name, varialbMapper);
		}
		varialbMapper.addMapper(field, instace);
	}
	public synchronized static void updateVaiable(String name) {
		VaribleMapper variableMapper = map.get(name);
		if(variableMapper == null)
			return;
		Object value = getVariable(name,false, null);
		variableMapper.update(value);
	}
	private static Object getVariableValue(String name) {
		Object value = null;
		List<VariableSupporter> variableSupporters = PlugsFactory.getPluginsInstanceList(VariableSupporter.class);
		for (VariableSupporter variableSupporter : variableSupporters) {
			value = variableSupporter.getVariableValue(name);
			if (value != null)
				break;
		}
		return value;
	}

	private static Object getVariable(String name, boolean required, String defaultValue) {
		Object value = null;
		List<Token> tokens = PathMatcher.getPathMatcher(name).getTokens();
		if (tokens.size() > 1) {
			for (Token token : tokens) {
				if (token.getType() == Token.TYPE_VARIABLE) {
					Object tempValue = getVariableValue(token.getName());
					if (tempValue == null) {
						if (required) {
							throw new VariableAutowiredFailedException(
									"could not found token value [" + token.getName() + "] at " + name);
						}
						break;
					}
					if (tempValue instanceof ConfigValue)
						tempValue = ((ConfigValue) tempValue).unwrapped();
					value = value == null ? tempValue : value + String.valueOf(tempValue);
				} else {
					value = value == null ? token.getToken() : value + token.getToken();
				}
			}
		} else {
			Token token = tokens.get(0);
			value = getVariableValue(token.getType() == Token.TYPE_VARIABLE ? token.getName() : name);
			if (value == null) {
				if (!StringUtil.equals(defaultValue, "")) {
					value = defaultValue;
				}
			}
		}
		if (value == null && required) {
			throw new VariableAutowiredFailedException("could not found token value at " + name);
		}
		return value;
	}

	public static <T> T getVariable(String name, Class<T> targetType, Variable variable, Field annotation,
			Object instance) {
		if (variable.autoRefresh() && annotation.getClass().equals(Field.class)) {
			addToUpdateMap(name,instance,(Field)annotation,variable);
		}
		return getVariable(name,targetType,variable,annotation);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getVariable(String name, Class<T> targetType, Variable variable, AnnotatedElement annotation) {
		Object value = getVariable(name, variable.required(), variable.defaultValue());
		value = parseVaiable(targetType, value, annotation);
		return (T) value;
	}
	private static Object parseVaiable(Class<?> type, Object value, AnnotatedElement annotations) {
		Adapter adapter = annotations.getAnnotation(Adapter.class);
		String attr;
		String suffix;
		Object tempValue = value;
		if (value == null)
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

	private static Object parseConfigType(Class<?> type, Object configValue) {
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
				values = ParameterUtils.parseBaseTypeArray(type, strValues, null);
				return values;
			}
		}
		if (type.equals(String.class) || type.equals(int.class) || type.equals(long.class) || type.equals(short.class)
				|| type.equals(boolean.class) || type.equals(double.class)) {
			return ((ConfigValue) configValue).unwrapped();
		}
		return configValue;
	}
}
