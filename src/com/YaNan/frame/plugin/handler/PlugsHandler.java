package com.YaNan.frame.plugin.handler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.YaNan.frame.plugin.definition.RegisterDefinition;
import com.YaNan.frame.utils.reflect.AppClassLoader;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;


/**
 * 组件实例处理器，用于对代理对象的处理，采用jdk方式
 * v1.1 支持cglib代理方式
 * v1.2 支持方法拦截
 * v1.2.1 修复方法拦截时无限调用bug
 * v1.2.2 添加获取RegisterDefinition方法
 * v1.2.3 添加每个MethodHandler对应的InvokeHandler
 * 
 * @author yanan
 *
 */
public class PlugsHandler implements InvocationHandler, MethodInterceptor {
	public static enum ProxyType {
		JDK, CGLIB
	}
	private Map<String,Object> attribute = new HashMap<String,Object>();
	private RegisterDefinition registerDefinition;// 注册描述类
	private Object proxyObject;// 代理对象
	private Class<?> proxyClass;// 代理类
	private Class<?> interfaceClass;// 接口类
	private ProxyType proxyType = ProxyType.JDK;// 代理模式
	
	/**
	 * return the proxy object
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T getProxyObject() {
		return (T) proxyObject;
	}

	public RegisterDefinition getRegisterDefinition() {
		return registerDefinition;
	}

	public void setRegisterDefinition(RegisterDefinition registerDefinition) {
		this.registerDefinition = registerDefinition;
	}

	/**
	 * return the proxy class
	 * 
	 * @return
	 */
	public Class<?> getProxyClass() {
		return proxyClass;
	}

	/**
	 * return the proxy interface
	 * 
	 * @return
	 */
	public Class<?> getInterfaceClass() {
		return interfaceClass;
	}
	/**
	 * jdk proxy PlugsHandler constructor
	 * 
	 * @param target
	 * @param mapperInterface
	 */
	public PlugsHandler(Object target, Class<?> mapperInterface, RegisterDefinition registerDefinition) {
		super();
		this.proxyObject = target;
		this.proxyClass = target.getClass();
		this.interfaceClass = mapperInterface;
		this.registerDefinition = registerDefinition;
	}

