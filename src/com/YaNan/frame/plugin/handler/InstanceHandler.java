package com.yanan.frame.plugin.handler;

import java.lang.reflect.Constructor;

import com.yanan.frame.plugin.definition.RegisterDefinition;
import com.yanan.frame.plugin.exception.PluginRuntimeException;

public interface InstanceHandler {
	/**
	 * 将代理对象实例化之前
	 * @param registerDescription
	 * @param plugClass
	 * @param args
	 */
	void before(RegisterDefinition registerDefinition, Class<?> plugClass,Constructor<?> constructor, Object... args);
	/**
	 * 将代理对象实例化之后
	 * @param registerDescription
	 * @param plugClass
	 * @param proxy
	 * @param args
	 */
	void after(RegisterDefinition registerDefinition, Class<?> plugClass,Constructor<?> constructor, Object proxyObject,Object... args);
	/**
	 * 对象实例化时异常
	 * @param registerDescription
	 * @param plug
	 * @param proxy
	 * @param t
	 * @param args
	 */
	void exception(RegisterDefinition registerDefinition, Class<?> plug,Constructor<?> constructor, Object proxyObject, PluginRuntimeException throwable, Object... args);

}
