package com.test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;

import com.yanan.framework.plugin.PlugsFactory;
import com.yanan.utils.reflect.ReflectUtils;
import com.yanan.utils.resource.Resource;
import com.yanan.utils.resource.ResourceManager;

public class EnviromentExecutorTest{
	static Map<Thread,Map<Class<?>,?>> map = new HashMap<>();
	static Map<Thread,Map<Class<?>,?>> inheritedMap = new HashMap<>();
	static Map<Runnable,Thread> threadRelated = new HashMap<>();
	@SuppressWarnings("unchecked")
	public static <T> void setInherited(Class<T> type,T value) {
		Thread currentThread = Thread.currentThread();
		Map<Class<?>, Object> threadLocal = (Map<Class<?>, Object>) inheritedMap.get(currentThread);
		if(threadLocal == null) {
			threadLocal = new HashMap<>();
			inheritedMap.put(currentThread, threadLocal);
		}
		threadLocal.put(type, value);
		System.out.println(currentThread+"   ==>"+threadLocal);
	}
	public static <T> void set(Class<T> type,T value) {
		Thread currentThread = Thread.currentThread();
		Map<Class<?>, Object> threadLocal = (Map<Class<?>, Object>) map.get(currentThread);
		if(threadLocal == null) {
			threadLocal = new HashMap<>();
			map.put(currentThread, threadLocal);
		}
		threadLocal.put(type, value);
	}
	public static void init(Runnable instance) throws IllegalAccessException, NoSuchFieldException {
		try {
			Thread currentThread = Thread.currentThread();
			threadRelated.put(instance, currentThread);
			System.out.println(threadRelated);
		} catch (IllegalArgumentException | SecurityException e) {
			e.printStackTrace();
		}
	}
	public static void preparedThreadLocal() {
		try {
			Thread currentThread = Thread.currentThread();
			Runnable runable = ReflectUtils.getDeclaredFieldValue("target", currentThread);
			System.out.println(threadRelated);
			Thread parent = threadRelated.get(runable);
			Map<Class<?>,?> inheritedThreadLoacal = inheritedMap.get(parent);
			System.out.println("父类:"+parent+"==>"+inheritedThreadLoacal);
			inheritedMap.put(currentThread, inheritedThreadLoacal);
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
	}
	@SuppressWarnings("unchecked")
	public static <T> T get(Class<T> type) {
		Thread currentThread = Thread.currentThread();
		Map<Class<?>, Object> threadLocal = (Map<Class<?>, Object>) map.get(currentThread);
		if(threadLocal != null)
			return (T) threadLocal.get(type);
		return null;
	}
	@SuppressWarnings("unchecked")
	public static <T> T getInherited(Class<T> type) {
		Thread currentThread = Thread.currentThread();
		Map<Class<?>, Object> threadLocal = (Map<Class<?>, Object>) inheritedMap.get(currentThread);
		if(threadLocal != null)
			return (T) threadLocal.get(type);
		return null;
	}
	@SuppressWarnings("unchecked")
	public static void clear() {
		Thread currentThread = Thread.currentThread();
		Map<Class<?>, Object> threadLocal = (Map<Class<?>, Object>) map.get(currentThread);
		if(threadLocal != null)
			threadLocal.clear();
	}
	public static void clearAll() {
		map.entrySet().removeIf(threadLocal->{
			threadLocal.getValue().entrySet().removeIf(entry->{
				return true;
			});
			return true;
		});
	}
	public static void main(String[] args) {
		Resource configResorce = ResourceManager.getResource("classpath:plugin.yc");
		PlugsFactory.init(configResorce);
		Thread thread = Thread.currentThread();
		setInherited(Integer.class,1);
		setInherited(int.class,2);
		Runnable runnable = PlugsFactory.proxyInstance(new Runnable() {
			@Override
			public void run() {
				System.out.println("可继承的表");
				System.out.println(getInherited(int.class));
				System.out.println(getInherited(Integer.class));
				System.out.println("------");
				set(Integer.class,3);
				set(int.class,4);
				System.out.println(get(int.class));
				System.out.println(get(Integer.class));
				LockSupport.unpark(thread);
			}
		},thread);
		System.out.println(runnable);
		new Thread(runnable).start(); 
		LockSupport.park();
		System.out.println("---    --");
		System.out.println(get(int.class));
		System.out.println(get(Integer.class));
	}
}
