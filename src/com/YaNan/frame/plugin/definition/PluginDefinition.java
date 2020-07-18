package com.yanan.frame.plugin.definition;

import java.io.File;

import com.yanan.frame.plugin.annotations.Service;
import com.yanan.utils.reflect.AppClassLoader;

/**
 * 组件描述类
 * 用于创建组件时的组件信息
 * @author yanan
 *
 */
public class PluginDefinition {
	public PluginDefinition(Service service, Class<?> cls) {
		this.clzz = cls;
		this.service = service;
	}
	public PluginDefinition(File file) throws ClassNotFoundException, InstantiationException, IllegalAccessException{
		String fileName = file.getName();
		String clzzStr = fileName.substring(0,fileName.lastIndexOf("."));
		this.clzz= new AppClassLoader(clzzStr,false).getLoadedClass();
	}
	public PluginDefinition(Class<?> plugClass) {
		this.clzz = plugClass;
	}
	/**
	 * 组件类
	 */
	private Class<?> clzz;
	public Class<?> getPlugClass() {
		return clzz;
	}
	public void setClzz(Class<?> clzz) {
		this.clzz = clzz;
	}
	public Service getService() {
		return service;
	}
	/**
	 * Service 注解
	 */
	private Service service;
}
