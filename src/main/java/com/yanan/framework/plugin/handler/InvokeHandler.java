package com.yanan.framework.plugin.handler;

/**
 * 调用拦截器
 * @author yanan
 *
 */
public interface InvokeHandler {
	default Object around(MethodHandler methodHandler) throws Throwable{
		return methodHandler.invoke();
	}
}