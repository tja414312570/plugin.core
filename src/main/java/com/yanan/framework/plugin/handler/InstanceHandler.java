package com.yanan.framework.plugin.handler;

import java.lang.reflect.Constructor;

import com.yanan.framework.plugin.definition.RegisterDefinition;
import com.yanan.framework.plugin.exception.PluginRuntimeException;

/**
 * 实例Handler
 * @author yanan
 *
 */
public interface InstanceHandler {
	/**
	 * 将代理对象实例化之前
	 * @param registerDefinition 组件定义
	 * @param plugClass 组件类
	 * @param constructor 构造器
	 * @param args 参数
	 */
	default void before(RegisterDefinition registerDefinition, Class<?> plugClass,Constructor<?> constructor, Object... args) {};
	/**
	 * 将代理对象实例化之后
	 * @param registerDefinition 组件定义
	 * @param constructor 构造器
	 * @param plugClass 组件类
	 * @param proxyObject 代理对象
	 * @param args 参数
	 */
	default void after(RegisterDefinition registerDefinition, Class<?> plugClass,Constructor<?> constructor, Object proxyObject,Object... args){};
	/**
	 * 对象实例化时异常
	 * @param registerDefinition 组件定义
	 * @param constructor 构造器
	 * @param plugClass 组件类
	 * @param proxyObject 代理对象
	 * @param throwable 异常
	 * @param args 参数
	 */
	default void exception(RegisterDefinition registerDefinition, Class<?> plugClass,Constructor<?> constructor, Object proxyObject, PluginRuntimeException throwable, Object... args){};

}