package com.test;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class LockTest {
	static int id = 0;
	public static void main(String[] args) {
		
		AtomicInteger integer = new AtomicInteger(0);
		Executor executor = Executors.newFixedThreadPool(10);
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				throw new RuntimeException(integer.getAndIncrement()+"");
			}
		};
		for(int i = 0;i<100;i++) {
			executor.execute(runnable);
		}
	}
}
