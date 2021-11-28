package com.yanan.framework.plugin.builder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.typesafe.config.ConfigValue;
import com.yanan.framework.plugin.ExtReflectUtils;
import com.yanan.framework.plugin.InstanceAfterProcesser;
import com.yanan.framework.plugin.InstanceBeforeProcesser;
import com.yanan.framework.plugin.InstanceProcess;
import com.yanan.framework.plugin.PlugsFactory;
import com.yanan.framework.plugin.ProxyModel;
import com.yanan.framework.plugin.autowired.plugin.CustomProxy;
import com.yanan.framework.plugin.builder.resolver.DelayParameterResolver;
import com.yanan.framework.plugin.builder.resolver.ParameterResolver;
import com.yanan.framework.plugin.definition.ConstructorDefinition;
import com.yanan.framework.plugin.definition.FieldDefinition;
import com.yanan.framework.plugin.definition.MethodDefinition;
import com.yanan.framework.plugin.definition.RegisterDefinition;
import com.yanan.framework.plugin.exception.PluginRuntimeException;
import com.yanan.framework.plugin.handler.FieldHandler;
import com.yanan.framework.plugin.handler.Handler;
import com.yanan.framework.plugin.handler.HandlerSet;
import com.yanan.framework.plugin.handler.InstanceHandler;
import com.yanan.framework.plugin.handler.PlugsHandler;
import com.yanan.utils.ArrayUtils;
import com.yanan.utils.asserts.Assert;
import com.yanan.utils.reflect.AppClassLoader;
import com.yanan.utils.reflect.ParameterUtils;
import com.yanan.utils.reflect.cache.ClassHelper;
import com.yanan.utils.string.StringUtil;

public class PluginInstanceFactory {

//	/**
//	 * 通过参数类型获取构造器
//	 * 
//	 * @param paramTypes
//	 * @return 构造器
//	 */
//	public static Constructor<?> getConstructorsss(RegisterDefinition registerDefinition, Class<?>[] paramTypes) {
//		// 排除掉数量不同的构造器
//		System.out.println(Modifier.toString(registerDefinition.getRegisterClass().getModifiers()));
//		System.out.println(registerDefinition.getRegisterClass()+"===>"+Arrays.toString(ClassHelper.getClassHelper(registerDefinition.getRegisterClass()).getDeclaredConstructors()));
//		Constructor<?> constructor = ParameterUtils.getEffectiveConstructor(registerDefinition.getRegisterClass(),
//				ClassHelper.getClassHelper(registerDefinition.getRegisterClass()).getDeclaredConstructors(), paramTypes);
//		if (constructor == null) {
//			StringBuilder sb = new StringBuilder();
//			for (int i = 0; i < paramTypes.length; i++) {
//				sb.append(paramTypes[i] == null ? null : paramTypes[i].getName())
//						.append(i < paramTypes.length - 1 ? "," : "");
//			}
//			throw new PluginRuntimeException("constructor " + registerDefinition.getRegisterClass().getSimpleName()
//					+ "(" + sb.toString() + ") is not exist at " + registerDefinition.getRegisterClass().getName());
//		}
//		return constructor;
//	}
//
//	public static Constructor<?> getConstructorss(RegisterDefinition registerDefinition, Object... args) {
//		Class<?>[] parameterTypes = AppClassLoader.getParameterTypes(args);
//		Constructor<?> constructor = null;
//		try {
//			constructor = getConstructor(registerDefinition, parameterTypes);
//		} catch (Throwable t) {
//			Iterator<Constructor<?>> iterator = ClassInfoCache.getClassHelper(registerDefinition.getRegisterClass())
//					.getConstructorHelperMap().keySet().iterator();
//			while (iterator.hasNext()) {
//				Constructor<?> con = iterator.next();
//				Class<?>[] matchType = con.getParameterTypes();
//				if (matchType.length != args.length)
//					continue;
//				if (AppClassLoader.matchType(matchType, parameterTypes)) {
//					constructor = con;
//					break;
//				}
//			}
//			if (constructor == null)
//				throw t;
//		}
//		return constructor;
//	}

