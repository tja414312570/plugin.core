package com.yanan.framework.plugin.autowired.plugin;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.concurrent.Executor;

import com.yanan.framework.plugin.PlugsFactory;
import com.yanan.framework.plugin.annotations.Register;
import com.yanan.framework.plugin.annotations.Service;
import com.yanan.framework.plugin.annotations.Support;
import com.yanan.framework.plugin.definition.RegisterDefinition;
import com.yanan.framework.plugin.exception.PluginRuntimeException;
import com.yanan.framework.plugin.handler.FieldHandler;
import com.yanan.framework.plugin.handler.HandlerSet;
import com.yanan.framework.plugin.handler.InstanceHandler;
import com.yanan.framework.plugin.handler.InvokeHandler;
import com.yanan.framework.plugin.handler.MethodHandler;
import com.yanan.utils.reflect.ReflectUtils;
import com.yanan.utils.string.StringUtil;

/**
 * 组件注入
 * 
 * @author yanan
 *
 */
@Support(Service.class)
@Register(attribute = "*", description = "Service服务的注入")
public class PluginWiredHandler implements InvokeHandler, FieldHandler, InstanceHandler {
	public void finalExecute(Executor executor, Executor finalExecutor) {
		try {
			executor.execute(null);
		} finally {
			finalExecutor.execute(null);
		}
	}

	@Override
	public void before(MethodHandler methodHandler) {
		Service service;
		try {
			WiredStackContext.push(WiredStackContext.METHOD,methodHandler);
			Parameter[] parameters = methodHandler.getMethod().getParameters();
			Object[] arguments = methodHandler.getParameters();
			for (int i = 0; i < parameters.length; i++) {
				Parameter parameter = parameters[i];
				service = parameter.getAnnotation(Service.class);
				if (service != null) {
					if (arguments[i] == null) {
						Class<?> type = parameters[i].getType();
						if (!service.id().trim().equals("")) {
							arguments[i] = PlugsFactory.getPluginsInstance(service.id());
							if (arguments[i] == null)
								throw new PluginRuntimeException("can't found bean id for \"" + service.id()
										+ "\" for parameter \"" + parameter.getName() + "\" for method \""
										+ methodHandler.getMethod().getName() + "\" for "
										+ methodHandler.getPlugsProxy().getRegisterDefinition().getRegisterClass());
						} else if (type.isArray()) {
							List<?> obj = PlugsFactory.getPluginsInstanceListByAttribute(type, service.attribute());
							arguments[i] = obj;
						} else if (type.getClass().equals(List.class)) {
							List<?> obj = PlugsFactory.getPluginsInstanceListByAttribute(type, service.attribute());
							arguments[i] = obj;
						} else {
							Object obj = null;
							try {
								obj = PlugsFactory.getPluginsInstanceByAttributeStrict(type, service.attribute());
							} catch (Throwable t) {
								obj = PlugsFactory.getPluginsInstance(type);
							}
							if (obj == null)
								throw new PluginRuntimeException("can't found register or bean for parameter \""
										+ parameter.getName() + "\" type \"" + parameter.getType() + "\" for method \""
										+ methodHandler.getMethod().getName() + "\" for "
										+ methodHandler.getPlugsProxy().getRegisterDefinition().getRegisterClass());
							arguments[i] = obj;
						}

					}
				}
			}
		} finally {
			WiredStackContext.pop();
		}

	}

	@Override
	public void after(MethodHandler methodHandler) {

	}

	@Override
	public void error(MethodHandler methodHandler, Throwable e) {
	}

