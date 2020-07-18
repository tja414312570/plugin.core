package com.YaNan.frame.plugin.decoder;

import com.YaNan.frame.plugin.PlugsFactory;
import com.YaNan.frame.utils.resource.Resource;

public interface ResourceDecoder<T extends Resource> {

	void decodeResource(PlugsFactory factory,T resource);

}
