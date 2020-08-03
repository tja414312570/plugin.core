package com.yanan.frame.plugin.autowired.enviroment;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.text.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import com.yanan.frame.plugin.Environment;
import com.yanan.frame.plugin.annotations.Register;
import com.yanan.frame.plugin.annotations.Support;
import com.yanan.frame.plugin.autowired.property.PropertyManager;
import com.yanan.frame.plugin.definition.RegisterDefinition;
import com.yanan.frame.plugin.exception.PluginRuntimeException;
import com.yanan.frame.plugin.handler.FieldHandler;
import com.yanan.frame.plugin.handler.InstanceHandler;
import com.yanan.frame.plugin.handler.InvokeHandler;
import com.yanan.frame.plugin.handler.InvokeHandlerSet;
import com.yanan.frame.plugin.handler.MethodHandler;
import com.yanan.utils.reflect.AppClassLoader;
import com.yanan.utils.reflect.ParameterUtils;
import com.yanan.utils.reflect.ReflectUtils;

@Support(Variable.class)
@Register(attribute = "*", description = "环境变量的属性的注入")
public class VariableWiredHandler implements InvokeHandler, InstanceHandler, FieldHandler {
	Environment environment;
	public VariableWiredHandler() {
		environment = Environment.getEnviroment();
	}
	private Logger log = LoggerFactory.getLogger(
			VariableWiredHandler.class);

		public Object getVariable(String name) {
			Object value = environment.getVariable(name);
			if(value == null) {
				Config config = environment.getConfigure();
				config.allowKeyNull();
				System.out.println(config);
				if(!config.isList("name")) {
					value = config.getValue(name);
				}else {
					value = config.getValueList(name);
				}
				
			}
			if(value == null) {
				value = PropertyManager.getInstance().getProperty(name);
			}
			return value;
		}
		
	@Override
	public void before(MethodHandler methodHandler) {
		// 遍历所有Field
		Variable variable;
		String name;
		Object value;
		Parameter[] parameters = methodHandler.getMethod().getParameters();
		Object[] arguments = methodHandler.getParameters();
		for (int i = 0; i < parameters.length; i++) {
			Parameter parameter = parameters[i];
			variable = parameter.getAnnotation(Variable.class);
			if (variable != null) {
				name = variable.value();
				if (variable != null) {
					name = variable.value();
					if (name.equals("")) {
						name = parameter.getName();
					}
					value = getVariable(name);
					try {
						if (value == null && variable.defaultValue().equals("")) {
							log.warn("failed to autowired parameter ! variable name \"" + name
									+ "\" value is null\r\n at class " + methodHandler.getPlugsProxy().getProxyClass()
									+ "\r\n at parameter " + parameter.getName());
						}
						if (value == null && !variable.defaultValue().equals("")) {
							value = variable.defaultValue();
						}
						if (arguments[i] == null) {
							arguments[i] = parseVaiable(parameter.getType(),value);
						}
					} catch (Exception e) {
						throw new VariableAutowiredFailedException("Error to process variable \r\nat class:"
								+ methodHandler.getPlugsProxy().getProxyClass() + "\r\nat parameter:" + parameter
								+ "\r\nat variable:" + name + "\r\nat variable value:" + value, e);
					}
				}
			}
		}
	}
	public static Object parseConfigType(Class<?> type, Object configValue) {
		if(type.isArray()) {
				Class<?> baseType = ReflectUtils.getArrayType(type);
				if(ReflectUtils.implementsOf(configValue.getClass(), ConfigList.class)) {
					ConfigList configList = (ConfigList) configValue;
					if(baseType.equals(String.class)||baseType.equals(int.class)
							||baseType.equals(long.class)||baseType.equals(boolean.class)
							||baseType.equals(double.class)) {
						return configList.unwrapped();
					}
				}else if (((ConfigValue) configValue).valueType().equals(ConfigValueType.STRING)) {
					String[] strValues = ((String)((ConfigValue) configValue).unwrapped()).split(",");
					Object values;
					try {
						values = ParameterUtils.parseBaseTypeArray(type, strValues, null);
						return values;
					} catch (ParseException e) {
						e.printStackTrace();
					}
				} 
		}
		if(type.equals(String.class) || type.equals(int.class)
				||type.equals(long.class) || type.equals(short.class)
				||type.equals(boolean.class) || type.equals(double.class)) {
			return ((ConfigValue)configValue).unwrapped();
		}
		return configValue;
	}
	private Object parseVaiable(Class<?> type, Object value) throws ParseException {
		System.out.println(value);
		System.out.println(value.getClass());
		if(ReflectUtils.implementsOf(value.getClass(), ConfigValue.class)) {
			value = parseConfigType(type,value);
			System.out.println(value);
		}
		if(value.getClass().equals(String.class)) {
			value = type.isArray() ? ParameterUtils
					.parseBaseTypeArray(type, ((String)value).split(","), null)
					: ParameterUtils.parseBaseType(type, ((String)value), null);
		}
		return value;
	}
	@Override
	public void after(MethodHandler methodHandler) {
	}

