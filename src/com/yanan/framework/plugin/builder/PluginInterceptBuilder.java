package com.yanan.framework.plugin.builder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.yanan.framework.plugin.Plugin;
import com.yanan.framework.plugin.PlugsFactory;
import com.yanan.framework.plugin.annotations.Support;
import com.yanan.framework.plugin.definition.RegisterDefinition;
import com.yanan.framework.plugin.handler.FieldHandler;
import com.yanan.framework.plugin.handler.InstanceHandler;
import com.yanan.framework.plugin.handler.InvokeHandler;
import com.yanan.framework.plugin.handler.HandlerSet;
import com.yanan.utils.ArrayUtils;
import com.yanan.utils.asserts.Assert;

/**
 * 组件拦截器构建
 * 
 * @author yanan
 *
 */
public class PluginInterceptBuilder {
	public static void builderRegisterIntercept(RegisterDefinition registerDefinition) {
		Class<?>[] plugins = registerDefinition.getServices();
		if (plugins != null)
			for (Class<?> interfacer : plugins) {
				initMethodHandlerMapping(interfacer, registerDefinition);
			}
		if (registerDefinition.getRegisterClass() != null) {
			initMethodHandlerMapping(registerDefinition.getRegisterClass(), registerDefinition);
			initConstructorHandlerMapping(registerDefinition.getRegisterClass(), registerDefinition);
			initFieldHandlerMapping(registerDefinition);
		}
	}

	public static void initFieldHandlerMapping(RegisterDefinition registerDefinition) {
		Plugin cplug = PlugsFactory.getPlugin(FieldHandler.class);
		if (cplug == null)
			return;
		Field[] fields = registerDefinition.getRegisterClass().getDeclaredFields();
		for (Field field : fields) {
			List<RegisterDefinition> registerList = cplug
					.getRegisterDefinitionListByAttribute(registerDefinition.getRegisterClass().getName() + "." + field.getName());
			Map<Class<?>, Object> annos = null;
			for (int i = 0; i < registerList.size(); i++) {
				FieldHandler handler = null;
				RegisterDefinition register = registerList.get(i);
				Class<?> registerClass = register.getRegisterClass();
				// 获取支持的注解
				Support support = registerClass.getAnnotation(Support.class);
				if (support == null) {
					handler = PluginInstanceFactory.getRegisterInstance(register,FieldHandler.class);
				} else {
					Class<? extends Annotation>[] supportClass = deduceSupport(support);
					for (Class<? extends Annotation> supportClzz : supportClass) {
						// 对比注解
						Annotation anno;
						// 从Field获取注解
						anno = field.getAnnotation(supportClzz);
						if (anno != null) {
							annos = getHandlerAnno(annos, anno);
							handler = PluginInstanceFactory.getRegisterInstance(register,FieldHandler.class);
						}
						if (handler == null) {
							anno = registerClass.getAnnotation(supportClzz);
							if (anno != null) {
								annos = getHandlerAnno(annos, anno);
								handler = PluginInstanceFactory.getRegisterInstance(register,FieldHandler.class);
							}
						}
					}
				}
				if (handler != null) {
					HandlerSet invokeHandlerSet = new HandlerSet(handler);
					invokeHandlerSet.setAnnotations(annos);
					registerDefinition.addFieldHandler(field,invokeHandlerSet);
				}
			}
			
		}
	}
	/**
	 * 推断支持注解类
	 * @param support 注解
	 * @return 支持的注解的数组
	 */
	@SuppressWarnings("unchecked")
	public static Class<? extends Annotation>[] deduceSupport(Support support) {
		Class<? extends Annotation>[] supportClassArray = new Class[0];
		if(support.name().length>0) {
			for(String supportStr : support.name()) {
				try {
					Class<? extends Annotation> suppportClass = (Class<? extends Annotation>) Class.forName(supportStr);
					supportClassArray = ArrayUtils.add(supportClassArray, suppportClass);
				}catch(ClassNotFoundException e) {
//					e.printStackTrace();
				}
			}
		}
		if(support.value().length>0) {
			if(supportClassArray != null && supportClassArray.length>0) {
				Class<? extends Annotation>[] temp = supportClassArray;
				supportClassArray = new Class[temp.length+support.value().length];
				System.arraycopy(temp, 0, supportClassArray, 0, temp.length);
				System.arraycopy(support.value(), temp.length-1, supportClassArray, temp.length,support.value().length);
			}else {
				supportClassArray = support.value();
			}
		}
		return supportClassArray;
	}

