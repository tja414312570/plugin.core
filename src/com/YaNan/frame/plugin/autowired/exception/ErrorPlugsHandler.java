package com.YaNan.frame.plugin.autowired.exception;


import java.lang.reflect.Constructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.YaNan.frame.plugin.PluginRuntimeException;
import com.YaNan.frame.plugin.RegisterDescription;
import com.YaNan.frame.plugin.annotations.Register;
import com.YaNan.frame.plugin.annotations.Support;
import com.YaNan.frame.plugin.handler.InstanceHandler;
import com.YaNan.frame.plugin.handler.InvokeHandler;
import com.YaNan.frame.plugin.handler.MethodHandler;
import com.YaNan.frame.utils.reflect.AppClassLoader;
import com.YaNan.frame.utils.reflect.cache.ClassHelper;

@Support(Error.class)
@Register(attribute="*",priority=Integer.MAX_VALUE)
public class ErrorPlugsHandler implements InvokeHandler,InstanceHandler{
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
		if(error!=null&&(AppClassLoader.implementOf(e.getClass(), error.exception())
				||AppClassLoader.extendsOf(e.getClass(), error.exception()))){
			if(error.recorder()){
				StringBuilder sb = new StringBuilder();
				if(methodHandler.getParameters()!=null&&methodHandler.getParameters().length>0){
					for(Object par: methodHandler.getParameters()){
						sb.append("[").append(par).append("]  ");
					}
				}else sb.append("Void");
				log.error("An error occurred  \r\n\t\tat method :"
			+methodHandler.getMethod()+"\r\n\t\tparameter :"
						+sb.toString(), e);;
			}
			if(!error.value().equals(""))
				methodHandler.interrupt(error.value());
		}
		e.printStackTrace();
	}

	@Override
	public void before(RegisterDescription registerDescription, Class<?> plugClass, Constructor<?> constructor,
			Object... args) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void after(RegisterDescription registerDescription, Class<?> plugClass, Constructor<?> constructor,
			Object proxyObject, Object... args) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void exception(RegisterDescription registerDescription, Class<?> plug, Constructor<?> constructor,
			Object proxyObject, PluginRuntimeException throwable, Object... args) {
		Error error = constructor.getAnnotation(Error.class);
		if(error==null)
			error = ClassHelper.getClassHelper(plug).getAnnotation(Error.class);
		if(error!=null&&(AppClassLoader.implementOf(throwable.getClass(), error.exception())
				||AppClassLoader.extendsOf(throwable.getClass(), error.exception()))){
			if(error.recorder()){
				StringBuilder sb = new StringBuilder();
				if(args!=null&&args.length>0){
					for(Object par: args){
						sb.append("[").append(par).append("]  ");
					}
				}else sb.append("Void");
				log.error("An error occurred  \r\n\t\tat method :"
			+constructor+"\r\n\t\tnparameter :"
						+sb.toString(), throwable);;
			}
		}
		throwable.interrupt();
		throwable.printStackTrace();
	}
}
