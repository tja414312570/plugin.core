package com.yanan.framework.plugin.matcher;

import java.lang.reflect.AnnotatedElement;

/**
 * 注解匹配器
 * @author yanan
 *
 */
public interface RegisterMatcher {
	/**
	 * 将目标类转化成属性
	 */
	String[] attributes(Class<?> registerClass);
	/**
	 * 通过方法调用
	 */
	String parseAttribute(Object... args);
	/**
	 * 通过自动注入
	 */
	String parseAttribute(AnnotatedElement args);
}
