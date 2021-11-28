package com.yanan.framework.plugin;

import com.yanan.framework.plugin.definition.RegisterDefinition;

public interface InstanceBeforeProcesser extends InstanceProcess{
	public Object before(RegisterDefinition registerDefinition,Class<?> serviceClasss,Object instance);
}
