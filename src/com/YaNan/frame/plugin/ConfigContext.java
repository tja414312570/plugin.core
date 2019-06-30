package com.YaNan.frame.plugin;

import com.typesafe.config.Config;

public class ConfigContext {
	private final static ConfigContext context = new ConfigContext();
	private Config blobalConfig;
	public static ConfigContext getInstance() {
		return context;
	}
	public synchronized void mergeConfig(Config config){
		if(blobalConfig == null) {
			blobalConfig = config;
		}else {
			blobalConfig.merge(config);
		}
	}
	public Config getGlobalConfig() {
		return blobalConfig;
	}
}
