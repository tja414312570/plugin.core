package com.test;

import java.util.Arrays;

import com.YaNan.frame.plugin.PlugsFactory;
import com.YaNan.frame.plugin.PlugsInitLock;

public class test {
	public static void main(String[] args) {
		for(int i = 0;i<1000;i++)
		new Thread(new Runnable() {
			@Override
			public void run() {
				PlugsFactory.init();
			}
		}).start();
		new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					System.out.println("holder:"+PlugsInitLock.getLockHolder());
					System.out.println("lock:"+PlugsInitLock.getLock()+" status :"+PlugsInitLock.getLock().isLock());
					System.out.println("queue:"+PlugsInitLock.getLockQueue());
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
//		PlugsFactory.init();
////		SimpleRegister simpleRegister = PlugsFactory.getPlugsInstance(SimpleRegister.class); 
////		System.out.println(simpleRegister);
//		System.out.println(Arrays.toString(PlugsFactory.getInstance().getScanPath()));
//		System.out.println(PlugsFactory.getInstance().getConfigureLocation());
	}
}	