	/**
	 * 获取一个服务的新对像
	 * 
	 * @param registerDefinition 注册定义
	 * @param service 服务类
	 * @param args 参数
	 * @param origin 原始对象
	 * @return 实例
	 */
	public static <T> T getRegisterNewInstance(RegisterDefinition registerDefinition, Class<T> service, Object[] args,
			Object origin) {
		ConstructorDefinition constructorDefinition = registerDefinition.getInstanceConstructor();
		// 获取构造器
		if (constructorDefinition == null) {
			constructorDefinition = builderConstructorDefinition(registerDefinition,args);
		}
		return getInstance(registerDefinition,constructorDefinition,service,origin);
	}

	private static <T> T getInstance(RegisterDefinition registerDefinition,ConstructorDefinition constructorDefinition, Class<T> service, Object origin) {
		checkPreparedParameter(constructorDefinition, registerDefinition);
		return getNewInstance(registerDefinition, service,
				constructorDefinition.getConstructor(), 
				constructorDefinition.getArgs(), origin);
}

	private static ConstructorDefinition builderConstructorDefinition(RegisterDefinition registerDefinition,
			Object[] args) {
		Class<?>[] parameterTypes = ParameterUtils.getParameterTypes(args);
		return builderConstructorDefinition(registerDefinition,parameterTypes,args);
	}
	private static ConstructorDefinition builderConstructorDefinition(RegisterDefinition registerDefinition,
			Class<?>[] types,Object[] args) {
		// 排除掉数量不同的构造器
		ConstructorDefinition constructorDefinition = new ConstructorDefinition(null, types, args, null, null);
		ExtReflectUtils.getEffectiveConstructor(constructorDefinition, registerDefinition.getRegisterClass());
		return constructorDefinition;
	}
	public static <T> T getRegisterNewInstanceByParamType(RegisterDefinition registerDefinition, Class<T> service,
			Class<?>[] paramTypes, Object... args) {
		// 获取构造器
		ConstructorDefinition constructorDefinition = builderConstructorDefinition(registerDefinition,paramTypes,args);
		// 获取构造器拦截器
		return getInstance(registerDefinition,constructorDefinition,service,null);
	}

