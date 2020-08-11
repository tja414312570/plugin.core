package com.yanan.framework.plugin.builder.resolver;

import com.yanan.framework.plugin.definition.RegisterDefinition;
import com.typesafe.config.ConfigValue;

public interface DelayParameterResolver<K extends ConfigValue> extends ParameterResolver<K> {
	Class<?> parameterType(K configValue,String methodName,Class<?>[] argsTypes,String typeName,int parameterIndex,RegisterDefinition registerDefinition);
}