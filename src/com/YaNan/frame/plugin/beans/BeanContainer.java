package com.YaNan.frame.plugin.beans;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.YaNan.frame.plugin.PluginRuntimeException;
import com.YaNan.frame.plugin.PlugsFactory;
import com.YaNan.frame.plugin.RegisterDescription;


public class BeanContainer {
	private static volatile BeanContainer beanContext;
	private Map<String,Object> beanContainer;
	private Map<Class<?>,List<Object>> beanClassContainer;
	public static BeanContainer getContext(){
		if(beanContext==null)
			synchronized (BeanContainer.class) {
				if(beanContext==null){
					beanContext = new BeanContainer();
				}
			}
		return beanContext;
	}
	public void addBean(String id,Object bean,RegisterDescription description){
		if(this.beanContainer==null)
			synchronized (beanContext) {
				if(this.beanContainer==null){
					this.beanContainer = new HashMap<String, Object>();
					this.beanClassContainer = new HashMap<Class<?>, List<Object>>();
				}
			}
		if(bean==null)
			throw new RuntimeException("bean id \""+id+"\" is null");
		if(this.beanContainer.containsKey(id))
			throw new RuntimeException("bean id \""+id+"\" is exists!");
		this.beanContainer.put(id, bean);
		if(description.getPlugs() != null && description.getPlugs().length > 0) {
			this.addBean(description.getRegisterClass(), bean);
			for(Class<?> clzz: description.getPlugs()){
				this.addBean(clzz,bean);
			}
		}else {
			Class<?> tempClass = description.getRegisterClass();
			while(tempClass != null && !tempClass.equals(Object.class)) {
				this.addBean(tempClass, bean);
				for(Class<?> interfacer : tempClass.getInterfaces())
					this.addBean(interfacer, bean);
				tempClass = tempClass.getSuperclass();
			}
		}
	}
	private void addBean(Class<?> registerClass, Object bean) {
		List<Object> list = this.beanClassContainer.get(registerClass);
		if(list==null){
			try {
				BeanManagerLock.tryLock(registerClass);
				if(list==null){
					list = new LinkedList<Object>();
					this.beanClassContainer.put(registerClass, list);
				}
			}finally {
				BeanManagerLock.release(registerClass);
			}
			
		}
		list.add(bean);
	}
	@SuppressWarnings("unchecked")
	public <T> T getBean(String beanId) {
		checkPluginAvailable();
		T bean =  (T) this.beanContainer.get(beanId);
		if(bean == null)
			throw new PluginRuntimeException("could not found bean for id \""+beanId+"\"");
		return bean;
	}
	private void checkPluginAvailable() {
		if(this.beanContainer==null){
			PlugsFactory.init();
			if(this.beanContainer==null)
				throw new RuntimeException("bean context is not init or no bean defined");
		}
	}
	@SuppressWarnings("unchecked")
	public <T> T getBean(Class<?> beanClass) {
		List<Object> list = getBeans(beanClass);
		if(list==null)
			throw new PluginRuntimeException("could not found bean for class \""+beanClass+"\"");
		if(list.size()>1)
			throw new PluginRuntimeException("could not get bean for \""+beanClass+"\" cause the need one but found "+list.size());
		return (T) list.get(0);
	}
	public List<Object> getBeans(Class<?> beanClass) {
		checkPluginAvailable();
		return this.beanClassContainer.get(beanClass);
	}
	public void clear() {
		if(beanContainer!=null)
			beanContainer.clear();
		if(beanClassContainer!=null)
			beanClassContainer.clear();
	}
	public void removeBean(String beanId,Class<?> beanClass) {
		if(beanContainer!=null)
			beanContainer.remove(beanId);
//		if(beanClassContainer!=null)
//			beanClassContainer.remove(beanClass);
	}
}