	/**
	 * 获取注册实例
	 * 
	 * @param registerDefinition 注册定义
	 * @param service            服务类
	 * @param args               参数
	 * @return 注册实例
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getRegisterInstance(RegisterDefinition registerDefinition, Class<T> service, Object... args) {
		Object proxy = null;
		// 判断是否单例
		if (registerDefinition.isSignlton()) {
			// 如果有ID属性，则hash为id的hash值
			int hash = StringUtil.isEmpty(registerDefinition.getId()) ? 
					!registerDefinition.isRelyService()?hash(args):hash(service, args)
					: hash(registerDefinition.getId());
			// 从容器获取属性
			proxy = registerDefinition.getProxyInstance(hash);
			// dcl
			if (proxy == null) {
				synchronized (registerDefinition) {
					if (proxy == null) {
						// 获取新的实例
						proxy = getRegisterInstance0(registerDefinition, service, args);
						// 将实例加入到代理列表
						registerDefinition.setProxyInstance(hash, proxy);
					}
				}
			}
		} else {
			proxy = getRegisterInstance0(registerDefinition, service, args);
		}
		return (T) proxy;
	}

	private static Object getRegisterInstance0(RegisterDefinition registerDefinition, Class<?> plug, Object[] args) {
		Object proxy = null;
		// 如果存在引用 获取引用对象
		if (!StringUtil.isEmpty(registerDefinition.getReferenceId())) {
			proxy = PlugsFactory.getPluginsInstance(registerDefinition.getReferenceId());
		}
		// 判断是否使用方法构造器
		MethodDefinition methodDefinition = registerDefinition.getInstanceMethod();
		if (methodDefinition != null) {
			try {
				// 运行时参数检查
				checkPreparedParameter(methodDefinition, registerDefinition);
				// 调用方法获取参数
				proxy = methodDefinition.getMethod().invoke(proxy, methodDefinition.getArgs());
				// 如果有要执行的方法，强制转换为cglib模式 ，并从新生成代理对象
//					registerDefinition.setProxyModel(ProxyModel.CGLIB);
//					if(registerDefinition.getAfterInstanceExecuteMethod() != null
//							|| registerDefinition.getAfterInstanceInitField() != null) {
//						registerDefinition.setProxyModel(ProxyModel.CGLIB);
//						origin = getRegisterInstance0(registerDefinition, plug, args,proxy);
//					}
			} catch (Throwable e) {
				String name = StringUtil.isEmpty(registerDefinition.getId())
						? registerDefinition.getRegisterClass().getName()
						: registerDefinition.getId();
				throw new PluginRuntimeException("failed to instance " + name + " by method " + methodDefinition, e);
			}
		}
		if ((StringUtil.equals(registerDefinition.getId(), registerDefinition.getReferenceId()) && proxy != null)
				|| Modifier.isFinal(registerDefinition.getRegisterClass().getModifiers())
				|| (registerDefinition.getInstanceMethod() != null
						&& registerDefinition.getProxyModel() == ProxyModel.NONE)) {
			proxy = getNewInstance(registerDefinition, plug, null, null, proxy);
		} else {
			proxy = getRegisterNewInstance(registerDefinition, plug, args, proxy);
		}
		
//		//如果引用ID和注册ID相同   则说明直接引用对象
//		if(StringUtil.equals(registerDefinition.getId(), registerDefinition.getReferenceId()) && origin != null) {
//			proxy = getNewInstance(registerDefinition,plug, null,null,origin);
//		}else {
//			// 判断是否单例
//			if (registerDefinition.isSignlton()) {
//				
//				
//				if (proxy == null) {
//					
//					//如果有方法 表明代理对象从方法中获取
//					if(methodDefinition != null){
//						try {
//							checkPreparedParameter(methodDefinition,registerDefinition);
//							proxy = methodDefinition.getMethod().invoke(proxy, methodDefinition.getArgs());
//							//如果有要执行的方法，强制转换为cglib模式 ，并从新生成代理对象
//							
//						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
//							String name = StringUtil.isEmpty(registerDefinition.getId())?registerDefinition.getRegisterClass().getName():registerDefinition.getId();
//							throw new PluginRuntimeException("failed to instance "+name+" by method "+methodDefinition,e);
//						}
//					}
//					Class<?> temp = null;
//					try {
//						if(origin != null) {
//							temp = registerDefinition.getRegisterClass();
//							registerDefinition.setRegisterClass(origin.getClass());
//						}
//						proxy = getRegisterNewInstance(registerDefinition,plug, args,origin);
//					}finally {
//						if(temp != null)
//						 registerDefinition.setRegisterClass(temp);
//					}
//				}
//				
//			} else {
//				proxy = getRegisterNewInstance(registerDefinition,plug, args,origin);
//			}
//		}
	return proxy;

	}

	/**
	 * 获取注册器实例
	 * 
	 * @param registerDefinition 注册定义
	 * @param plug               组价类
	 * @param paramType          参数类型
	 * @param args               参数
	 * @return 组件实例
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getRegisterInstanceByParamType(RegisterDefinition registerDefinition, Class<T> plug,
			Class<?>[] paramType, Object... args) {
		Object proxy = null;
		// 判断是否单例
		if (registerDefinition.isSignlton()) {
			int hash = hash(plug, StringUtil.isEmpty(registerDefinition.getId()) ? args : registerDefinition.getId());
			proxy = registerDefinition.getProxyInstance(hash);
			if (proxy == null)
				proxy = getRegisterNewInstanceByParamType(registerDefinition, plug, paramType, args);
			registerDefinition.setProxyInstance(hash, proxy);
		} else {
			proxy = getRegisterNewInstanceByParamType(registerDefinition, plug, paramType, args);
		}
		return (T) proxy;
	}

	public static void checkPreparedParameter(MethodDefinition methodDefinition,
			RegisterDefinition registerDefinition) {
		// 存在运行时解析参数
		if (methodDefinition != null && methodDefinition.getResolvers() != null) {
			ParameterResolver<ConfigValue> parameterResolver;
			for (int i = 0; i < methodDefinition.getResolvers().length; i++) {
				if ((parameterResolver = methodDefinition.getResolvers()[i]) != null) {
					// 解析参数
					methodDefinition.getArgs()[i] = parameterResolver.resove(
							(ConfigValue) methodDefinition.getArgs()[i], methodDefinition.getType()[i], i,
							registerDefinition);
					methodDefinition.getResolvers()[i] = null;
				}
			}
			methodDefinition.setResolvers(null);
		}
	}

	public static int hash(Object... objects) {
		int hash = 0;
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] == null) {
				hash += 0;
			} else if (objects[i].getClass().isArray()) {
				Object[] arrays = (Object[]) objects[i];
				hash += hash(arrays);
			} else {
				hash += objects[i].hashCode();
			}
		}

		return hash;
	}

	@SuppressWarnings("unchecked")
	public static <T> T getNewInstance(RegisterDefinition registerDefinition, Class<T> service,
			Constructor<?> constructor, Object[] args, Object origin) {
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
		HandlerSet constructorInvokeHanderSet = null;
		InstanceHandler handler = null;
		try {
			if (constructor != null) {
				constructorInvokeHanderSet = registerDefinition.getConstructorHandler(constructor);
				if (constructorInvokeHanderSet != null) {
					Iterator<HandlerSet> handlerIterator = constructorInvokeHanderSet.iterator();
					while (handlerIterator.hasNext()) {
						handler = (InstanceHandler) handlerIterator.next().getHandler();
						handler.before(registerDefinition, service, constructor, args);
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
						target = proxy = PlugsHandler.newCglibProxy(registerDefinition.getRegisterClass(),
								registerDefinition, constructor.getParameterTypes(), args);
						break;
					case BOTH:
						target = PlugsHandler.newCglibProxy(registerDefinition.getRegisterClass(), registerDefinition,
								constructor.getParameterTypes(), args);
						//使用双代理，需要先调用一次
//							// 属性处理
						initInterceptSet(registerDefinition, service, constructor, args, proxy, target,
								constructorInvokeHanderSet, handler);
						initProxyField(registerDefinition, target);
						initProxyMethod(registerDefinition, target);
						proxy = PlugsHandler.newMapperProxy(service, registerDefinition, target);
						break;
					case NONE:
						target = proxy = constructor.newInstance(args);
						break;
					default:
						target = constructor.newInstance(args);
						if (service.equals(Handler.class) || service.equals(InstanceHandler.class))
							proxy = target;
						else
							proxy = PlugsHandler.newMapperProxy(service, registerDefinition, target);
						break;
					}
				} else if (registerDefinition.getProxyModel() != ProxyModel.NONE) {
					target = proxy = PlugsHandler.newCglibProxy(registerDefinition.getRegisterClass(),
							registerDefinition, constructor.getParameterTypes(), args);
				} else {
					target = proxy = constructor.newInstance(args);
				}
			}
			if (origin != null) {
				if (constructor != null) {
					AppClassLoader.DisClone(proxy, origin);
				} else {
					target = proxy = origin;
				}
			}
			//前置处理
			proxy = beforeProcess(registerDefinition,service,proxy);
			// 属性处理
			initInterceptSet(registerDefinition, service, constructor, args, proxy, target,
					constructorInvokeHanderSet, handler);
			initProxyField(registerDefinition, proxy);
			initProxyMethod(registerDefinition, proxy);
		} catch (Throwable t) {
			String id = StringUtil.isEmpty(registerDefinition.getId()) ? registerDefinition.getRegisterClass().getName()
					: registerDefinition.getId();
//			PluginRuntimeException exception = t.getClass().equals(PluginRuntimeException.class)
//					? (PluginRuntimeException) t
//					: new PluginRuntimeException("failed to instance register for ["+id+"]",t);
			PluginRuntimeException exception = new PluginRuntimeException(
					"failed to instance register for [" + id + "]", t);
			if (constructorInvokeHanderSet != null) {
				if (handler != null)
					handler.exception(registerDefinition, service, constructor, proxy, exception, args);
				if (!exception.isInterrupt()) {
					Iterator<HandlerSet> handlerIterator = constructorInvokeHanderSet.iterator();
					InstanceHandler i;
					while (handlerIterator.hasNext() && !exception.isInterrupt()) {
						i = (InstanceHandler) handlerIterator.next().getHandler();
						if (i == handler)
							continue;
						i.exception(registerDefinition, service, constructor, proxy, exception, args);
					}
				}
			}
			if (!exception.isInterrupt())
				throw exception;
		}
		if(ArrayUtils.indexOf(registerDefinition.getServices(), CustomProxy.class) != -1) {
			proxy = ((CustomProxy<T>)proxy).getInstance();
		}
		proxy = afterProcess(registerDefinition,service,proxy);
		return (T) proxy;
	}

	/**
	 * 前置处理
	 * @param registerDefinition
	 * @param service
	 * @param proxy
	 * @return
	 */
	private static Object beforeProcess(RegisterDefinition registerDefinition, Class<?> service, Object proxy) {
		if(!InstanceProcess.class.isAssignableFrom(proxy.getClass())){
			List<InstanceBeforeProcesser> instanceProcessList = PlugsFactory.getPluginsInstanceList(InstanceBeforeProcesser.class);
			for (InstanceBeforeProcesser instanceBeforeProcesser : instanceProcessList) {
				proxy = instanceBeforeProcesser.before(registerDefinition, service, proxy);
				Assert.isNotNull(proxy,"processer return is null at "+PlugsFactory.getInstanceClass(instanceBeforeProcesser).getName()+" , please check");
			} 
		}
		return proxy;
	}
	/**
	 * 后置处理
	 * @param registerDefinition
	 * @param service
	 * @param proxy
	 * @return
	 */
	private static Object afterProcess(RegisterDefinition registerDefinition, Class<?> service, Object proxy) {
		if(!InstanceProcess.class.isAssignableFrom(proxy.getClass())){
			List<InstanceAfterProcesser> instanceProcessList = PlugsFactory.getPluginsInstanceList(InstanceAfterProcesser.class);
			for (InstanceAfterProcesser instanceProcesser : instanceProcessList) {
				proxy = instanceProcesser.after(registerDefinition, service, proxy);
				Assert.isNotNull(proxy,"processer return is null at "+PlugsFactory.getInstanceClass(instanceProcesser).getName()+" , please check");
			} 
		}
		return proxy;
	}
	private static <T> void initInterceptSet(RegisterDefinition registerDefinition, Class<T> service,
			Constructor<?> constructor, Object[] args, Object proxy, Object target,
			HandlerSet constructorInvokeHanderSet, InstanceHandler handler) {
		if (registerDefinition.getFieldInterceptMapping() != null) {
			FieldHandler fieldHandler;
			Map<Field, HandlerSet> fieldInterceptMapping = registerDefinition.getFieldInterceptMapping();
			Iterator<Entry<Field, HandlerSet>> fieldInterceptIterator = fieldInterceptMapping.entrySet()
					.iterator();
			while (fieldInterceptIterator.hasNext()) {
				Entry<Field, HandlerSet> entry = fieldInterceptIterator.next();
				Field field = entry.getKey();
				HandlerSet filedHandlerSet = entry.getValue();
				Iterator<HandlerSet> fieldIterator = filedHandlerSet.iterator();
				while (fieldIterator.hasNext()) {
					HandlerSet fieldHandlerSet = fieldIterator.next();
					fieldHandler = fieldHandlerSet.getHandler();
					fieldHandler.preparedField(registerDefinition, proxy, target, fieldHandlerSet, field);
				}
			}
		}
		// 调用代理完成
		if (constructorInvokeHanderSet != null) {
			Iterator<HandlerSet> handlerIterator = constructorInvokeHanderSet.iterator();
			while (handlerIterator.hasNext()) {
				handler = (InstanceHandler) handlerIterator.next().getHandler();
				handler.after(registerDefinition, service, constructor, target, args);
			}
		}
	}

