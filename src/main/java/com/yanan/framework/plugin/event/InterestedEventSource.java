package com.yanan.framework.plugin.event;

public class InterestedEventSource {
	public String getName() {
		return "event:"+this.hashCode();
	}
}