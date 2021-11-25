package com.yanan.framework.plugin;

import com.yanan.framework.plugin.definition.RegisterDefinition;

public interface RegisterInitProcess {

	void process(PlugsFactory plugsFactory, RegisterDefinition currentRegisterDefinition);

}
