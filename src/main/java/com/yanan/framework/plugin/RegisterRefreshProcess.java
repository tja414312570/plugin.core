package com.yanan.framework.plugin;

import com.yanan.framework.plugin.definition.RegisterDefinition;

public interface RegisterRefreshProcess extends InstanceProcess{

	void process(PlugsFactory plugsFactory, RegisterDefinition currentRegisterDefinition);

}
