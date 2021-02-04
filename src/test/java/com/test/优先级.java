package com.test;

import com.yanan.framework.plugin.Plugin;
import com.yanan.framework.plugin.definition.PluginDefinition;
import com.yanan.framework.plugin.definition.RegisterDefinition;

public class 优先级 {
	public static void main(String[] args) {
		PluginDefinition pluginDefinition = new PluginDefinition(test.class);
		Plugin plugin = new Plugin(pluginDefinition);
		for(int i = 0;i<10;i++) {
			RegisterDefinition registerDefinition = new RegisterDefinition();
			registerDefinition.setPriority((int)(Math.random()*100));
			registerDefinition.setId(i+"");
			plugin.addRegister(registerDefinition);
		}
		System.out.println("");
		plugin.getRegisterDefinitionList().forEach(register->{
			System.out.println(register.getId()+"==>"+register.getPriority());
		});
	}
}
