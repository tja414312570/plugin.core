package com.yanan.framework.plugin;

import com.yanan.framework.plugin.event.InterestedEventSource;

public class PluginEventSource extends InterestedEventSource{
	@Override
	public String getName() {
		return PlugsFactory.getInstance().toString();
	}
}