	@Override
	public void before(RegisterDefinition registerDefinition, Class<?> plugClass, Constructor<?> constructor,
			Object... arguments) {
		Service service;
		try {
			WiredStackContext.push(WiredStackContext.CONSTRUCT,registerDefinition,plugClass,constructor,arguments);
			// 获取构造方法所有的参数
			Parameter[] parameters = constructor.getParameters();
			for (int i = 0; i < parameters.length; i++) {
				Parameter parameter = parameters[i];
				// 获取参数的Service注解
				service = parameter.getAnnotation(Service.class);
				if (service != null) {
					// 如果参数不为Null时，不注入此参数
					if (arguments[i] == null) {
						// 获取参数的类型
						Class<?> type = parameters[i].getType();
						// 如果Service的注解具有id属性，说明为Bean注入
						if (!service.id().trim().equals("")) {
							// 将该参数为设置为获取到的Bean
							arguments[i] = PlugsFactory.getPluginsInstance(service.id());
							if (arguments[i] == null)
								throw new PluginRuntimeException("can't found bean id for \"" + service.id()
										+ "\" type \"" + parameter.getType() + "\" for parameter \""
										+ parameter.getName() + "\" for construct \"" + constructor + "\" for "
										+ registerDefinition.getRegisterClass());
							// 如果获取到的参数类型为数组
						} else if (type.isArray()) {
							// 获得数组的真实类型
							Class<?> typeClass = ReflectUtils.getListGenericType(parameters[i]);
							List<?> obj = PlugsFactory.getPluginsInstanceListByAttribute(typeClass,
									service.attribute());
							Object[] arr = (Object[]) Array.newInstance(typeClass, obj.size());
							arguments[i] = obj.toArray(arr);
						} else if (type.getClass().equals(List.class)) {
							// 获取数组参数的类型
							Class<?> typeClass = ReflectUtils.getListGenericType(parameters[i]);
							// 获取服务返回的所有实现的实例
							List<?> obj = PlugsFactory.getPluginsInstanceListByAttribute(typeClass,
									service.attribute());
							arguments[i] = obj;
						} else {
							// 如果以上都没有匹配到，则首先从服务里获取，没有获取到时重Bean容器里获取。
							Object obj = null;
							try {
								obj = PlugsFactory.getPluginsInstanceByAttributeStrict(type, service.attribute());
							} catch (Throwable t) {
								obj = PlugsFactory.getPluginsInstance(type);
							}
							if (obj == null)
								throw new PluginRuntimeException(
										"can't found register or bean for parameter \"" + parameter.getName()
												+ "\" type \"" + parameter.getType() + "\" for construct \""
												+ constructor + "\" for " + registerDefinition.getRegisterClass());
							arguments[i] = obj;
						}
					}
				}
			}
		} finally {
			WiredStackContext.pop();
		}
	}

	@Override
	public void after(RegisterDefinition registerDefinition, Class<?> plugClass, Constructor<?> constructor,
			Object proxyObject, Object... args) {
		// TODO Auto-generated method stub

	}

	@Override
	public void exception(RegisterDefinition registerDefinition, Class<?> plug, Constructor<?> constructor,
			Object proxyObject, PluginRuntimeException throwable, Object... args) {
		// TODO Auto-generated method stub

	}

	@Override
	public void preparedField(RegisterDefinition registerDefinition, Object proxy, Object target, HandlerSet handlerSet,
			Field field) {
		try {
			WiredStackContext.push(WiredStackContext.FIELD,registerDefinition,proxy,target,handlerSet,field);
			Service service = handlerSet.getAnnotation(Service.class);
			try {
				field.setAccessible(true);
				Class<?> type = field.getType();
				if (!service.id().trim().equals("")) {
					Object object = PlugsFactory.getPluginsInstance(service.id());
					if (object == null)
						throw new PluginRuntimeException(
								"can't found bean id for \"" + service.id() + "\" for field \"" + field.getName()
										+ "\" for " + registerDefinition.getRegisterClass());
					field.set(target, object);
				} else if (type.isArray()) {
					List<?> obj = PlugsFactory.getPluginsInstanceListByAttribute(type, service.attribute());
					field.set(target, obj.toArray());
				} else if (type.getClass().equals(List.class)) {
					List<?> obj = PlugsFactory.getPluginsInstanceListByAttribute(type, service.attribute());
					field.set(target, obj);
				} else {
					Object obj = null;
					try {
						// from field name
						obj = PlugsFactory.getPluginsInstance(field.getName());
					} catch (Throwable t) {
					}
					if (obj == null) {
						try {
							// from type
							obj = PlugsFactory.getPluginsInstance(type);
						} catch (Throwable e) {
							e.printStackTrace();
						}
					}
					if (obj == null && StringUtil.isNotEmpty(service.attribute())) {
						try {
							// from attris
							obj = PlugsFactory.getPluginsInstanceByAttributeStrict(type, service.attribute());
						} catch (Throwable e) {
						}
					}
					if (obj == null)
						throw new PluginRuntimeException(
								"can't found register or bean for field \"" + field.getName() + "\" type \""
										+ field.getType() + "\" for " + registerDefinition.getRegisterClass());
					field.set(target, obj);
				}
				field.setAccessible(false);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new RuntimeException("failed to autowired service ! at class : "
						+ registerDefinition.getRegisterClass().getName() + " at field : " + field.getName(), e);
			}
		} finally {
			WiredStackContext.pop();
		}
	}
}