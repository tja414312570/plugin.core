package com.yanan.frame.plugin.thread;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.test.EnviromentExecutorTest;
import com.yanan.frame.plugin.annotations.Register;
import com.yanan.frame.plugin.definition.RegisterDefinition;
import com.yanan.frame.plugin.exception.PluginRuntimeException;
import com.yanan.frame.plugin.handler.InstanceHandler;
import com.yanan.frame.plugin.handler.InvokeHandler;
import com.yanan.frame.plugin.handler.MethodHandler;
import com.yanan.utils.reflect.ReflectUtils;

@Register(signlTon=true)
public class ThreadLoacalClear implements InvokeHandler,InstanceHandler{
	public static boolean isRunable(Method method) {
		return (method.getName().equals("run") 
				&& Modifier.isPublic(method.getModifiers())
				&& method.getReturnType().equals(void.class)
				&& method.getParameterCount() == 0);
	}
	public static boolean isRunable(Class<?> clzz) {
		return ReflectUtils.implementsOf(clzz, Runnable.class);
	}
	@Override
	public void before(MethodHandler methodHandler) {
		if(isRunable(methodHandler.getMethod())) {
			EnviromentExecutorTest.preparedThreadLocal();
		}
	}
    public void remove(Object threadLocalMap) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, InvocationTargetException, NoSuchMethodException {
    	Object tab = ReflectUtils.getDeclaredFieldValue("table", threadLocalMap);
    	int len = Array.getLength(tab);
    	for(int i = 0;i < len;i++) {
    		Object e = Array.get(tab, i);
    		Object key;
    		if(e != null && (key = ReflectUtils.invokeDeclaredMethod(e, "get")) != null) {
    			System.out.println("清理:"+key+"-->"+i);
    			ReflectUtils.invokeDeclaredMethod(key, "clear");
    			ReflectUtils.invokeDeclaredMethod(threadLocalMap, "expungeStaleEntry",i);
    		}
    	}
    }
	@Override
	public void after(MethodHandler methodHandler) {
		if(isRunable(methodHandler.getMethod())) {
			try {
				
				Thread currentThread = Thread.currentThread();
				Object inheritableThreadLocals = ReflectUtils.getDeclaredFieldValue("inheritableThreadLocals", currentThread);
				if (inheritableThreadLocals != null) {
					remove(inheritableThreadLocals);
				} 
				Object threadLocals = ReflectUtils.getDeclaredFieldValue("threadLocals", currentThread);
				if (threadLocals != null) {
					remove(threadLocals);
				} 
				System.out.println("结束执行:" + inheritableThreadLocals+"===>"+threadLocals);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void error(MethodHandler methodHandler, Throwable exception) {
		System.out.println("执行异常");
		exception.printStackTrace();
	}

	@Override
	public void before(RegisterDefinition registerDefinition, Class<?> plugClass, Constructor<?> constructor,
			Object... args) {
	}

	@Override
	public void after(RegisterDefinition registerDefinition, Class<?> plugClass, Constructor<?> constructor,
			Object proxyObject, Object... args) {
		if(isRunable(registerDefinition.getRegisterClass())) {
			try {
				EnviromentExecutorTest.init((Runnable)proxyObject);
			} catch (IllegalAccessException | NoSuchFieldException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void exception(RegisterDefinition registerDefinition, Class<?> plugClass, Constructor<?> constructor,
			Object proxyObject, PluginRuntimeException throwable, Object... args) {
	}

}
