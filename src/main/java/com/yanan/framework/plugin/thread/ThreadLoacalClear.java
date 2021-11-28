package com.yanan.framework.plugin.thread;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.yanan.framework.plugin.annotations.Register;
import com.yanan.framework.plugin.handler.InvokeHandler;
import com.yanan.framework.plugin.handler.MethodHandler;
import com.yanan.utils.reflect.ReflectUtils;

//@Register(signlTon=true)
public class ThreadLoacalClear implements InvokeHandler{
	public static Class<?>[] expungeStaleEntryParameterType = {int.class};
	/**
	 * 判断实例是否是线程
	 * @param method 方法
	 * @return 是否为线程方法
	 */
	public static boolean isRunable(Method method) {
		//这里只判断run方法无参数 且实现了runnable
		return (method.getName().equals("run") 
//				&& Modifier.isPublic(method.getModifiers())
//				&& method.getReturnType().equals(void.class)
				&& method.getParameterCount() == 0
				&& ReflectUtils.implementsOf(method.getDeclaringClass(), Runnable.class));
	}
	
	@Override
	public void before(MethodHandler methodHandler) {
	}
	/**
	 * 移除线程池
	 * @param threadLocalMap 线程表
	 * @throws IllegalArgumentException ex
	 * @throws IllegalAccessException ex
	 * @throws NoSuchFieldException ex
	 * @throws SecurityException ex
	 * @throws InvocationTargetException ex
	 * @throws NoSuchMethodException ex
	 */
    public void remove(Object threadLocalMap) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, InvocationTargetException, NoSuchMethodException {
    	// Entry[] tab = threadLocalMap.table;
     	Object tab = ReflectUtils.getDeclaredFieldValue("table", threadLocalMap);
     	// int len = tab.length
    	int len = Array.getLength(tab);
    	for(int i = 0;i < len;i++) {
    		//Entry e = tab[i];
    		Object e = Array.get(tab, i);
    		// if(e != null && e.get() != null);
    		if(e != null && ReflectUtils.invokeMethod(e, "get") != null) {
    			//e.clear();
    			ReflectUtils.invokeMethod(e, "clear");
    			//threadLocalMap.expungeStaleEntry(i);
    			ReflectUtils.invokeDeclaredMethod(threadLocalMap, "expungeStaleEntry",expungeStaleEntryParameterType ,i);
    		}
    	}
    }
    /**
     * 移除当前线程的局部变量
     */
    public void removeThreadLocalMap() {
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
    	}catch (Exception e) {
			e.printStackTrace();
		}
    }
	@Override
	public void after(MethodHandler methodHandler) {
		if(isRunable(methodHandler.getMethod())) {
			removeThreadLocalMap();
		}
	}

	@Override
	public void error(MethodHandler methodHandler, Throwable exception) {
		after(methodHandler);
	}

//	@Override
//	public void after(RegisterDefinition registerDefinition, Class<?> plugClass, Constructor<?> constructor,
//			Object proxyObject, Object... args) {
//		if(isRunable(registerDefinition.getRegisterClass())) {
//			try {
//				EnviromentExecutorTest.init((Runnable)proxyObject);
//			} catch (IllegalAccessException | NoSuchFieldException e) {
//				e.printStackTrace();
//			}
//		}
//	}

}