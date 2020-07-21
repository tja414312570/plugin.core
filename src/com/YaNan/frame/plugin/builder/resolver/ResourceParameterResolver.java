package com.yanan.frame.plugin.builder.resolver;

import com.typesafe.config.ConfigValue;
import com.yanan.frame.plugin.annotations.Register;
import com.yanan.frame.plugin.definition.RegisterDefinition;
import com.yanan.utils.resource.AbstractResourceEntry;
import com.yanan.utils.resource.ResourceManager;
@Register(attribute= {"file","resource"})
public class ResourceParameterResolver implements ParameterResolver<ConfigValue>{

	@Override
	public Object resove(ConfigValue configValue, String typeName, int parameterIndex,
			RegisterDefinition registerDefinition) {
		AbstractResourceEntry abstractResourceEntry = ResourceManager.getResource((String) configValue.unwrapped());
		if(typeName.equals("file")) {
			return abstractResourceEntry.getFile();
		}
		return abstractResourceEntry;
	}
}
