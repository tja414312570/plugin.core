package com.yanan.framework.plugin.autowired.plugin;

/**
 * 自定义bean接口
 * 提供用户自定义用户需要的类型bean的注册
 * @author yanan
 * @param <T>
 */
public interface CustomProxy<T> {
	T getInstance();
}