	/**
	 * 代理实例化后调用方法
	 * 
	 * @param registerDefinition 注册定义
	 * @param proxy 代理对象
	 */
	public static void initProxyMethod(RegisterDefinition registerDefinition, Object proxy) {
		if (registerDefinition.getAfterInstanceExecuteMethod() != null) {
			for (MethodDefinition methodDefinition : registerDefinition.getAfterInstanceExecuteMethod()) {
				try {
					checkPreparedParameter(methodDefinition, registerDefinition);
					Method method = ClassHelper.getClassHelper(proxy.getClass())
							.getDeclaredMethod(methodDefinition.getMethod().getName(), methodDefinition.getMethod().getParameterTypes());
					if(method == null && proxy.getClass().getName().indexOf("com.sun.proxy.$Proxy") != -1) {
						return;
					}
					method.setAccessible(true);
					method.invoke(proxy, methodDefinition.getArgs());
					method.setAccessible(false);
				} catch (Throwable e) {
					throw new PluginRuntimeException("failed to invoke @PostConstructor method " + methodDefinition.getMethod(), e);
				}
			}
		}

	}

	/**
	 * 代理实例化后调用方法
	 * 
	 * @param registerDefinition 注册定义
	 * @param proxy 代理类
	 * @throws IllegalAccessException ex
	 * @throws IllegalArgumentException ex
	 * @throws InvocationTargetException ex
	 * @throws NoSuchMethodException ex
	 * @throws SecurityException ex
	 */
	@SuppressWarnings("unchecked")
	public static void initProxyField(RegisterDefinition registerDefinition, Object proxy)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
			SecurityException {
		if (registerDefinition.getAfterInstanceInitField() != null) {
			for (FieldDefinition fieldDefinition : registerDefinition.getAfterInstanceInitField()) {
				if (fieldDefinition.getResolver() != null)
					fieldDefinition.setValue(((DelayParameterResolver<ConfigValue>) fieldDefinition.getResolver())
							.resove((ConfigValue) fieldDefinition.getValue(), fieldDefinition.getType(), 0,
									registerDefinition));
				try {
					Field field = ClassHelper.getClassHelper(proxy.getClass())
							.getDeclaredField(fieldDefinition.getField().getName());
					if(field == null && proxy.getClass().getName().indexOf("com.sun.proxy.$Proxy") != -1) {
						return;
					}
					new AppClassLoader(proxy).set(field, fieldDefinition.getValue());
				} catch (Throwable t) {
					String type = fieldDefinition.getValue() == null ? "null"
							: fieldDefinition.getValue().getClass().getName();
					throw new PluginRuntimeException(
							"faied to init field " + registerDefinition.getRegisterClass().getName() + "."
									+ fieldDefinition.getField().getName() + " with [" + fieldDefinition.getValue()
									+ "] type [" + type + "]",
							t);
				}

			}
		}
	}
	/**
	 * 销毁实例
	 * @param instance 要销毁的实例
	 */
	public static void destoryInstance(Object instance) {
		try {
			PlugsHandler handler = PlugsFactory.getPluginsHandler(instance);
			RegisterDefinition registerDefinition = handler.getRegisterDefinition();
			MethodDefinition destoryMethod;
			if((destoryMethod = registerDefinition.getDestoryMethod()) != null) {
				destoryMethod.getMethod().invoke(instance,destoryMethod.getArgs());
			}
			registerDefinition.getProxyContainer().entrySet().removeIf(entry->{
				return entry.getValue().equals(instance);
			});
		} catch (Throwable e) {
			throw new PluginRuntimeException("failed to invoke desotry method for "+instance, e);
		}
	}
	/**
	 * 销毁注册描述
	 * @param registerDefinition 组件定义
	 */
	public static void destory(RegisterDefinition registerDefinition) {
		if(registerDefinition.isSignlton() && 
				registerDefinition.getProxyContainer() != null && 
				registerDefinition.getDestoryMethod() != null) {
			registerDefinition.getProxyContainer().values().forEach(instance->{
				try {
					MethodDefinition destoryMethod;
					if((destoryMethod = registerDefinition.getDestoryMethod()) != null) {
						destoryMethod.getMethod().invoke(instance,destoryMethod.getArgs());
					}
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					String name = StringUtil.isEmpty(registerDefinition.getId())
							? registerDefinition.getRegisterClass().getName()
							: registerDefinition.getId();
					throw new PluginRuntimeException("failed to invoke instance " + name + " @PreDestory method " + registerDefinition.getDestoryMethod(), e);
				}
			});
		}
	}
}