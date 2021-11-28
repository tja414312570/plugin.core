package com.yanan.framework.plugin.autowired.enviroment;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yanan.framework.plugin.annotations.Register;
import com.yanan.framework.plugin.annotations.Support;
import com.yanan.framework.plugin.definition.RegisterDefinition;
import com.yanan.framework.plugin.handler.FieldHandler;
import com.yanan.framework.plugin.handler.HandlerSet;
import com.yanan.framework.plugin.handler.InstanceHandler;
import com.yanan.framework.plugin.handler.InvokeHandler;
import com.yanan.framework.plugin.handler.MethodHandler;
import com.yanan.utils.reflect.ReflectUtils;

@Support(Variable.class)
@Register(attribute = "*", description = "环境变量的属性的注入")
public class VariableWiredHandler implements InvokeHandler, InstanceHandler, FieldHandler {

	private Logger log = LoggerFactory.getLogger(VariableWiredHandler.class);

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
			value = VariableProcesser.getVariable(name, field.getType(), variable, field,target);
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

	private Object getParameterValue(Parameter parameter, Object arg, String location) {
		Variable variable = parameter.getAnnotation(Variable.class);
		if (variable == null)
			return arg;

		String name = variable.value();
		if (name.equals("")) {
			name = parameter.getName();
		}
		try {
			arg = VariableProcesser.getVariable(name, parameter.getType(), variable, parameter);
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