package com.yanan.framework.plugin.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Encrypt {
	/**
	 * 秘钥服务接口
	 * @return 加密服务类
	 */
	Class<? extends EncryptService> interfacer();
	/**
	 * 参数
	 * @return 参数
	 */
	String[] parameters() default {};
}