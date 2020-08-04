package com.yanan.framework.plugin.handler;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.yanan.framework.plugin.definition.RegisterDefinition;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;


/**
 * 组件实例处理器，用于对代理对象的处理，采用jdk方式
 * v1.1 支持cglib代理方式
 * 
 * @author yanan
 *
 */
public class ProxyHandler implements MethodInterceptor {
	private volatile Map<Method,Method> methods = new HashMap<Method,Method>();
	private RegisterDefinition registerDefinition;// 注册描述类
	private Object proxyObject;// 代理对象
	private Class<?> proxyClass;// 代理类
	private Class<?> interfaceClass;// 接口类
	private Object linkObject;//链接对象

	/**
	 * return the proxy object
	 * 
	 * @return 代理对象
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
	 * @return 代理类
	 */
	public Class<?> getProxyClass() {
		return proxyClass;
	}

	/**
	 * return the proxy interface
	 * @return 调用接口
	 */
	public Class<?> getInterfaceClass() {
		return interfaceClass;
	}

	/**
	 * return the proxy method handler mapping
	 * @return the proxy method handler mapping
	 */
	public Map<Method, HandlerSet> getHandlerMapping() {
		return registerDefinition == null ? null : registerDefinition.getMethodInterceptMapping();
	}

	/**
	 * cglib proxy PlugsHandler constructor
	
	 * @param proxyClass 代理类
	 * @param parameterType 参数类型
	 * @param parameters 参数
	 * @param registerDefinition 注册定义
	 * @param linkProxy 连接代理
	 */
	public ProxyHandler(Class<?> proxyClass,Class<?>[] parameterType, Object[] parameters, RegisterDefinition registerDefinition,Object linkProxy) {
		this.proxyClass = proxyClass;
		this.linkObject = linkProxy;
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


	public static <T> T newCglibProxy(Class<?> proxyClass, RegisterDefinition registerDefinition,Class<?>[] parameterType,Object linkProxy,
			Object... parameters) {
		return new ProxyHandler(proxyClass, parameterType,parameters, registerDefinition,linkProxy).getProxyObject();
	}

	@Override
	public Object intercept(Object object, Method method, Object[] parameters, MethodProxy methodProxy)
			throws Throwable {
			Method linkMethod = this.methods.get(method);
			if(linkMethod==null){
				synchronized (this) {
					if(linkMethod==null){
//						RegisterDefinition linkRegister = this.registerDefinition.getLinkRegister();
						linkMethod = linkObject.getClass().getMethod(method.getName(), method.getParameterTypes());//linkRegister.getRegisterClass()
						this.methods.put(method, linkMethod);
					}
				}
			}
			return linkMethod.invoke(linkObject, parameters);
	}
}