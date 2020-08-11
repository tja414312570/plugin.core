package com.test;

import com.yanan.utils.reflect.AppClassLoader;

public class test {
	public static void main(String[] args) {
//		PlugsFactory.init();
		Class<?> temps = AppClassLoader.class;
		while(temps != null) {
			System.out.print(temps+"--->");
			temps = temps.getSuperclass();
		}
//		System.out.println(ClassLoader.class.getSuperclass());
//		SimpleRegister simpleRegister = PlugsFactory.getPlugsInstance(SimpleRegister.class); 
//		System.out.println(simpleRegister);
	}
}	
