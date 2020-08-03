package com.yanan.frame.plugin.autowired.exception;

import java.lang.reflect.Constructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yanan.frame.plugin.annotations.Register;
import com.yanan.frame.plugin.annotations.Support;
import com.yanan.frame.plugin.definition.RegisterDefinition;
import com.yanan.frame.plugin.exception.PluginRuntimeException;
import com.yanan.frame.plugin.handler.InstanceHandler;
import com.yanan.frame.plugin.handler.InvokeHandler;
import com.yanan.frame.plugin.handler.MethodHandler;
import com.yanan.utils.reflect.ReflectUtils;
import com.yanan.utils.reflect.cache.ClassHelper;

@Support(Error.class)
@Register(attribute = "*", priority = Integer.MAX_VALUE)
public class ErrorPlugsHandler implements InvokeHandler, InstanceHandler {
	private Logger log = LoggerFactory.getLogger(ErrorPlugsHandler.class);

	@Override
	public void before(MethodHandler methodHandler) {
	}

	@Override
	public void after(MethodHandler methodHandler) {
	}

	@Override
	public void error(MethodHandler methodHandler, Throwable e) {
		Error error = methodHandler.getInvokeHandlerSet().getAnnotation(Error.class);
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
			if (!error.value().equals(""))
				methodHandler.interrupt(error.value());
		}
		e.printStackTrace();
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
