package com.yanan.frame.plugin.annotations;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * 
 * @author yanan
 *
 */
@Target({ElementType.TYPE} )
@Retention(RetentionPolicy.RUNTIME)
public @interface Support {
	/**
	 * 默认支持的注解
	 * @return 注解集合
	 */
	Class<? extends Annotation>[] value() default {};
	/**
	 * 默认支持的注解
	 * @return 支持的类型的字符串类型
	 */
	String[] name() default {};
}
