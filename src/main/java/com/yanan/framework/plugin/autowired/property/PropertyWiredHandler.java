package com.yanan.framework.plugin.autowired.property;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yanan.framework.plugin.annotations.Register;
import com.yanan.framework.plugin.annotations.Support;
import com.yanan.framework.plugin.definition.RegisterDefinition;
import com.yanan.framework.plugin.exception.PluginRuntimeException;
import com.yanan.framework.plugin.handler.FieldHandler;
import com.yanan.framework.plugin.handler.InstanceHandler;
import com.yanan.framework.plugin.handler.InvokeHandler;
import com.yanan.framework.plugin.handler.HandlerSet;
import com.yanan.framework.plugin.handler.MethodHandler;
import com.yanan.utils.reflect.AppClassLoader;
import com.yanan.utils.reflect.ParameterUtils;

@Support(Property.class)
@Register(attribute = "*", description = "Property文件的属性的注入")
public class PropertyWiredHandler implements InvokeHandler, InstanceHandler, FieldHandler {
	private Logger log = LoggerFactory.getLogger(
			PropertyWiredHandler.class);

	@Override
	public Object around(MethodHandler methodHandler) throws Throwable{
		// 遍历所有Field
		Property property;
		String propertyName;
		String propertyValue;
		Parameter[] parameters = methodHandler.getMethod().getParameters();
		Object[] arguments = methodHandler.getParameters();
		for (int i = 0; i < parameters.length; i++) {
			Parameter parameter = parameters[i];
			property = parameter.getAnnotation(Property.class);
			if (property != null) {
				propertyName = property.value();
				if (property != null) {
					propertyName = property.value();
					if (propertyName.equals("")) {
						propertyName = parameter.getName();
					}
					propertyValue = PropertyManager.getInstance().getProperty(propertyName);
					try {
						if (propertyValue == null && property.defaultValue().equals("")) {
							throw new RuntimeException("failed to autowired parameter ! property name \"" + propertyName
									+ "\" value is null\r\n at class " + methodHandler.getPlugsProxy().getProxyClass()
									+ "\r\n at parameter " + parameter.getName());
						}
						if (propertyValue == null && !property.defaultValue().equals("")) {
							propertyValue = property.defaultValue();
						}
						if (arguments[i] == null) {
							arguments[i] = parameter.getType().isArray() ? ParameterUtils
									.parseBaseTypeArray(parameter.getType(), propertyValue.split(","), null)
									: ParameterUtils.parseBaseType(parameter.getType(), propertyValue, null);
						}
					} catch (Exception e) {
						log.error("Error to process property \r\nat class:"
								+ methodHandler.getPlugsProxy().getProxyClass() + "\r\nat parameter:" + parameter
								+ "\r\nat property:" + propertyName + "\r\nat property value:" + propertyValue, e);
					}
				}
			}
		}
		return methodHandler.invoke();
	}
	
	@Override
	public void preparedField(RegisterDefinition registerDefinition, Object proxy, Object target,
			HandlerSet handlerSet, Field field) {
		Property property = handlerSet.getAnnotation(Property.class);
		String propertyName = null;
		String propertyValue = null;
		propertyName = property.value();
		if (propertyName.equals(""))
			propertyName = field.getName();
		try {
			propertyValue = PropertyManager.getInstance().getProperty(propertyName);
			if (propertyValue == null &&property!=null&& !property.defaultValue().equals("")) {
				propertyValue = property.defaultValue();
			}
			if (propertyValue == null && property.required()) {
				throw new RuntimeException("the required property '"+propertyName+"' value is null");
			}
			new AppClassLoader(target).set(field,
					field.getType().isArray()
							? ParameterUtils.parseBaseTypeArray(field.getType(), propertyValue.split(","), null)
							: propertyValue);
		} catch (Exception e) {
			if(property.required())
				throw new PropertyAutowiredFailedException("failed to autowired parameter ! property name \"" + propertyName
					+ "\" value is null\r\nat class : " + registerDefinition.getRegisterClass().getName()
					+ "\r\nat field : " + field.getName(),e);
		}
	}

	@Override
	public void before(RegisterDefinition registerDefinition, Class<?> plugClass, Constructor<?> constructor,
			Object... args) {
		Property property;
		String propertyName;
		String propertyValue;
		Parameter[] parameters = constructor.getParameters();
		for (int i = 0; i < parameters.length; i++) {
			Parameter parameter = parameters[i];
			property = parameter.getAnnotation(Property.class);
			if (property != null) {
				propertyName = property.value();
				if (propertyName.equals("")) {
					propertyName = parameter.getName();
				}
				propertyValue = PropertyManager.getInstance().getProperty(propertyName);
				try {
					if (propertyValue == null && property.defaultValue().equals("")) {
						throw new RuntimeException("failed to autowired parameter ! property name \"" + propertyName
								+ "\" value is null\r\n at class : " + registerDefinition.getRegisterClass().getName()
								+ "\" value is null\r\n at constructor : "
								+ registerDefinition.getRegisterClass().getName() + "\r\n at parameter : "
								+ parameter.getName());
					}
					if (propertyValue == null && !property.defaultValue().equals("")) {
						propertyValue = property.defaultValue();
					}
					args[i] = parameter.getType().isArray()
							? ParameterUtils.parseBaseTypeArray(parameter.getType(), propertyValue.split(","), null)
							: ParameterUtils.parseBaseType(parameter.getType(), propertyValue, null);
				} catch (Exception e) {
					log.error("Error to process property ! \r\nat class : "
							+ registerDefinition.getRegisterClass().getName()
							+ "\" value is null\r\n at constructor : "
							+ registerDefinition.getRegisterClass().getName() + "\r\nat parameter : "
							+ parameter.getName() + "\r\nat property : " + propertyName + "\r\nat property value : "
							+ propertyValue, e);
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