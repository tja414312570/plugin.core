package com.yanan.frame.plugin.decoder;

import com.yanan.utils.resource.Resource;
import com.yanan.frame.plugin.PlugsFactory;

public interface ResourceDecoder<T extends Resource> {

	void decodeResource(PlugsFactory factory,T resource);

}
