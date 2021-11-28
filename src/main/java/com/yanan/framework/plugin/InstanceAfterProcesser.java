package com.yanan.framework.plugin;

import com.yanan.framework.plugin.definition.RegisterDefinition;

public interface InstanceAfterProcesser extends InstanceProcess{
	public Object after(RegisterDefinition registerDefinition,Class<?> serviceClasss,Object instance);
}
