package com.YaNan.frame.plugin;

import com.typesafe.config.Config;

public class ConfigContext {
	private final static ConfigContext context = new ConfigContext();
	private Config globalConfig;
	public static ConfigContext getInstance() {
		return context;
	}
	public synchronized void mergeConfig(Config config){
		if(globalConfig == null) {
			globalConfig = config;
		}else {
			globalConfig.merge(config);
		}
	}
	public Config getGlobalConfig() {
		return globalConfig;
	}
}
