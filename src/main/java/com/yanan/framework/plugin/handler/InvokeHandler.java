package com.yanan.framework.plugin.handler;

/**
 * 调用拦截器
 * @author yanan
 *
 */
public interface InvokeHandler {
	/**
	 * 在方法调用之前
	 * @param methodHandler 方法handler
	 */
	default void before(MethodHandler methodHandler) {};
	/**
	 * 在方法调用之后执行
	 * @param methodHandler 方法handler
	 */
	default void after(MethodHandler methodHandler) {};
	/**
	 * 在调用方法异常时执行
	 * @param methodHandler 方法handler 
	 * @param exception 异常
	 */
	default void error(MethodHandler methodHandler, Throwable exception) {};
}