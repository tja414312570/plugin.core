package com.yanan.frame.plugin.autowired.enviroment;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于获取.properties中的属性，该属性可以是任意.properties文件中的
 * key  注意，如果出现了多个相同的key的value，那么可能取任意一个，取决于服务器启动时
 * 扫描的到该属性的先后循序，最终相同属性会选最后一个。
 * @author yanan
 *
 */
@Target({ElementType.PARAMETER,ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Variable {
	/**
	 * 参数名称
	 * @return 属性名
	 */
	String value() default "";
	/**
	 * 默认值
	 * @return 默认值
	 */
	String defaultValue() default "";
	/**
	 * 是否必须
	 * @return 是否必须
	 */
	boolean required() default true;
	
}