	public static void initConstructorHandlerMapping(Class<?> serviceClass, RegisterDefinition registerDefinition) {
		Constructor<?>[] constructors = registerDefinition.getRegisterClass().getDeclaredConstructors();
		// 获取所有的构造器的拦截器
		Plugin cplug = PlugsFactory.getPlugin(InstanceHandler.class);
		if (cplug == null)
			return;
		// 找出满足当前类的拦截器
		List<RegisterDefinition> registerList = cplug.getRegisterDefinitionListByAttribute(serviceClass.getName());
		for (Constructor<?> constructor : constructors) {
			Map<Class<?>, Object> annos = null;
			for (int i = 0; i < registerList.size(); i++) {
				InstanceHandler handler = null;
				RegisterDefinition register = registerList.get(i);
				Class<?> registerClass = register.getRegisterClass();
				// 获取支持的注解
				Support support = registerClass.getAnnotation(Support.class);
				if (support == null) {
					handler = PluginInstanceFactory.getRegisterInstance(register,InstanceHandler.class);
				} else {
					Class<? extends Annotation>[] supportClass = deduceSupport(support);
					for (Class<? extends Annotation> supportClzz : supportClass) {
						// 对比注解
						Annotation anno;
						// 从构造器获取注解
						anno = constructor.getAnnotation(supportClzz);
						if (anno != null) {
							annos = getHandlerAnno(annos, anno);
							handler = PluginInstanceFactory.getRegisterInstance(register,InstanceHandler.class);
						}
						if (handler == null) {
							Parameter[] parameters = constructor.getParameters();
							for (Parameter parameter : parameters) {
								if ((anno = parameter.getAnnotation(supportClzz)) != null) {
									annos = getHandlerAnno(annos, anno);
									handler = PluginInstanceFactory.getRegisterInstance(register,InstanceHandler.class);
								}
							}
						}
						if (handler == null) {
							anno = registerClass.getAnnotation(supportClzz);
							if (anno != null) {
								annos = getHandlerAnno(annos, anno);
								handler = PluginInstanceFactory.getRegisterInstance(register,InstanceHandler.class);
							}
						}
					}
				}
				if (handler != null) {
					HandlerSet invokeHandlerSet = new HandlerSet(handler);
					invokeHandlerSet.setAnnotations(annos);
					registerDefinition.addConstructorHandler(constructor, invokeHandlerSet);
				}
			}
		}
	}

	public static void initMethodHandlerMapping(Class<?> serviceClass, RegisterDefinition registerDefinition) {
		Assert.isNull(serviceClass, "class is null");
		// 获取所有的方法
		Method[] methods = serviceClass.getDeclaredMethods();
		for (Method method : methods) {
			// 从组件工厂获取所有调用拦截器
			Plugin plugin = PlugsFactory.getPlugin(InvokeHandler.class);
			if (plugin == null)
				return;
			// 获取所有的具有此属性的拦截器的组件
			List<RegisterDefinition> registerList = plugin
					.getRegisterDefinitionListByAttribute(serviceClass.getName() + "." + method.getName());
			for (RegisterDefinition invokeRegisterDefinition : registerList) {
				if (invokeRegisterDefinition.getRegisterClass().equals(registerDefinition.getRegisterClass()))
					continue;
				Class<?> registerClass = invokeRegisterDefinition.getRegisterClass();
				Support support = registerClass.getAnnotation(Support.class);
				InvokeHandler handler = null;
				Map<Class<?>, Object> annos = null;
				if (support == null) {
					handler = PluginInstanceFactory.getRegisterInstance(invokeRegisterDefinition,InvokeHandler.class);
				} else {
					Class<? extends Annotation>[] supportClass = deduceSupport(support);
					for (Class<? extends Annotation> supportClzz : supportClass) {
						// 依次从代理类或接口类的方法和类声明中获取支持的注解
						Annotation anno;
						// 获取代理类的方法
						Method proMethod = registerDefinition.getLoader().getMethod(method.getName(),
								method.getParameterTypes());
						if (proMethod != null) {
							if ((anno = proMethod.getAnnotation(supportClzz)) != null) {
								annos = getHandlerAnno(annos, anno);
								handler = PluginInstanceFactory.getRegisterInstance(invokeRegisterDefinition,InvokeHandler.class);
							} else {
								Parameter[] parameters = proMethod.getParameters();
								for (Parameter parameter : parameters) {
									if ((anno = parameter.getAnnotation(supportClzz)) != null) {
										annos = getHandlerAnno(annos, anno);
										handler = PluginInstanceFactory.getRegisterInstance(invokeRegisterDefinition,InvokeHandler.class);
									}
								}
							}
						}
						if (handler == null) {
							if ((anno = method.getAnnotation(supportClzz)) != null) {
								annos = getHandlerAnno(annos, anno);
								handler = PluginInstanceFactory.getRegisterInstance(invokeRegisterDefinition,InvokeHandler.class);
							} else {
								Parameter[] parameters = method.getParameters();
								for (Parameter parameter : parameters) {
									if ((anno = parameter.getAnnotation(supportClzz)) != null) {
										annos = getHandlerAnno(annos, anno);
										handler = PluginInstanceFactory.getRegisterInstance(invokeRegisterDefinition,InvokeHandler.class);
									}
								}
							}
						}
						if (handler == null && (anno = registerClass.getAnnotation(supportClzz)) != null) {
							annos = getHandlerAnno(annos, anno);
							handler = PluginInstanceFactory.getRegisterInstance(invokeRegisterDefinition,InvokeHandler.class);
						}
						if (handler == null && (anno = serviceClass.getAnnotation(supportClzz)) != null) {
							annos = getHandlerAnno(annos, anno);
							handler = PluginInstanceFactory.getRegisterInstance(invokeRegisterDefinition,InvokeHandler.class);

						}
					}
				}
				if (handler != null) {
					HandlerSet invokeHandlerSet = new HandlerSet(handler);
					invokeHandlerSet.setAnnotations(annos);
					registerDefinition.addMethodHandler(method, invokeHandlerSet);
				}
			}
		}
	}

	public static Map<Class<?>, Object> getHandlerAnno(Map<Class<?>, Object> annos, Annotation anno) {
		if (annos == null)
			annos = new HashMap<Class<?>, Object>();
		annos.put(anno.annotationType(), anno);
		return annos;
	}
}