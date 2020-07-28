package com.yanan.frame.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

import com.yanan.frame.plugin.event.AbstractEvent;
import com.yanan.frame.plugin.event.EventListener;
import com.yanan.frame.plugin.event.InterestedEventSource;
import com.yanan.utils.asserts.Assert;
import com.yanan.utils.asserts.Function;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;

/**
 * 环境类，提供Plugin环境 环境类提供全局上下文配置，全局属性 采用holder的单例模式 提供一个系统事件
 * 
 * @author yanan
 *
 */
public class Environment extends AbstractQueuedSynchronizer{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6463910343135833934L;
	public static final String MAIN_CLASS = "-MAIN-CLASS";
	public static final String MAIN_CLASS_PATH = "-MAIN-CLASS-PATH";
	private static final String ENVIROMENT_EXECUTOR_TOKEN = "ENVIROMENT_EXECUTOR_TOKEN_";
	private static InheritableThreadLocal<Environment> enInheritableThreadLocal = new InheritableThreadLocal<>();
	private Map<String, List<EventListener<AbstractEvent>>> eventListenerMap = new ConcurrentHashMap<>();
	// 全局配置
	private Config globalConfig;
	// 全局变量
	private Map<String, Object> globalVariable = new ConcurrentHashMap<String, Object>();

	/**
	 * 分发事件
	 * 
	 * @param eventSource   事件源
	 * @param abstractEvent 事件
	 */
	public void distributeEvent(InterestedEventSource eventSource, AbstractEvent abstractEvent) {
		List<EventListener<AbstractEvent>> eventListenerList = eventListenerMap.get(eventSource.getName());
		if (eventListenerList == null)
			return;
		eventListenerList.forEach(eventListener -> eventListener.onEvent(abstractEvent));
	}

	/**
	 * 注册时间监听
	 * 
	 * @param eventSource   时间源
	 * @param eventListener 时间监听
	 */
	@SuppressWarnings("unchecked")
	public final void registEventListener(InterestedEventSource eventSource,
			EventListener<? extends AbstractEvent> eventListener) {
		synchronized (eventSource.getName().intern()) {
			List<EventListener<AbstractEvent>> eventListenerList = eventListenerMap.get(eventSource.getName());
			if (eventListenerList == null) {
				eventListenerList = new ArrayList<EventListener<AbstractEvent>>();
				eventListenerMap.put(eventSource.getName(), eventListenerList);
			}
			eventListenerList.add((EventListener<AbstractEvent>) eventListener);
		}
	}

	/**
	 * 移除事件监听
	 * 
	 * @param eventSource    事件源
	 * @param eventListenerArray 事件监听
	 */
	@SafeVarargs
	public final void removeEventListener(InterestedEventSource eventSource,
			EventListener<? extends AbstractEvent>... eventListenerArray) {
		synchronized (eventSource.getName().intern()) {
			// 如果没有传入事件，则删除所有事件
			if (eventListenerArray.length == 0)
				eventListenerMap.remove(eventSource.getName());
			else {
				List<EventListener<AbstractEvent>> eventListenerList = eventListenerMap.get(eventSource.getName());
				if (eventListenerList == null) {
					eventListenerMap.remove(eventSource.getName());
					return;
				}
				// 删除对应的事件
				for (EventListener<? extends AbstractEvent> eventListener : eventListenerArray)
					eventListenerList.remove(eventListener);
				if (eventListenerList.isEmpty()) {
					eventListenerMap.remove(eventSource.getName());
				}
			}
		}
	}

	/**
	 * Eventment的holder实现
	 * 
	 * @author yanan
	 *
	 */
	public final static class EnviromentHolder {
		private static final Environment ENVIRONMENT = new Environment();
	}

	private Environment() {
		enInheritableThreadLocal.set(this);
		globalConfig = ConfigFactory.parseMap(eventListenerMap);
	};

	/**
	 * instance a Environment
	 * 
	 * @return Environment the environment
	 */
	public static Environment getEnviroment() {
		return EnviromentHolder.ENVIRONMENT;
	}

	/**
	 * get the global environment configure
	 * 
	 * @return global configure
	 */
	public Config getConfigure() {
		return this.globalConfig;
	}

