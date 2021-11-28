package com.yanan.framework.plugin.autowired.enviroment;

import org.slf4j.Logger;

import com.typesafe.config.Config;
import com.yanan.framework.plugin.Environment;
import com.yanan.framework.plugin.annotations.Register;
import com.yanan.framework.plugin.annotations.Service;
import com.yanan.framework.plugin.autowired.property.PropertyManager;

@Register
public class EnvironmentVariableSupport implements VariableSupporter{
	Environment environment;
	@Service
	private Logger logger ;
	public EnvironmentVariableSupport() {
		environment = Environment.getEnviroment();
	}
	public Object getVariableValue(String name) {
		Object value = environment.getVariable(name);
		if (value == null) {
			Config config = environment.getConfigure();
			config.allowKeyNull();
			if (!config.isList(name)) {
				value = config.getValue(name);
			} else {
				value = config.getValueList(name);
			}
		}
		if (value == null) {
			value = PropertyManager.getInstance().getProperty(name);
		}
		return value;
	}
}
