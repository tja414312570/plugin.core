package com.yanan.framework.plugin.handler;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.yanan.utils.reflect.ReflectUtils;

import net.sf.cglib.proxy.MethodProxy;

/**
 * 方法处理器
 * v1.0 支持方法信息的传递
 * v1.1 20180910 新增每个MethodHandler进入Handler处理时、
 * 		对应的Handler的获取
 * @author yanan
 *
 */
public class MethodHandler {
	private PlugsHandler plugsProxy; 
	private Method method;
	private HandlerSet invokeHandlerSet;
	private MethodProxy methodProxy;
	private Object[] parameters;
	private boolean chain=true;
	private Map<String,Object> attribute = new HashMap<String,Object>();
	private Object interruptResult;
	private Object proxy;
	private HandlerSet handlerSet;

	public boolean isChain() {
		return chain;
	}
	public void setChain(boolean chain) {
		this.chain = chain;
	}
	public MethodHandler(PlugsHandler plugsProxy, Method method, Object[] args, Object proxy,HandlerSet handlerSet) {
		this.plugsProxy = plugsProxy;
		this.method = method;
		this.parameters = args;
		this.proxy = proxy;
		this.handlerSet = handlerSet;
	}
	public Object getProxy() {
		return proxy;
	}
	public MethodHandler(PlugsHandler plugsProxy, Method method, MethodProxy methodProxy, Object[] args, Object proxy,HandlerSet handlerSet) {
		this.plugsProxy = plugsProxy;
		this.methodProxy = methodProxy;
		this.method = method;
		this.parameters = args;
		this.proxy = proxy;
		this.handlerSet = handlerSet;
	}
	/**
	 * 获取方法的组件处理器
	 * @return 组件Handler
	 */
	public PlugsHandler getPlugsProxy() {
		return plugsProxy;
	}

	public void setPlugsProxy(PlugsHandler plugsProxy) {
		this.plugsProxy = plugsProxy;
	}
	/**
	 * 获取拦截的方法
	 * @return 执行的方法
	 */
	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}
	/**
	 * 获取拦截的方法的参数
	 * @return 参数
	 */
	public Object[] getParameters() {
		return parameters;
	}

	public void setParameters(Object[] parameters) {
		this.parameters = parameters;
	}

	public Object getResult() {
		return null;
	}
	/**
	 * 中断返回数据，中断之后代理的方法和之后的拦截器都不再工作
	 * @param result 作为原方法的返回结果
	 */
	public void interrupt(Object result){
		this.chain = false;
		this.setInterruptResult(result) ;
	}

	@SuppressWarnings("unchecked")
	public <T> T getAttribute(String key) {
		return (T) attribute.get(key);
	}
	/**
	 * 设置此次方法执行的属性，之后的相关拦截器都能获取到此方法此次执行的该参数
	 * @param key 属性名
	 * @param value 属性值
	 */
	public void addAttribute(String key,Object value) {
		this.attribute.put(key, value);
	}
	/**
	 * 获取方法的拦截链
	 * @return 调用handler的集合
	 */
	public HandlerSet getHandlerSet() {
		return invokeHandlerSet;
	}
	public void setHandlerSet(HandlerSet invokeHandlerSet) {
		this.invokeHandlerSet = invokeHandlerSet;
	}
	/**
	 * 获取中断结果
	 * @return 中断结果
	 */
	public Object getInterruptResult() {
		return interruptResult;
	}
	public void setInterruptResult(Object interruptResult) {
		this.interruptResult = interruptResult;
	}
	public Object invoke() throws Throwable{
		HandlerSet next = this.handlerSet;
		if(next == null) {
			if(this.methodProxy != null) {
				return methodProxy.invokeSuper(proxy, parameters);
			}else {
				return ReflectUtils.invokeMethod(this.getPlugsProxy().getProxyObject(), method, parameters);
			}
		}
		if(handlerSet != null) {
			this.handlerSet = this.handlerSet.getNext();
		}
		return ((InvokeHandler)next.getHandler()).around(this);
	}
}