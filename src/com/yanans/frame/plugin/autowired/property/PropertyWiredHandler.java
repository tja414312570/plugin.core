package com.yanan.frame.plugin.autowired.property;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yanan.frame.plugin.annotations.Register;
import com.yanan.frame.plugin.annotations.Support;
import com.yanan.frame.plugin.definition.RegisterDefinition;
import com.yanan.frame.plugin.exception.PluginRuntimeException;
import com.yanan.frame.plugin.handler.FieldHandler;
import com.yanan.frame.plugin.handler.InstanceHandler;
import com.yanan.frame.plugin.handler.InvokeHandler;
import com.yanan.frame.plugin.handler.InvokeHandlerSet;
import com.yanan.frame.plugin.handler.MethodHandler;
import com.yanan.utils.reflect.AppClassLoader;

@Support(Property.class)
@Register(attribute = "*", description = "Property文件的属性的注入")
public class PropertyWiredHandler implements InvokeHandler, InstanceHandler, FieldHandler {
	private Logger log = LoggerFactory.getLogger(
			PropertyWiredHandler.class);

	@Override
	public void before(MethodHandler methodHandler) {
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
							arguments[i] = parameter.getType().isArray() ? AppClassLoader
									.parseBaseTypeArray(parameter.getType(), propertyValue.split(","), null)
									: AppClassLoader.parseBaseType(parameter.getType(), propertyValue, null);
						}
					} catch (Exception e) {
						log.error("Error to process property \r\nat class:"
								+ methodHandler.getPlugsProxy().getProxyClass() + "\r\nat parameter:" + parameter
								+ "\r\nat property:" + propertyName + "\r\nat property value:" + propertyValue, e);
					}
				}
			}
		}
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
							? AppClassLoader.parseBaseTypeArray(field.getType(), propertyValue.split(","), null)
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
							? AppClassLoader.parseBaseTypeArray(parameter.getType(), propertyValue.split(","), null)
							: AppClassLoader.parseBaseType(parameter.getType(), propertyValue, null);
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
