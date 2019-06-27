package com.YaNan.frame.plugin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD,ElementType.PARAMETER} )
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceParameter {
	String value() default "";
}
