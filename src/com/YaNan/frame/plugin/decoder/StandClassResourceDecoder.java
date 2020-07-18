package com.YaNan.frame.plugin.decoder;

import com.YaNan.frame.plugin.PlugsFactory;
import com.YaNan.frame.utils.resource.Resource;

public class StandClassResourceDecoder implements ResourceDecoder<Resource>{
	@Override
	public void decodeResource(PlugsFactory factory,Resource resource) {
		String scanExpress = resource.getPath();
		System.out.println("扫描资源:"+scanExpress);
	}
}