	@Override
	public void error(MethodHandler methodHandler, Throwable e) {
	}

	@Override
	public void preparedField(RegisterDefinition registerDefinition, Object proxy, Object target,
			InvokeHandlerSet handlerSet, Field field) {
		System.out.println("llllll");
		Variable variable = handlerSet.getAnnotation(Variable.class);
		String name = null;
		Object value = null;
		name =  variable.value();
		if (name.equals(""))
			name = field.getName();
		try {
			System.out.println(name);
			value = getVariable(name);
			if (value == null &&variable!=null&& !variable.defaultValue().equals("")) {
				value = variable.defaultValue();
			}
			if (value == null && variable.required()) {
				throw new VariableAutowiredFailedException("the required variable '"+name+"' value is null");
			}
			new AppClassLoader(target).set(field,
					parseVaiable(field.getType(),value));
			
		} catch (Exception e) {
			if(variable.required())
				throw new VariableAutowiredFailedException("failed to autowired parameter ! variable name \"" + name
					+ "\" value is null\r\nat class : " + registerDefinition.getRegisterClass().getName()
					+ "\r\nat field : " + field.getName(),e);
		}
	}

	@Override
	public void before(RegisterDefinition registerDefinition, Class<?> plugClass, Constructor<?> constructor,
			Object... args) {
		Variable variable;
		String name;
		String value;
		Parameter[] parameters = constructor.getParameters();
		for (int i = 0; i < parameters.length; i++) {
			Parameter parameter = parameters[i];
			variable = parameter.getAnnotation(Variable.class);
			if (variable != null) {
				name = variable.value();
				if (name.equals("")) {
					name = parameter.getName();
				}
				value =  environment.getVariable(name);
				try {
					if (value == null && variable.defaultValue().equals("")) {
						log.warn("failed to autowired parameter ! variable name \"" + name
								+ "\" value is null\r\n at class : " + registerDefinition.getRegisterClass().getName()
								+ "\" value is null\r\n at constructor : "
								+ registerDefinition.getRegisterClass().getName() + "\r\n at parameter : "
								+ parameter.getName());
					}
					if (value == null && !variable.defaultValue().equals("")) {
						value = variable.defaultValue();
					}
					if (value == null && variable.required()) {
						throw new VariableAutowiredFailedException("the required vaiable '"+name+"' value is null");
					}
					args[i] = parameter.getType().isArray()
							? ParameterUtils.parseBaseTypeArray(parameter.getType(), value.split(","), null)
							: ParameterUtils.parseBaseType(parameter.getType(), value, null);
				} catch (Exception e) {
					log.error("Error to process variable ! \r\nat class : "
							+ registerDefinition.getRegisterClass().getName()
							+ "\" value is null\r\n at constructor : "
							+ registerDefinition.getRegisterClass().getName() + "\r\nat parameter : "
							+ parameter.getName() + "\r\nat variable : " + name + "\r\nat variable value : "
							+ value, e);
				}
			}
		}
	}

	@Override
	public void after(RegisterDefinition registerDefinition, Class<?> plugClass, Constructor<?> constructor,
			Object proxyObject, Object... args) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void exception(RegisterDefinition registerDefinition, Class<?> plug, Constructor<?> constructor,
			Object proxyObject, PluginRuntimeException throwable, Object... args) {
		// TODO Auto-generated method stub
		
	}

}
