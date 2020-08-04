package com.yanan.framework.plugin.handler;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import com.yanan.framework.plugin.PlugsFactory;

/**
 * 拦截器链表
 * v1.0 实现Handler的链表结构
 * v1.1 添加属性annotation，用于对特殊注解的Handler的
 * 		快速与便捷处理方式
 * 
 * @author yanan
 */
public class HandlerSet {
	private Object handler;
	private Map<Class<?>,Object> annotations;
	private HandlerSet first;
	private HandlerSet before;
	private HandlerSet last;
	private HandlerSet next;
	
	public void addAnnotation(Annotation annos){
		if(this.annotations==null){
			this.annotations = new HashMap<Class<?>,Object>();
		}
		this.annotations.put(annos.annotationType(),annos);
	}
	public void setAnnotations(Map<Class<?>, Object> annotations) {
		this.annotations = annotations;
	}
	@SuppressWarnings("unchecked")
	public <T> T getAnnotation(Class<?> annoClzz){
		return this.annotations==null?null:
			(T)this.annotations.get(annoClzz);
	}
	@SuppressWarnings("unchecked")
	public <T> Collection<T> getAnnotations(){
		return this.annotations==null?null:
			(Collection<T>)this.annotations.values();
	}
	public HandlerSet(Object handler){
		this.handler=handler;
		this.first = this;
		this.last = this;    
	}
	
	public void addHandlerSet(HandlerSet handlerSet){
		handlerSet.setBefore(this);
		handlerSet.setFirst(this.first);
		this.setNext(handlerSet);
		this.setLast(handlerSet);
	}
	public HandlerSet getBefore() {
		return before;
	}
	public Iterator<HandlerSet> iterator(){
		return new HandlerItertor(this);
	}

	void setBefore(HandlerSet before) {
		this.before = before;
	}
	@SuppressWarnings("unchecked")
	public <T> T getHandler() {
		return (T) handler;
	}
	public void setHandler(Object handler) {
		this.handler = handler;
	}
	public HandlerSet getLast() {
		return last;
	}
	public void setLast(HandlerSet last) {
		this.last = last;
		if(this.before!=null)
			this.before.setLast(last);
	}
	public HandlerSet getNext() {
		return next;
	}
	public void setNext(HandlerSet next) {
		this.next = next;
	}
	
	public HandlerSet getFirst() {
		return first;
	}

	public void setFirst(HandlerSet first) {
		this.first = first;
	}
	
	class HandlerItertor implements Iterator<HandlerSet>{
		private HandlerSet current=null;
		private HandlerSet next=null;
		public HandlerItertor(HandlerSet handlerSet){
			next = handlerSet;
		}
		@Override
		public boolean hasNext() {
			return next!=null;
		}
		@Override
		public HandlerSet next() {
			current = next;
			next = next.getNext();
			return current;
		}
		
	}

	public boolean hasHandlerSet(HandlerSet handlerSet) {
		Iterator<HandlerSet> iterator = this.iterator();
		while(iterator.hasNext()) {
			HandlerSet current = iterator.next();
			Class<?> currentInvokeClass = PlugsFactory.getPluginsHandler(current.getHandler()).getProxyClass();
			Class<?> invokeClass = PlugsFactory.getPluginsHandler(handlerSet.getHandler()).getProxyClass();
			if(Objects.equals(currentInvokeClass,invokeClass))
				return true;
		}
		return false;
	}
}