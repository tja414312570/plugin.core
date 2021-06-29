package com.yanan.framework.plugin;

import com.yanan.framework.plugin.event.AbstractEvent;

/**
 * 组件事件
 * @author yanan
 *
 */
public class PluginEvent implements AbstractEvent{
	public static enum EventType{
		init,add_resource,refresh,completed,instation,add_registerDefinition, register_init,inited, add_pluginDefinition
	}
	private EventType eventType;
	private Object eventContent;
	public EventType getEventType() {
		return eventType;
	}
	public PluginEvent(EventType eventType, Object eventContent) {
		super();
		this.eventType = eventType;
		this.eventContent = eventContent;
	}
	public void setEventType(EventType eventType) {
		this.eventType = eventType;
	}
	public Object getEventContent() {
		return eventContent;
	}
	public void setEventContent(Object eventContent) {
		this.eventContent = eventContent;
	}
	@Override
	public String toString() {
		return "envent:"+this.eventType+",content:"+this.eventContent;
	}
}