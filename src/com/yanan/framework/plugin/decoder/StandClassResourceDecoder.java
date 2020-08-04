package com.yanan.framework.plugin.decoder;

import com.yanan.utils.resource.Resource;
import com.yanan.framework.plugin.PlugsFactory;

public class StandClassResourceDecoder implements ResourceDecoder<Resource>{
	@Override
	public void decodeResource(PlugsFactory factory,Resource resource) {
		String scanExpress = resource.getPath();
		System.out.println("扫描资源:"+scanExpress);
	}
}