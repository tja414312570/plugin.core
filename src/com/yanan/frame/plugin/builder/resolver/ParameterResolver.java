package com.yanan.frame.plugin.builder.resolver;

import com.yanan.frame.plugin.definition.RegisterDefinition;
import com.typesafe.config.ConfigValue;

@FunctionalInterface
public interface ParameterResolver<K extends ConfigValue> {
	Object resove(K configValue,String typeName,int parameterIndex,RegisterDefinition registerDefinition);
}
