package com.test;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class LockTest {
	static int id = 0;
	public static void main(String[] args) {
		
		Executor executor = Executors.newFixedThreadPool(10);
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				id ++;
				System.out.println(id);
			}
		};
		for(int i = 0;i<100;i++) {
			executor.execute(runnable);
		}
	}
}
