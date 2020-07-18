package com.yanan.frame.plugin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.yanan.frame.plugin.annotations.group.Defaullt;


/**
 * 实例化之后执行某方法
 * 仅作用同一实例
 * @author yanan
 */
@Target(ElementType.METHOD )
@Retention(RetentionPolicy.RUNTIME)
public @interface AfterInstantiation {
	Class<? extends AnnotationHandler>[] value() default {AnnotationHandler.class};
	Class<?> group() default Defaullt.class;
}
