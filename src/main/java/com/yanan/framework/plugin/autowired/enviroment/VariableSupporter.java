package com.yanan.framework.plugin.autowired.enviroment;

import java.lang.reflect.AnnotatedElement;

public interface VariableSupporter {
	public <T> T getVariable(String name,Class<T> targetType,Variable variable,AnnotatedElement annotation);
}
