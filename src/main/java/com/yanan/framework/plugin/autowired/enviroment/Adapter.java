package com.yanan.framework.plugin.autowired.enviroment;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 作用于类，使得加载器不会从新加载此类
 * @author yanan
 */
@Target({ TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Adapter {
	Class<?>[] input() default {};
	Class<?>[] target() default {};
}
