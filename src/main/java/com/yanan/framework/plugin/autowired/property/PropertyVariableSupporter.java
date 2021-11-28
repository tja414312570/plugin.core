package com.yanan.framework.plugin.autowired.property;

import com.yanan.framework.plugin.annotations.Register;
import com.yanan.framework.plugin.autowired.enviroment.VariableSupporter;

@Register
public class PropertyVariableSupporter implements VariableSupporter{

	@Override
	public Object getVariableValue(String name) {
		return PropertyManager.getInstance().getProperty(name);
	}

}
