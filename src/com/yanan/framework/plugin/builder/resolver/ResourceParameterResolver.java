package com.yanan.framework.plugin.builder.resolver;

import com.typesafe.config.ConfigValue;
import com.yanan.framework.plugin.annotations.Register;
import com.yanan.framework.plugin.definition.RegisterDefinition;
import com.yanan.utils.resource.Resource;
import com.yanan.utils.resource.ResourceManager;
@Register(attribute= {"resource"})
public class ResourceParameterResolver implements ParameterResolver<ConfigValue>{

	@Override
	public Object resove(ConfigValue configValue, String typeName, int parameterIndex,
			RegisterDefinition registerDefinition) {
		Resource abstractResourceEntry = ResourceManager.getResource((String) configValue.unwrapped());
		return abstractResourceEntry;
	}
}