package com.yanan.framework.plugin;

import com.yanan.framework.plugin.definition.RegisterDefinition;

public interface RegisterRefreshProcess {

	void process(PlugsFactory plugsFactory, RegisterDefinition currentRegisterDefinition);

}
