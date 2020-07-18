package com.yanan.frame.plugin.builder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.typesafe.config.ConfigValue;
import com.yanan.frame.plugin.ParameterUtils;
import com.yanan.frame.plugin.PlugsFactory;
import com.yanan.frame.plugin.ProxyModel;
import com.yanan.frame.plugin.builder.resolver.DelayParameterResolver;
import com.yanan.frame.plugin.builder.resolver.ParameterResolver;
import com.yanan.frame.plugin.definition.FieldDefinition;
import com.yanan.frame.plugin.definition.MethodDefinition;
import com.yanan.frame.plugin.definition.RegisterDefinition;
import com.yanan.frame.plugin.exception.PluginRuntimeException;
import com.yanan.frame.plugin.handler.FieldHandler;
import com.yanan.frame.plugin.handler.InstanceHandler;
import com.yanan.frame.plugin.handler.InvokeHandler;
import com.yanan.frame.plugin.handler.InvokeHandlerSet;
import com.yanan.frame.plugin.handler.PlugsHandler;
import com.yanan.utils.reflect.AppClassLoader;
import com.yanan.utils.reflect.cache.ClassHelper;
import com.yanan.utils.reflect.cache.ClassInfoCache;
import com.yanan.utils.string.StringUtil;

public class PluginInstanceFactory {
	
