package com.YaNan.frame.plugin.builder.resolver;

import com.YaNan.frame.plugin.definition.RegisterDefinition;
import com.typesafe.config.ConfigList;
public class ResourceParameterResolver implements ParameterResolver<ConfigList>{
	@Override
	public Object resove(ConfigList configValue, RegisterDefinition registerDefinition) {
		return null;
	}
}
