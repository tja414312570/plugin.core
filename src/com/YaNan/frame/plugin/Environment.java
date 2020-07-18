package com.YaNan.frame.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.YaNan.frame.plugin.event.AbstractEvent;
import com.YaNan.frame.plugin.event.EventListener;
import com.YaNan.frame.plugin.event.InterestedEventSource;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * 环境类，提供Plugin环境
 * 环境类提供全局上下文配置，全局属性
 * 采用holder的单例模式
 * 提供一个系统事件
 * @author yanan
 *
 */
public class Environment {
	public static final String MAIN_CLASS = "-MAIN-CLASS";
	public static final String MAIN_CLASS_PATH = "-MAIN-CLASS-PATH";
	private static InheritableThreadLocal<Environment> enInheritableThreadLocal = new InheritableThreadLocal<>();
	private Map<String,List<EventListener<AbstractEvent>>>
	eventListenerMap = new ConcurrentHashMap<>();
	//全局配置
	private Config globalConfig;
	//全局变量
	private Map<String,Object> globalVariable = new ConcurrentHashMap<String,Object>();
	/**
	 * 分发事件
	 * @param eventSource 事件源
	 * @param abstractEvent 事件
	 */
	public void distributeEvent(InterestedEventSource eventSource,AbstractEvent abstractEvent) {
		List<EventListener<AbstractEvent>> eventListenerList = eventListenerMap.get(eventSource.getName());
		if(eventListenerList == null)
			return;
		eventListenerList.forEach(eventListener -> eventListener.onEvent(abstractEvent));
	}
	/**
	 * 注册时间监听
	 * @param eventSource 时间源
	 * @param eventListener 时间监听
	 */
	@SuppressWarnings("unchecked")
	public final void registEventListener(InterestedEventSource eventSource,EventListener<? extends AbstractEvent> eventListener) {
		synchronized (eventSource) {
			List<EventListener<AbstractEvent>> eventListenerList = eventListenerMap.get(eventSource.getName());
			if(eventListenerList == null) {
				eventListenerList = new ArrayList<EventListener<AbstractEvent>>();
				eventListenerMap.put(eventSource.getName(), eventListenerList);
			}
			eventListenerList.add((EventListener<AbstractEvent>) eventListener);
		}
	}
	/**
	 * 移除事件监听
	 * @param eventSource 事件源
	 * @param eventListeners 时间监听
	 */
	@SafeVarargs
	public final void removeEventListener(InterestedEventSource eventSource, EventListener<? extends AbstractEvent>... eventListenerArray) {
		synchronized (eventSource) {
			//如果没有传入事件，则删除所有事件
			if(eventListenerArray.length == 0)
				eventListenerMap.remove(eventSource.getName());
			else {
				List<EventListener<AbstractEvent>> eventListenerList = eventListenerMap.get(eventSource.getName());
				if(eventListenerList == null) {
					eventListenerMap.remove(eventSource.getName());
					return;
				}
				//删除对应的事件
				for(EventListener<? extends AbstractEvent> eventListener : eventListenerArray)
					eventListenerList.remove(eventListener);
				if(eventListenerList.isEmpty()) {
					eventListenerMap.remove(eventSource.getName());
				}
			}
		}
	}
	/**
	 * Eventment的holder实现
	 * @author yanan
	 *
	 */
	public final static class EnviromentHolder{
		private static final Environment ENVIRONMENT = new Environment();
	}
	private Environment() {
		enInheritableThreadLocal.set(this);
		globalConfig = ConfigFactory.parseMap(eventListenerMap);
	};
	/**
	 * instance a Environment 
	 * @return Environment the environment
	 */
	public static Environment getEnviroment() {
		return EnviromentHolder.ENVIRONMENT;
	}
	/**
	 * get the global environment configure
	 * @return global configure
	 */
	public Config getConfigure() {
		return this.globalConfig;
	}
	/**
	 * merge configure to global configure
	 * @param config the configure
	 */
	public synchronized void mergeConfig(Config config){
		if(globalConfig == null) {
			globalConfig = config;
		}else {
			globalConfig.merge(config);
		}
	}
	/**
	 * merge the map to global configure
	 */
	public synchronized void mergeVariableToConfig(){
		Config config = ConfigFactory.parseMap(this.globalVariable);
		if(globalConfig == null) {
			globalConfig = config;
		}else {
			globalConfig.merge(config);
		}
	}
	/**
	 * set the variable to environment
	 * @param key name
	 * @param value value
	 */
	public void setVariable(String key,Object value) {
		this.globalVariable.put(key, value);
	}
	/**
	 * get a variable
	 * @param <T> variable type
	 * @param key variable name
	 * @return variable
	 */
	@SuppressWarnings("unchecked")
	public <T> T getVariable(String key) {
		return (T) this.globalVariable.get(key);
	}
	/**
	 * judge the environment whether contains a variable
	 * @param key variable name
	 * @return boolean true or false
	 */
	public boolean hasVariable(String key) {
		return this.globalVariable.containsKey(key);
	}
	/**
	 * remove variable
	 * @param keys names
	 */
	public void removeVariable(String... keys) {
		for(String key : keys)
			this.globalVariable.remove(key);
	}
}