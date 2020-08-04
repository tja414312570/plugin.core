package com.yanan.framework.plugin.decoder;

import com.yanan.utils.resource.Resource;
import com.yanan.framework.plugin.PlugsFactory;

public interface ResourceDecoder<T extends Resource> {
	void decodeResource(PlugsFactory factory,T resource);

}