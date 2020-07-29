package com.yanan.frame.plugin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 实例销毁后执行的方法
 * 仅作用同一实例
 * @author yanan
 */
@Target(ElementType.METHOD )
@Retention(RetentionPolicy.RUNTIME)
public @interface Destory {
}
