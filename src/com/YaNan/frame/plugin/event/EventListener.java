package com.yanan.frame.plugin.event;

/**
 * 统一事件接口
 * @author yanan
 *
 */
@FunctionalInterface
public interface EventListener<T extends AbstractEvent> {
	void onEvent(T abstractEvent);
}