	/**
	 * 通过参数类型获取构造器
	 * 
	 * @param paramTypes
	 * @return
	 */
	public static Constructor<?> getConstructor(RegisterDefinition registerDefinition,Class<?>[] paramTypes) {
		// 排除掉数量不同的构造器
		Constructor<?> constructor = ParameterUtils
				.getEffectiveConstructor(ClassHelper.getClassHelper(registerDefinition.getRegisterClass())
						.getConstructors(), paramTypes);
		if (constructor == null) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < paramTypes.length; i++) {
				sb.append(paramTypes[i] == null ? null : paramTypes[i].getName())
						.append(i < paramTypes.length - 1 ? "," : "");
			}
			throw new PluginRuntimeException("constructor " + registerDefinition.getRegisterClass().getSimpleName() + "(" + sb.toString()
					+ ") is not exist at " + registerDefinition.getRegisterClass().getName());
		}
		return constructor;
	}
	public static Constructor<?> getConstructor(RegisterDefinition registerDefinition,Object... args) {
		Class<?>[] parameterTypes = AppClassLoader.getParameterTypes(args);
		Constructor<?> constructor = null;
		try {
			constructor = getConstructor(registerDefinition,parameterTypes);
		} catch (Throwable t) {
			Iterator<Constructor<?>> iterator = ClassInfoCache.getClassHelper(registerDefinition.getRegisterClass())
					.getConstructorHelperMap().keySet().iterator();
			while (iterator.hasNext()) {
				Constructor<?> con = iterator.next();
				Class<?>[] matchType = con.getParameterTypes();
				if (matchType.length != args.length)
					continue;
				if (AppClassLoader.matchType(matchType, parameterTypes)) {
					constructor = con;
					break;
				}
			}
			if (constructor == null)
				throw t;
		}
		return constructor;
	}
	
	/**
	 * 获取一个服务的新对像
	 * 
	 * @param plug
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public static <T> T getRegisterNewInstance(RegisterDefinition registerDefinition,Class<T> service, Object[] args,Object origin){
		// 获取构造器
		Constructor<?> constructor = getConstructor(registerDefinition,origin == null?args:new Class<?>[0]);
		return getNewInstance(registerDefinition,service, constructor, args,origin);
	}

	public static <T> T getRegisterNewInstanceByParamType(RegisterDefinition registerDefinition,Class<T> service, 
			Class<?>[] paramTypes, Object... args){
		// 获取构造器
		Constructor<?> constructor = getConstructor(registerDefinition,paramTypes);
		// 获取构造器拦截器
		return getNewInstance(registerDefinition,service, constructor, args,null);
	}
	
	/**
	 * 获取服务的实例
	 * 
	 * @param plug
	 * @param args
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getRegisterInstance(RegisterDefinition registerDefinition,Class<T> service, Object... args){
		Object proxy = null;
		MethodDefinition methodDefinition = null;
		//如果存在引用  获取引用对象
		if(!StringUtil.isEmpty(registerDefinition.getReferenceId())) {
			proxy = PlugsFactory.getPluginsInstance(registerDefinition.getReferenceId());
		}
		//如果有方法 表明代理对象从方法中获取
		if((methodDefinition = registerDefinition.getInstanceMethod()) != null){
			try {
				if(methodDefinition.getResolvers()!=null) {
					ParameterResolver<ConfigValue> parameterResolver;
					for(int i = 0;i<methodDefinition.getResolvers().length;i++) {
						if((parameterResolver = methodDefinition.getResolvers()[i])!=null) {
							methodDefinition.getArgs()[i] = parameterResolver.
									resove((ConfigValue) methodDefinition.getArgs()[i], methodDefinition.getType()[i], i, registerDefinition);
						}
					}
				}
				proxy = methodDefinition.getMethod().invoke(proxy, methodDefinition.getArgs());
				//如果有要执行的方法，强制转换为cglib模式 ，并从新生成代理对象
				if(registerDefinition.getAfterInstanceExecuteMethod() != null
						|| registerDefinition.getAfterInstanceInitField() != null) {
					registerDefinition.setProxyModel(ProxyModel.CGLIB);
					proxy = getRegisterInstance0(registerDefinition, service, args,proxy);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				String name = StringUtil.isEmpty(registerDefinition.getId())?registerDefinition.getRegisterClass().getName():registerDefinition.getId();
				throw new PluginRuntimeException("failed to instance "+name+" by method "+methodDefinition,e);
			}
		}else{
			proxy = getRegisterInstance0(registerDefinition,service, args,null);
		}
		return (T) proxy;
	}

	private static Object getRegisterInstance0(RegisterDefinition registerDefinition,Class<?> plug, Object[] args,Object origin){
		Object proxy;
		// 判断是否单例
		if (registerDefinition.isSignlton()) {
			int hash = hash(plug, StringUtil.isEmpty(registerDefinition.getId())?args:registerDefinition.getId());
			proxy = registerDefinition.getProxyInstance(hash);
			if (proxy == null)
				proxy = getRegisterNewInstance(registerDefinition,plug, args,origin);
			registerDefinition.setProxyInstance(hash, proxy);
		} else {
			proxy = getRegisterNewInstance(registerDefinition,plug, args,origin);
		}
		return proxy;
	}
	@SuppressWarnings("unchecked")
	public static <T> T getRegisterInstanceByParamType(RegisterDefinition registerDefinition,Class<T> plug, Class<?>[] paramType, Object... args) {
		Object proxy = null;
		// 判断是否单例
		if (registerDefinition.isSignlton()) {
			int hash = hash(plug, StringUtil.isEmpty(registerDefinition.getId())?args:registerDefinition.getId());
			proxy = registerDefinition.getProxyInstance(hash);
			if (proxy == null)
				proxy = getRegisterNewInstanceByParamType(registerDefinition,plug, paramType, args);
			registerDefinition.setProxyInstance(hash, proxy);
		} else {
			proxy = getRegisterNewInstanceByParamType(registerDefinition,plug, paramType, args);
		}
		return (T) proxy;
	}
	public static int hash(Object... objects) {
		int hash = 0;
		for (int i = 0; i < objects.length; i++)
			hash += objects[i].hashCode();
		return hash;
	}
	@SuppressWarnings("unchecked")
	public static <T> T getNewInstance(RegisterDefinition registerDefinition,Class<T> service, Constructor<?> constructor, Object[] args,Object origin) {
//		if (registerDefinition.getLinkRegister() != null) {// 如果有链接的注册器
//			// 获取链接类的相同构造器
//			try {
//				if (registerDefinition.getLinkProxy() == null) {
//					PluginInstanceFactory.getRegisterNewInstance(registerDefinition, plug, args)
//					Object linkObject = this.linkRegister.getRegisterInstance(plug, args);
//					this.linkProxy = ProxyHandler.newCglibProxy(this.getRegisterClass(), this,
//							constructor.getParameterTypes(), linkObject, args);
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			return (T) linkProxy;
//		}
		Object proxy = null;
		Object target = null;
		InvokeHandlerSet constructorInvokeHanderSet = null;
		InstanceHandler handler = null;
		try {
			if (constructor != null) {
				constructorInvokeHanderSet = registerDefinition.getConstructorHandler(constructor);
				if (constructorInvokeHanderSet != null) {
					Iterator<InvokeHandlerSet> handlerIterator = constructorInvokeHanderSet.iterator();
					while (handlerIterator.hasNext()) {
						handler = (InstanceHandler) handlerIterator.next().getInvokeHandler();
						handler.before(registerDefinition, service, constructor, args);
					}
				}
			}
			// 实例化对象
			if (service.isInterface()) {
				switch (registerDefinition.getProxyModel()) {
				case DEFAULT:
					target = constructor.newInstance(args);
					proxy = PlugsHandler.newMapperProxy(service, registerDefinition, target);
					break;
				case JDK:
					target = constructor.newInstance(args);
					proxy = PlugsHandler.newMapperProxy(service, registerDefinition, target);
					break;
				case CGLIB:
					target = proxy = PlugsHandler.newCglibProxy(registerDefinition.getRegisterClass(), registerDefinition,
							constructor.getParameterTypes(), args);
					break;
				case BOTH:
					target = PlugsHandler.newCglibProxy(registerDefinition.getRegisterClass(), registerDefinition, constructor.getParameterTypes(),
							args);
					proxy = PlugsHandler.newMapperProxy(service, registerDefinition, target);
					break;
				default:
					target = constructor.newInstance(args);
					if (service.equals(InvokeHandler.class) || service.equals(InstanceHandler.class))
						proxy = target;
					else
						proxy = PlugsHandler.newMapperProxy(service, registerDefinition, target);
					break;
				}
			} else {
				target = proxy = PlugsHandler.newCglibProxy(registerDefinition.getRegisterClass(), registerDefinition,
						constructor.getParameterTypes(), args);
			}
			if(origin != null) {
				AppClassLoader.DisClone(proxy, origin);
			}
			//属性处理
			
			if (registerDefinition.getFieldInterceptMapping() != null) {
				FieldHandler fieldHandler;
				Map<Field, InvokeHandlerSet> fieldInterceptMapping = registerDefinition.getFieldInterceptMapping();
				Iterator<Entry<Field, InvokeHandlerSet>> fieldInterceptIterator = fieldInterceptMapping.entrySet().iterator();
				while(fieldInterceptIterator.hasNext()) {
					Entry<Field, InvokeHandlerSet> entry = fieldInterceptIterator.next();
					Field field = entry.getKey();
					InvokeHandlerSet filedHandlerSet = entry.getValue();
					Iterator<InvokeHandlerSet> fieldIterator = filedHandlerSet.iterator();
					while (fieldIterator.hasNext()) {
						InvokeHandlerSet  fieldInvokeHandlerSet = fieldIterator.next();
						fieldHandler = fieldInvokeHandlerSet.getInvokeHandler();
						fieldHandler.preparedField(registerDefinition, proxy, target,fieldInvokeHandlerSet, field);
					}
				}
			}
			// 调用代理完成
			if (constructorInvokeHanderSet != null) {
				Iterator<InvokeHandlerSet> handlerIterator = constructorInvokeHanderSet.iterator();
				while (handlerIterator.hasNext()) {
					handler = (InstanceHandler) handlerIterator.next().getInvokeHandler();
					handler.after(registerDefinition, service, constructor, target, args);
				}
			}
			initProxyField(registerDefinition,proxy);
			initProxyMethod(registerDefinition,proxy);
		} catch (Throwable t) {
			PluginRuntimeException exception = t.getClass().equals(PluginRuntimeException.class)
					? (PluginRuntimeException) t
					: new PluginRuntimeException(t);
			if (constructorInvokeHanderSet != null) {
				if (handler != null)
					handler.exception(registerDefinition, service, constructor, proxy, exception, args);
				if (!exception.isInterrupt()) {
					Iterator<InvokeHandlerSet> handlerIterator = constructorInvokeHanderSet.iterator();
					InstanceHandler i;
					while (handlerIterator.hasNext() && !exception.isInterrupt()) {
						i = (InstanceHandler) handlerIterator.next().getInvokeHandler();
						if (i == handler)
							continue;
						i.exception(registerDefinition, service, constructor, proxy, exception, args);
					}
				}

			}
			if (!exception.isInterrupt())
				throw exception;
		}

		return (T) proxy;
	}
	/**
	 * 代理实例化后调用方法
	 * 
	 * @param proxy
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public static void initProxyMethod(RegisterDefinition registerDefinition,Object proxy)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (registerDefinition.getAfterInstanceExecuteMethod() != null) {
			for (MethodDefinition methodDefinition : registerDefinition.getAfterInstanceExecuteMethod()) {
				methodDefinition.getMethod().setAccessible(true);
				methodDefinition.getMethod().invoke(proxy);
				methodDefinition.getMethod().setAccessible(false);
			}
		}
	}
	/**
	 * 代理实例化后调用方法
	 * 
	 * @param proxy
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 */
	@SuppressWarnings("unchecked")
	public static void initProxyField(RegisterDefinition registerDefinition,Object proxy)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		if (registerDefinition.getAfterInstanceInitField() != null) {
			for (FieldDefinition fieldDefinition : registerDefinition.getAfterInstanceInitField()) {
				if(fieldDefinition.getResolver() != null)
					fieldDefinition.setValue(((DelayParameterResolver<ConfigValue>)fieldDefinition.getResolver())
							.resove((ConfigValue)fieldDefinition.getValue(), fieldDefinition.getType(), 0, registerDefinition));
				try {
					new AppClassLoader(proxy).set(fieldDefinition.getField(), fieldDefinition.getValue());
				}catch(Throwable t) {
					String type = fieldDefinition.getValue() == null? "null":fieldDefinition.getValue().getClass().getName();
					throw new PluginRuntimeException("faied to init field "+registerDefinition.getRegisterClass().getName()
							+"."+fieldDefinition.getField().getName()+" with ["+fieldDefinition.getValue()+"] type ["
							+type+"]",t);
				}
				
			}
		}
	}
}
