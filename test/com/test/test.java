package com.test;

import java.util.Arrays;

import com.yanan.frame.plugin.PlugsFactory;

public class test {
	public static void main(String[] args) {
		PlugsFactory.init();
//		SimpleRegister simpleRegister = PlugsFactory.getPlugsInstance(SimpleRegister.class); 
//		System.out.println(simpleRegister);
		System.out.println(Arrays.toString(PlugsFactory.getInstance().getScanPath()));
		System.out.println(PlugsFactory.getInstance().getConfigureLocation());
	}
}	
