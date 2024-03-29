package com.yanan.framework.plugin.autowired.exception;

import java.lang.reflect.Constructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yanan.framework.plugin.annotations.Register;
import com.yanan.framework.plugin.annotations.Support;
import com.yanan.framework.plugin.definition.RegisterDefinition;
import com.yanan.framework.plugin.exception.PluginRuntimeException;
import com.yanan.framework.plugin.handler.InstanceHandler;
import com.yanan.framework.plugin.handler.InvokeHandler;
import com.yanan.framework.plugin.handler.MethodHandler;
import com.yanan.utils.reflect.ReflectUtils;
import com.yanan.utils.reflect.cache.ClassHelper;

@Support(Error.class)
@Register(attribute = "*", priority = Integer.MAX_VALUE)
public class ErrorPlugsHandler implements InvokeHandler, InstanceHandler {
	private Logger log = LoggerFactory.getLogger(ErrorPlugsHandler.class);

	@Override
	public Object around(MethodHandler methodHandler) throws Throwable {
		try {
			return methodHandler.invoke();
		}catch(Exception e) {
			Error error = methodHandler.getHandlerSet().getAnnotation(Error.class);
			if (error != null && (ReflectUtils.implementOf(e.getClass(), error.exception())
					|| ReflectUtils.extendsOf(e.getClass(), error.exception()))) {
				if (error.recorder()) {
					StringBuilder sb = new StringBuilder();
					if (methodHandler.getParameters() != null && methodHandler.getParameters().length > 0) {
						for (Object par : methodHandler.getParameters()) {
							sb.append("[").append(par).append("]  ");
						}
					} else
						sb.append("Void");
					log.error("An error occurred  \r\n\t\tat method :" + methodHandler.getMethod() + "\r\n\t\tparameter :"
							+ sb.toString(), e);
					;
				}
				
			}
			throw e;
		}
		
	}

	@Override
	public void before(RegisterDefinition registerDefinition, Class<?> plugClass, Constructor<?> constructor,
			Object... args) {
		// TODO Auto-generated method stub

	}

	@Override
	public void after(RegisterDefinition registerDefinition, Class<?> plugClass, Constructor<?> constructor,
			Object proxyObject, Object... args) {
		// TODO Auto-generated method stub

	}

	@Override
	public void exception(RegisterDefinition registerDefinition, Class<?> plug, Constructor<?> constructor,
			Object proxyObject, PluginRuntimeException throwable, Object... args) {
		Error error = constructor.getAnnotation(Error.class);
		if (error == null)
			error = ClassHelper.getClassHelper(plug).getAnnotation(Error.class);
		if (error != null && (ReflectUtils.implementOf(throwable.getClass(), error.exception())
				|| ReflectUtils.extendsOf(throwable.getClass(), error.exception()))) {
			if (error.recorder()) {
				StringBuilder sb = new StringBuilder();
				if (args != null && args.length > 0) {
					for (Object par : args) {
						sb.append("[").append(par).append("]  ");
					}
				} else
					sb.append("Void");
				log.error(
						"An error occurred  \r\n\t\tat method :" + constructor + "\r\n\t\tnparameter :" + sb.toString(),
						throwable);
				;
			}
		}
		throwable.interrupt();
		throwable.printStackTrace();
	}
}