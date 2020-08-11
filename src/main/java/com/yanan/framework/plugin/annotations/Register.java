package com.yanan.framework.plugin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * 
 * @author yanan
 *
 */

import com.yanan.framework.plugin.ProxyModel;
@Target({ElementType.TYPE} )
@Retention(RetentionPolicy.RUNTIME)
public @interface Register {
	/**
	 * 组件优先级，数值越低，优先级越高
	 * @return 组件优先级
	 */
	int priority() default 0;
	/**
	 * 是否采用单例模式
	 * @return 单例模式
	 */
	boolean signlTon() default true;
	/**
	 * 组件的属性，匹配模式 *匹配任意字符，？匹配任意单字符
	 * @return 属性
	 */
	String[] attribute() default "*";
	/**
	 * 用于描述组件所实现的接口
	 * @return 组件实现的接口
	 */
	Class<?>[] register() default {};
	/**
	 * 组件描述
	 * @return 组件描述
	 */
	String description() default "";
	/**
	 * 描述组件实现类所在父类，当接口不在此类时可以使用此属性指向接口所在位置
	 * @return 组件所执行实际类位置
	 */
	Class<?> declare() default Object.class;
	/**
	 * 代理模式
	 * @return 代理模式
	 */
	ProxyModel model() default ProxyModel.DEFAULT;
	/**
	 * 组件实例化并在Field赋值完成后之后执行的方法
	 * @return 初始化后执行的方法
	 */
	String[] afterInstance() default {};
	/**
	 * 注册器ID
	 * @return 注册器ID
	 */
	String id() default "";
	/**
	 * 实例销毁时执行的方法
	 * @return 方法名称
	 */
	String destory() default "";
	
}