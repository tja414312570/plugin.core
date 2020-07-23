package com.yanan.frame.plugin;

import com.yanan.frame.plugin.event.AbstractEvent;

/**
 * 组件事件
 * @author yanan
 *
 */
public class PluginEvent implements AbstractEvent{
	public static enum EventType{
		init,add_resource,refresh,completed,instation,add_registerDefinition, register_init,inited
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
}