	/**
	 * 从全局配置中获取配置
	 * 
	 * @param path 配置路径
	 * @return 配置
	 */
	public Config getConfig(String path) {
		Assert.isNull(path);
		if (this.globalConfig == null)
			return null;
		try {
			this.globalConfig.allowKeyNull(true);
			Config config = this.globalConfig.getConfig(path);
			return config;
		} finally {
			this.globalConfig.allowKeyNull(false);
		}
	}

	/**
	 * 获取一个配置，当配置不存在抛出异常
	 * 
	 * @param path 路径
	 * @return 配置
	 */
	public Config getRequiredConfig(String path) {
		Assert.isNull(path);
		if (this.globalConfig == null)
			throw new NullPointerException("the global config is null");
		return this.globalConfig.getConfig(path);
	}

	/**
	 * 获取配置值
	 * 
	 * @param path 路径
	 * @return 配置值
	 */
	public ConfigValue getConfigValue(String path) {
		Assert.isNull(path);
		if (this.globalConfig == null)
			return null;
		this.globalConfig.allowKeyNull(true);
		try {
			ConfigValue config = this.globalConfig.getValue(path);
			return config;
		} finally {
			this.globalConfig.allowKeyNull(false);
		}
	}

	public ConfigValue getRequiredConfigValue(String path) {
		Assert.isNull(path);
		if (this.globalConfig == null)
			throw new NullPointerException("the global config is null");
		return this.globalConfig.getValue(path);
	}

	/**
	 * merge configure to global configure
	 * 
	 * @param config the configure
	 */
	public synchronized void mergeConfig(Config config) {
		if (globalConfig == null) {
			globalConfig = config;
		} else {
			globalConfig.merge(config);
		}
	}

	/**
	 * merge the map to global configure
	 */
	public synchronized void mergeVariableToConfig() {
		Config config = ConfigFactory.parseMap(this.globalVariable);
		if (globalConfig == null) {
			globalConfig = config;
		} else {
			globalConfig.merge(config);
		}
	}

	/**
	 * set the variable to environment
	 * 
	 * @param key   name
	 * @param value value
	 */
	public void setVariable(String key, Object value) {
		this.globalVariable.put(key, value);
	}
	/**
	 * 对比并设置值,注意，对比和设置值的时候key和value均不能为空
	 * @param key key
	 * @param oldValue 希望的值
	 * @param newValue 新值
	 * @return 是否成功
	 */
	public boolean compareAndSet(String key,Object oldValue,Object newValue) {
		return this.globalVariable.replace(key, oldValue, newValue);
	}
	/**
	 * 在整个环境期间保证只执行一次,注意，抛出异常后可以重复执行
	 * @param mark 任务标志；
	 * @param function 任务
	 */
	public void executorOnce(final Object mark,final Function function) {
		Assert.isNull(mark, "the function mark is null");
		String key = ENVIROMENT_EXECUTOR_TOKEN+mark;
		if(!hasVariable(key)) {
			synchronized (key.intern()) {
				if(!hasVariable(key)) {
					function.execute();
					setVariable(key, true);
				}
			}
		}
	}
	
	/**
	 * get a variable
	 * 
	 * @param     <T> variable type
	 * @param key variable name
	 * @return variable
	 */
	@SuppressWarnings("unchecked")
	public <T> T getVariable(String key) {
		return (T) this.globalVariable.get(key);
	}
	/**
	 * 获取变量，如果变量不存在，返回一个默认值
	 * @param key key
	 * @param defaultValue default value
	 * @param <T> the parameterType
	 * @return result
	 */
	public <T> T getVariable(String key, T defaultValue) {
		if(!globalVariable.containsKey(key))
			return defaultValue;
		return getVariable(key);
	}
	/**
	 * 获取一个变量，不允许返回空
	 * @param key key
	 * @param <T> the parameterType
	 * @return value
	 */
	public <T> T getRequiredVariable(String key) {
		T t = getVariable(key);
		Assert.isNull(t,"cloud not found required variable for key ["+key+"]");
		return t;
	}
	/**
	 * judge the environment whether contains a variable
	 * 
	 * @param key variable name
	 * @return boolean true or false
	 */
	public boolean hasVariable(String key) {
		return this.globalVariable.containsKey(key);
	}

	/**
	 * remove variable
	 * 
	 * @param keys names
	 */
	public void removeVariable(String... keys) {
		for (String key : keys)
			this.globalVariable.remove(key);
	}
}
