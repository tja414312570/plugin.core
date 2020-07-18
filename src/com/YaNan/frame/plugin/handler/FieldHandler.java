package com.yanan.frame.plugin.handler;

import java.lang.reflect.Field;

import com.yanan.frame.plugin.definition.RegisterDefinition;
/**
 * 字段拦截器
 * 当实例初始化之后立刻调用
 * @author yanan
 *
 */
public interface FieldHandler {
	/**
	 * 当要准备字段时掉此方法
	 * @param registerDescription
	 * @param proxy
	 * @param target
	 * @param desc
	 * @param args
	 */
	void preparedField(RegisterDefinition registerDefinition, Object proxy, Object target, InvokeHandlerSet handlerSet,
			Field field);

}
