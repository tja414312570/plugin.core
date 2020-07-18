package com.test;

import java.io.IOException;
import java.io.InputStream;

import com.typesafe.config.Config;
import com.yanan.frame.plugin.ConfigContext;
import com.yanan.frame.plugin.PlugsFactory;
import com.yanan.frame.plugin.PlugsFactory.STREAM_TYPT;

public class testConfigure {
	public static void main(String[] args) throws IOException {
		PlugsFactory.init();
		InputStream is = testConfigure.class.getResourceAsStream("../../plugin2.yc");
		PlugsFactory.getInstance().addPlugs(is,STREAM_TYPT.CONF,null);
		Config config = ConfigContext.getInstance().getGlobalConfig().getConfig("MVC");
		
		System.out.println(ConfigContext.getInstance());
		System.out.println(config);
		config.allowKeyNull();
		System.out.println(ConfigContext.getInstance().getGlobalConfig().getConfig("app"));
	}
}
