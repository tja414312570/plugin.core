package com.YaNan.frame.plugin;

import com.YaNan.frame.plugin.event.InterestedEventSource;

public class PluginEventSource extends InterestedEventSource{
	@Override
	public String getName() {
		return PlugsFactory.getInstance().toString();
	}
}
