package com.test;

import java.util.Arrays;

import com.yanan.frame.plugin.Environment;
import com.yanan.frame.plugin.PluginEvent;
import com.yanan.frame.plugin.PluginEventSource;
import com.yanan.frame.plugin.PlugsFactory;
import com.yanan.frame.plugin.builder.PluginInstanceFactory;
import com.yanan.frame.plugin.decoder.StandScanResource;
import com.yanan.frame.plugin.definition.RegisterDefinition;
import com.yanan.frame.plugin.event.EventListener;
import com.yanan.utils.resource.Resource;
import com.yanan.utils.resource.ResourceManager;

public class PlugsFactoryTest {
	public static void main(String[] args) {
		Environment.getEnviroment().registEventListener(new PluginEventSource(), new EventListener<PluginEvent>() {
			@Override
			public void onEvent(PluginEvent abstractEvent) {
				System.out.println("事件:"+abstractEvent.getEventType()+"==>"+abstractEvent.getEventContent());
			}
		});
//		Resource scanRscource = new StandScanResource("classpath:com.yanan");
		Resource configResorce = ResourceManager.getResource("classpath:plugin.yc");
		PlugsFactory.init(configResorce);
//		String jar = "/Volumes/GENERAL/mvn/com/github/tja414312570/plugin.utils/0.0.1/plugin.utils-0.0.1.jar!/**utils**";
//		PlugsFactory.init(new StandScanResource("classpath:com.test"));
		
//		PlugsFactory.init("classpath:plugin.yc");
//		PlugsFactory.init(new StandScanResource(jar));
		Runnable runnable = PlugsFactory.proxyInstance(new Runnable() {
			private String name;
			@Override
			public void run() {
				System.out.println(this);
				System.out.println("执行了我");
			}
		});
		new Thread(runnable).start();;
		System.out.println(Environment.getEnviroment().getConfig("MVC"));
	}
}
