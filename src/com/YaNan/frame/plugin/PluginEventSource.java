package com.yanan.frame.plugin;

import com.yanan.frame.plugin.event.InterestedEventSource;

public class PluginEventSource extends InterestedEventSource{
	@Override
	public String getName() {
		return PlugsFactory.getInstance().toString();
	}
}