	/**
	 * cglib proxy PlugsHandler constructor
	 * 
	 * @param proxyClass
	 * @param parameters
	 */
	public PlugsHandler(Class<?> proxyClass,Class<?>[] parameterType, Object[] parameters, RegisterDefinition registerDefinition) {
		this.proxyClass = proxyClass;
		this.proxyType = ProxyType.CGLIB;
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(proxyClass);
		enhancer.setCallback(this);
		this.registerDefinition = registerDefinition;
		if (parameters.length == 0)
			this.proxyObject = enhancer.create();
		else
			this.proxyObject = enhancer.create(parameterType,
					parameters);
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable{
		// if the interface class is InvokeHandler class ,jump the method filter
		InvokeHandlerSet handler = null;
		if (registerDefinition != null) {
			handler = registerDefinition.getMethodHandler(method);
		}
		MethodHandler mh = null;
		if (handler != null) {
			mh = new MethodHandler(this, method, args);
			Iterator<InvokeHandlerSet> iterator = handler.iterator();
			InvokeHandlerSet hs;
			while (iterator.hasNext()) {
				hs = iterator.next();
				mh.setInvokeHandlerSet(hs);
				((InvokeHandler)hs.getInvokeHandler()).before(mh);
				if (!mh.isChain())
					return mh.getInterruptResult();
			}
		}
		try {
			method.setAccessible(true);
			Object result = method.invoke(this.proxyObject, args);
			method.setAccessible(false);
			if (handler != null) {
				mh.setOriginResult(result);
				Iterator<InvokeHandlerSet> iterator = handler.iterator();
				InvokeHandlerSet hs;
				while (iterator.hasNext()) {
					hs = iterator.next();
					mh.setInvokeHandlerSet(hs);
					((InvokeHandler)hs.getInvokeHandler()).after(mh);
					if (!mh.isChain())
						return mh.getInterruptResult();
				}
			}
			return result;
		} catch (Throwable e) {
			if (handler != null) {
				Iterator<InvokeHandlerSet> iterator = handler.iterator();
				InvokeHandlerSet hs;
				while (iterator.hasNext()) {
					hs = iterator.next();
					mh.setInvokeHandlerSet(hs);
					((InvokeHandler)hs.getInvokeHandler()).error(mh, e);
					if (!mh.isChain())
						return mh.getInterruptResult();
				}
			}
			throw processException(e);
		}
	}
	/**
	 * process the exception
	 * @param e
	 * @return
	 */
	private Throwable processException(Throwable e) {
		if(AppClassLoader.extendsOf(e.getClass(), InvocationTargetException.class)){
			InvocationTargetException exc = (InvocationTargetException) e;
			if(exc.getTargetException()!=null)
				e = exc.getTargetException();
		}
		return e;
	}

	@SuppressWarnings("unchecked")
	public static <T> T newMapperProxy(Class<T> mapperInterface, RegisterDefinition registerDefinition,
			Object target) {
		ClassLoader classLoader = mapperInterface.getClassLoader();
		Class<?>[] interfaces = new Class[1];
		interfaces[0] = mapperInterface;
		PlugsHandler plugsHandler = new PlugsHandler(target, mapperInterface, registerDefinition);
		return (T) Proxy.newProxyInstance(classLoader, interfaces, plugsHandler);
	}

	public static <T> T newCglibProxy(Class<?> proxyClass, RegisterDefinition registerDefinition,Class<?>[] parameterType,
			Object... parameters) {
		return new PlugsHandler(proxyClass, parameterType,parameters, registerDefinition).getProxyObject();
	}


	@Override
	public Object intercept(Object object, Method method, Object[] parameters, MethodProxy methodProxy)
			throws Throwable {
		InvokeHandlerSet handler = null;
		MethodHandler mh = null;
		if (registerDefinition != null) {
			handler = registerDefinition.getMethodHandler(method);
		}
		try {
		if (handler != null) {
			mh = new MethodHandler(this, method, parameters);
			Iterator<InvokeHandlerSet> iterator = handler.iterator();
			InvokeHandlerSet hs;
			while (iterator.hasNext()) {
				hs = iterator.next();
				mh.setInvokeHandlerSet(hs);
				((InvokeHandler)hs.getInvokeHandler()).before(mh);
				if (!mh.isChain())
					return mh.getInterruptResult();
			}
		}
			Object result = methodProxy.invokeSuper(object, parameters);
			if (handler != null) {
				mh.setOriginResult(result);
				Iterator<InvokeHandlerSet> iterator = handler.iterator();
				InvokeHandlerSet hs;
				while (iterator.hasNext()) {
					hs = iterator.next();
					mh.setInvokeHandlerSet(hs);
					((InvokeHandler)hs.getInvokeHandler()).after(mh);
					if (!mh.isChain())
						return mh.getInterruptResult();
				}

			}
			return result;
		}catch (Throwable e) {
			if (handler != null) {
				Iterator<InvokeHandlerSet> iterator = handler.iterator();
				InvokeHandlerSet hs;
				while (iterator.hasNext()) {
					hs = iterator.next();
					mh.setInvokeHandlerSet(hs);
					((InvokeHandler)hs.getInvokeHandler()).error(mh, e);
					if (!mh.isChain())
						return mh.getInterruptResult();
				}
			}
			throw e;
		}
	}

	public ProxyType getProxyType() {
		return proxyType;
	}

	public Map<String,Object> getAttributes() {
		return attribute;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getAttribute(String key) {
		return (T) attribute.get(key);
	}

	public void setAttribute(String key,Object value) {
		this.attribute.put(key, value);
	}
}