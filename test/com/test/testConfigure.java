package com.test;

import java.io.IOException;

import com.typesafe.config.Config;
import com.yanan.frame.plugin.Environment;
import com.yanan.frame.plugin.PlugsFactory;
import com.yanan.utils.resource.Resource;
import com.yanan.utils.resource.ResourceManager;

public class testConfigure {
	public static void main(String[] args) throws IOException {
		Resource resource= ResourceManager.getResource("classpath:plugin2.yc");
		PlugsFactory.init(resource);
		
		Config config =Environment.getEnviroment().getConfig("MVC");
		System.out.println(config);
		System.out.println(Environment.getEnviroment().getRequiredConfigValue("MVC.SERVER.JBOSS").valueType());
		System.out.println(Environment.getEnviroment().getVariable("key")+"");
		System.out.println(Environment.getEnviroment().getRequiredVariable("key")+"");
	}
}
