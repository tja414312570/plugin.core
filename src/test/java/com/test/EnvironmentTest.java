package com.test;

import java.lang.reflect.Modifier;

import com.yanan.framework.plugin.Environment;
import com.yanan.framework.plugin.event.AbstractEvent;
import com.yanan.framework.plugin.event.EventListener;
import com.yanan.framework.plugin.event.InterestedEventSource;

public class EnvironmentTest {
	transient String rnm;
	static class MyEvent implements AbstractEvent{
		private String name;
		public MyEvent(String name) {
			this.name = name;
		}
	}
	public static void main(String[] args) throws NoSuchFieldException, SecurityException {
		System.out.println(Modifier.isFinal(EnvironmentTest.class.getDeclaredField("rnm").getModifiers()));
		Environment environment = Environment.getEnviroment();
		InterestedEventSource eventSource = new InterestedEventSource() {
			@Override
			public int hashCode() {
				return 0;
			}
		};
		InterestedEventSource eventSource2 = new InterestedEventSource() {
			@Override
			public int hashCode() {
				return 0;
			}
		};
		EventListener<MyEvent> eventListener = new EventListener<MyEvent>() {

			@Override
			public void onEvent(MyEvent event) {
				System.out.println("接受到事件1:"+event.name);
			}
		};
		EventListener<AbstractEvent> eventListener2 = new EventListener<AbstractEvent>() {

			@Override
			public void onEvent(AbstractEvent event) {
				System.out.println("接受到事件2:"+event);
			}
		};
		System.out.println(hash(eventSource));
		System.out.println(hash(eventSource2));
		environment.registEventListener(eventSource, eventListener);
		environment.registEventListener(eventSource2, eventListener2);
		MyEvent abstractEvent = new MyEvent("事件名称");
		MyEvent abstractEvent2 = new MyEvent("事件名称2");
		environment.distributeEvent(eventSource, abstractEvent);
		environment.removeEventListener(eventSource, eventListener2);
		environment.distributeEvent(eventSource, abstractEvent2);
		environment.removeEventListener(eventSource);
		environment.distributeEvent(eventSource, abstractEvent2);
	}
	 static final int hash(Object key) {
	        int h;
	        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
	    }
}
