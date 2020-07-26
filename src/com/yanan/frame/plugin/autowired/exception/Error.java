package com.yanan.frame.plugin.autowired.exception;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 错误处理注解，提供bean执行过程的异常处理
 * @author yanan
 *
 */
@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Error {
	/**
	 * 上限异常
	 * @return 异常
	 */
	Class<?> exception() default Throwable.class;
	/**
	 * 默认值
	 * @return 表达式
	 */
	String value() default "";
	/**
	 * 是否记录错误
	 * @return 是否记录错误
	 */
	boolean recorder() default true;
}
