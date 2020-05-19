package com.YaNan.frame.plugin;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 组件初始化锁
 * @author yanan
 *
 */
public class PlugsInitLock {
	/**
	 * 被阻塞的线程队列
	 */
	private volatile static List<Thread> lockQueue = new LinkedList<Thread>();
	public static List<Thread> getLockQueue() {
		return lockQueue;
	}
	public static Thread getLockHolder() {
		return lockHolder;
	}
	public static Lock getLock() {
		return lock;
	}
	/**
	 * 已获取得锁的线程
	 */
	private volatile static Thread lockHolder;
	public static void main(String[] args) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				PlugsInitLock.tryLock();
				try {
					System.out.println("十秒后解锁");
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("解锁");
				PlugsInitLock.unLock();
			}
		}).start();
		new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("等待锁");
				PlugsInitLock.checkLock();;
				System.out.println("已释放锁");
			}
		}).start();
		new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("等待锁1");
				PlugsInitLock.checkLock();;
				System.out.println("已释放锁1x");
			}
		}).start();
		new Thread(new Runnable() {
			@Override
			public void run() {
				while(lock.isLock()){
					System.out.println("=====");
					System.out.println("锁获得的线程:"+lockHolder);
					System.out.println("排队的队列："+lockQueue);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
	//互斥锁
	private static Lock lock = new Lock();
	public static void tryLock(){
		lock.tryLock();
	}
	public static void checkLock(){
		Thread thread = Thread.currentThread();
		if(lock.isLock() &&  PlugsInitLock.lockHolder != thread){
			PlugsInitLock.lockQueue.add(thread);
			while(lock.isLock()){
				Thread.yield();
			}
		}
	}
	public static void unLock(){
		lock.unLock();
	}
	public static class Lock{
		private volatile boolean lock;
		public boolean isLock() {
			return lock;
		}
		public void tryLock(){
			if(!lock){
				synchronized (PlugsInitLock.lock) {
					Thread thread = Thread.currentThread();
					PlugsInitLock.lockHolder = thread;
					lock = true;
				}
			}
		}
		public void unLock(){
			if(lock){
				synchronized (PlugsInitLock.lock) {
					lock = false;
					PlugsInitLock.lockHolder = null;
					lockQueue.clear();
				}
			}
		}
	}
}
