package com.yanan.framework.plugin.builder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import com.typesafe.config.impl.SimpleConfigObject;
import com.yanan.framework.plugin.ExtReflectUtils;
import com.yanan.framework.plugin.Plugin;
import com.yanan.framework.plugin.PlugsFactory;
import com.yanan.framework.plugin.ProxyModel;
import com.yanan.framework.plugin.annotations.Register;
import com.yanan.framework.plugin.annotations.Service;
import com.yanan.framework.plugin.autowired.plugin.CustomProxy;
import com.yanan.framework.plugin.builder.resolver.DelayParameterResolver;
import com.yanan.framework.plugin.builder.resolver.ParameterResolver;
import com.yanan.framework.plugin.definition.ConstructorDefinition;
import com.yanan.framework.plugin.definition.FieldDefinition;
import com.yanan.framework.plugin.definition.MethodDefinition;
import com.yanan.framework.plugin.definition.PluginDefinition;
import com.yanan.framework.plugin.definition.RegisterDefinition;
import com.yanan.framework.plugin.exception.PluginInitException;
import com.yanan.framework.plugin.handler.PlugsHandler;
import com.yanan.utils.ArrayUtils;
import com.yanan.utils.CollectionUtils;
import com.yanan.utils.reflect.AppClassLoader;
import com.yanan.utils.reflect.ReflectUtils;
import com.yanan.utils.reflect.TypeToken;
import com.yanan.utils.string.StringUtil;

/**
 * 组件定义构造工厂
 * @author yanan
 */
public class PluginDefinitionBuilderFactory {
	public static final String CONFIG_ARGS = "args";
	public static final String CONFIG_TYPES = "types";
	public static final String CONFIG_ID = "id";
	public static final String CONFIG_PRIORITY = "priority";
	public static final String CONFIG_ATTRIBUTE = "attribute";
	public static final String CONFIG_REF = "ref";
	public static final String CONFIG_CLASS = "class";
	public static final String CONFIG_MODEL = "model";
	public static final String CONFIG_SIGNITON = "signlton";
	public static final String CONFIG_DESCIPTION = "description";
	public static final String CONFIG_FIELD = "field";
	public static final String CONFIG_SERVICE = "service";
	public static final String CONFIG_INIT = "init";
	public static final String CONFIG_METHOD = "method";
	public static final String CONFIG_DESTORY = "destory";
	/**
	 * 构建一个组件或则注册器
	 * @param pluginClass 组件类
	 * @return 组件定义或则注册定义
	 */
	public static Object builderPluginDefinitionAuto(Class<?> pluginClass) {
		Service service = pluginClass.getAnnotation(Service.class);
		if (service != null || pluginClass.isInterface()) 
			return builderPluginDefinition(pluginClass);
		return builderRegisterDefinition(pluginClass);
	}
	
	/**
	 * 强制将类作为Plugin的定义
	 * @param pluginClass
	 * @return 组件定义
	 */
	public static Plugin builderPluginDefinition(Class<?> pluginClass) {
		Plugin plugin = PlugsFactory.getPlugin(pluginClass);
		if(plugin == null) {
			Service service = pluginClass.getAnnotation(Service.class);
			PluginDefinition plugsDescrption = new PluginDefinition(service, pluginClass);
			plugin = new Plugin(plugsDescrption);
		}
		return plugin;
	}
	/**
	 * 构造注册器定义
	 * @param registerClass 注册类
	 * @return 注册器定义
	 */
	public static RegisterDefinition builderRegisterDefinition(Class<?> registerClass) {
//		if(registerClass.isInterface())
//			throw new PluginInitException("the required build class is an interface ["+registerClass.getName()+"]");
		Register register = registerClass.getAnnotation(Register.class);
		if(register == null)
			return buildRegisterDefinitionByDefault(registerClass);
		return buildRegisterDefinitionByAnnotation(register, registerClass);
	}
	/**
	 * 使用默认的方式构造注册定义
	 * @param registerClass 注册定义类
	 * @return 注册定义
	 */
	public static RegisterDefinition buildRegisterDefinitionByDefault(Class<?> registerClass) {
		RegisterDefinition register = new RegisterDefinition();
		register.setRegisterClass(registerClass);
		// 读取属性
		register.setPriority( Integer.MAX_VALUE);
		register.setSignlton(true);
		register.setDescription("default register description :" + registerClass.getName());
		// 获取实现类
		register.setLoader(new AppClassLoader(registerClass, false));
		// 获取实现类所在的接口
		register.setServices(registerClass.getInterfaces());
		checkAfterBuilder(register);	
		return register;
	}
	/**
	 * 使用注解的方式构造注册定义
	 * @param register 注册注解
	 * @param registerClass 注册类
	 * @return 注册定义
	 */
	public static RegisterDefinition buildRegisterDefinitionByAnnotation(Register register, Class<?> registerClass) {
		RegisterDefinition registerDefinition = new RegisterDefinition();
		AppClassLoader loader = new AppClassLoader(register.declare().equals(Object.class) ? 
				registerClass : register.declare(), false);
		registerDefinition.setLoader(loader);
		registerDefinition.setRegisterClass(loader.getLoadedClass());
		registerDefinition.setRegister(register);
//		registerDefinition.setServices(register.register().length == 0 ? 
//				loader.getLoadedClass().getInterfaces() : register.register());
		registerDefinition.setServices(buildService(register.register(),loader.getLoadedClass().getInterfaces()));
		registerDefinition.setPriority(register.priority());
		registerDefinition.setSignlton(register.signlTon());
		registerDefinition.setAttribute(register.attribute());
		registerDefinition.setDescription(register.description());
		registerDefinition.setProxyModel(register.model());
		registerDefinition.setId(register.id());
		String[] afterInstance = register.afterInstance();
		Method afterInstaceMethod;
		if(afterInstance.length>0) {
			for (int i = 0; i < afterInstance.length; i++) {
				try {
					afterInstaceMethod = loader.getDeclaredMethod(afterInstance[i]);
					checkAfterInstanitiationMethod(afterInstance[i],afterInstaceMethod,registerDefinition.getRegisterClass());
					registerDefinition.addAfterInstanceExecuteMethod(new MethodDefinition(afterInstaceMethod, null, null,null,null));
				} catch (NoSuchMethodException | SecurityException e) {
					throw new PluginInitException("failed to get init method \"" + afterInstance[i] + "\"", e);
				}
			}
		}
		if(StringUtil.isNotEmpty(register.destory())) {
			try {
				Method method  = loader.getDeclaredMethod(register.destory());
				checkAfterInstanitiationMethod(register.destory(),method,registerDefinition.getRegisterClass());
				registerDefinition.setDestoryMethod(new MethodDefinition(method, null, null,null,null));
			} catch (NoSuchMethodException | SecurityException e) {
				throw new PluginInitException("failed to get init method \"" + register.destory() + "\"", e);
			}
		}
		Method[] methods = registerDefinition.getRegisterClass().getMethods();
		for(Method method : methods) {
			PostConstruct afterInstanceAnno = method.getAnnotation(PostConstruct.class);
			if(afterInstanceAnno != null) {
//				checkAfterInstanitiationMethod(method.getName(),method,registerDefinition.getRegisterClass());
				registerDefinition.addAfterInstanceExecuteMethod(new MethodDefinition(method, method.getParameterTypes(), new Object[method.getParameterCount()],null,null));
			}
			PreDestroy destory = method.getAnnotation(PreDestroy.class);
			if(destory != null) {
				registerDefinition.setDestoryMethod(new MethodDefinition(method, method.getParameterTypes(), new Object[method.getParameterCount()],null,null));
			}
		}
		checkAfterBuilder(registerDefinition);	
		return registerDefinition;
		//查找实例化后的方法
//		checkPlugs(registerDefinition.getPlugs());
	}
	private static Class<?>[] buildService(Class<?>[] register, Class<?>[] interfaces) {
		if(register == null && interfaces == null)
			return null;
		if(register == null || register.length ==0)
			return interfaces;
		if(interfaces == null || interfaces.length == 0)
			return register;
		int len = register.length + interfaces.length;
		Class<?>[] clzzs = new Class<?>[len];
		System.arraycopy(register, 0, clzzs, 0, register.length);
		System.arraycopy(interfaces, 0, clzzs, register.length, interfaces.length);
		return  clzzs;
	}


	/**
	 * 通过配置的方式构造注册定义
	 * @param config 配置
	 * @return 注册定义
	 */
	public static RegisterDefinition buildRegisterDefinitionByConfig(Config config){
		RegisterDefinition registerDefinition = null;
		try {
			config.allowKeyNull(true);
			String className = config.getString(CONFIG_CLASS);
			String ref = config.getString(CONFIG_REF);
			if (className == null && ref == null)
				throw new RuntimeException("can't fond class property and no reference any at \""
						+ config.origin().url() + "\" at line : " + config.origin().lineNumber());
			if(ref != null) {
				//推断类型
				RegisterDefinition	refDefinition = PlugsFactory.getInstance().getRegisterDefinition(ref);
				//推断类型
				Class<?> refRegisterClass;
				if(refDefinition.getInstanceMethod() != null) {
					refRegisterClass = PlugsFactory.getPluginsInstance(refDefinition.getId()).getClass();
				}else {
					refRegisterClass =refDefinition.getRegisterClass();
				}
				registerDefinition = builderRegisterDefinition(refRegisterClass);
				registerDefinition.setRegisterClass(refRegisterClass);
				registerDefinition.setId(refDefinition.getId());
			}
			if(className != null) {
				AppClassLoader loader = new AppClassLoader(className, false);
				registerDefinition = builderRegisterDefinition(loader.getLoadedClass());
			}
			//获取实例化方法
			String id = config.getString(CONFIG_ID);
			String instanitionMethodStr = config.getString(CONFIG_METHOD);
			if(!StringUtil.isBlank(instanitionMethodStr)) {
				MethodDefinition instanitionMethod = deduceInstanitionMethod(config,registerDefinition);
				if(!instanitionMethod.getMethod().getReturnType().equals(registerDefinition.getRegisterClass())) {
					registerDefinition =  builderRegisterDefinition(instanitionMethod.getMethod().getReturnType());
				}
				registerDefinition.setInstanceMethod(instanitionMethod);
			}else if(!StringUtil.isBlank(id)){
				//如果id不为空
				ConstructorDefinition constructorDefinition = deduceInstanitionConstructor(config, registerDefinition);
				registerDefinition.setInstanceConstructor(constructorDefinition);
			}
			if(StringUtil.isNotEmpty(id)) {
				registerDefinition.setId(id);
			}
//			else if(StringUtil.isNotEmpty(ref) && StringUtil.isNotEmpty(registerDefinition.getId())){
//				registerDefinition.setId(registerDefinition.getId()+ref+"_"+registerDefinition.hashCode());
//			}
				
			if(StringUtil.isNotBlank(ref))
				registerDefinition.setReferenceId(ref);
			registerDefinition.setConfig(config);
			registerDefinition.setPriority(config.getInt(CONFIG_PRIORITY,registerDefinition.getPriority()));
			//推断是否单例模式
			deduceSigniton(config, registerDefinition);
			registerDefinition.setSignlton(config.getBoolean(CONFIG_SIGNITON,registerDefinition.isSignlton()));
			String[] atts = config.hasPath(CONFIG_ATTRIBUTE)?config.getString(CONFIG_ATTRIBUTE).split(","):registerDefinition.getAttribute();
			registerDefinition.setAttribute(atts);
			registerDefinition.setDescription(config.getString(CONFIG_DESCIPTION,registerDefinition.getDescription()));
			String model = config.getString(CONFIG_MODEL, registerDefinition.getProxyModel().toString());
			registerDefinition.setProxyModel(ProxyModel.getProxyModel(model));
			//属性的赋值
			deduceInstanitionField(config, registerDefinition);
			//推断实现类
			deduceServices(config, registerDefinition);
			//获取实例后执行的方法
			deduceAfterInstanceExecuteMethod(config,registerDefinition);
			//推断destory方法
			deduceDestoryMethod(config,registerDefinition);
			checkAfterBuilder(registerDefinition);	
//				checkPlugs(register.get);
//			PlugsFactory.getInstance().addRegisterHandlerQueue(this);
			return registerDefinition;
		} catch (Exception e) {
				throw new PluginInitException("plugin exception init at \"" + config + "\" at line "
						+ config.origin().lineNumber(), e);
		}
	}

	public static void deduceSigniton(Config config, RegisterDefinition registerDefinition) {
		if(config.hasPath(CONFIG_SIGNITON)) {
			registerDefinition.setSignlton(config.getBoolean(CONFIG_SIGNITON));
		}else {
			if(!StringUtil.isEmpty(registerDefinition.getId()))
				registerDefinition.setSignlton(true);
		}
	}

	public static void deduceServices(Config config, RegisterDefinition registerDefinition) {
		String services =  config.getString(CONFIG_SERVICE);
		if(services != null) {
			registerDefinition.setServices(getPlugs(registerDefinition.getRegisterClass(), services));
		}else {
			Class<?>[] plugs = registerDefinition.getRegisterClass().getInterfaces();
			if(plugs == null || plugs.length == 0) {
				plugs = new Class<?>[]{registerDefinition.getRegisterClass()};
			}
			registerDefinition.setServices(plugs);
		}
	}
	/**
	 * 构造完成之后的一些检查 
	 * @param registerDefinition 组件定义
	 */
	private static void checkAfterBuilder(RegisterDefinition registerDefinition) {
//		if(CollectionUtils.isNotEmpty(registerDefinition.getAfterInstanceExecuteMethod())
//				|| CollectionUtils.isNotEmpty(registerDefinition.getAfterInstanceInitField())
//				|| ArrayUtils.indexOf(registerDefinition.getServices(), CustomProxy.class) != -1) {
//			if(registerDefinition.getProxyModel() != ProxyModel.NONE)
//			registerDefinition.setProxyModel(ProxyModel.CGLIB);
//		}
		if(CollectionUtils.isNotEmpty(registerDefinition.getAfterInstanceExecuteMethod())
				|| CollectionUtils.isNotEmpty(registerDefinition.getAfterInstanceInitField())) {
			if(registerDefinition.getProxyModel() != ProxyModel.NONE && registerDefinition.getRegisterClass().getInterfaces().length==0 && registerDefinition.getProxyModel() != ProxyModel.BOTH)
				registerDefinition.setProxyModel(ProxyModel.CGLIB);
		}
		if(ArrayUtils.indexOf(registerDefinition.getServices(), CustomProxy.class) != -1)
			registerDefinition.setProxyModel(ProxyModel.CGLIB);
	}

	private static void deduceAfterInstanceExecuteMethod(Config config, RegisterDefinition registerDefinition) throws NoSuchMethodException {
		//如果有初始化后执行的方法
		if(config.hasPath(CONFIG_INIT)) {
			//重置默认的执行方法
			registerDefinition.setAfterInstanceExecuteMethod(null);
			//判断init的类型
			Method method;
			ConfigValue configValue = config.getValue(CONFIG_INIT);
			//字符类型--》init:method 无参数格式
			if(configValue.valueType() == ConfigValueType.STRING) {
				String methodName = (String) configValue.unwrapped();
				method = ExtReflectUtils.getEffectiveMethod(registerDefinition.getRegisterClass(),methodName,new Class<?>[0]);
				registerDefinition.addAfterInstanceExecuteMethod(new MethodDefinition(method, null, null,null,null));
			}else if(configValue.valueType() == ConfigValueType.OBJECT) {
				processMethodDefinitionObject(config, registerDefinition, configValue);
			}else if(configValue.valueType() == ConfigValueType.LIST) {
				processMethodDefinitionList(config, registerDefinition);
			}
		}
	}
	/**
	 * 处理list形式的方法定义
	 * @param config config
	 * @param registerDefinition 注册定义 
	 * @param methodName 方法名
	 * @throws NoSuchMethodException ex
	 */
	public static void processMethodDefinitionList(Config config, RegisterDefinition registerDefinition
		) throws NoSuchMethodException {
		Method method;
		String methodName;
		List<ConfigValue> configList = config.getList(CONFIG_INIT);
		Iterator<ConfigValue> iterator = configList.iterator();
		while(iterator.hasNext()) {
			ConfigValue childConfigValue = iterator.next();
			if(childConfigValue.valueType() == ConfigValueType.STRING) {
				methodName = (String) childConfigValue.unwrapped();
				method = ExtReflectUtils.getEffectiveMethod(registerDefinition.getRegisterClass(),methodName,new Class<?>[0]);
				registerDefinition.addAfterInstanceExecuteMethod(new MethodDefinition(method, null, null,null,null));
			}else {
				SimpleConfigObject simpleConfigObject = (SimpleConfigObject) childConfigValue;
				Entry<String, ConfigValue> entry = simpleConfigObject.entrySet().iterator().next();
				methodName = entry.getKey();
				MethodDefinition methodDefinition = deduceParameterTypeFromConfigValue(registerDefinition, methodName, entry.getValue());
				method = ExtReflectUtils.getEffectiveMethod(registerDefinition.getRegisterClass(), methodName, methodDefinition.getArgsType());
				methodDefinition.setMethod(method);
				registerDefinition.addAfterInstanceExecuteMethod(methodDefinition);
			}
		}
	}
	/**
	 * 处理object的方法定义
	 * @param config 配置
	 * @param registerDefinition 注册定义
	 * @param configValue 配置值
	 * @throws NoSuchMethodException ex
	 */
	public static void processMethodDefinitionObject(Config config, RegisterDefinition registerDefinition,
			ConfigValue configValue) throws NoSuchMethodException {
		String methodName;
		Method method;
		SimpleConfigObject simpleConfigObject = (SimpleConfigObject) configValue;
		Entry<String, ConfigValue> entry = simpleConfigObject.entrySet().iterator().next();
		methodName = entry.getKey();
		MethodDefinition methodDefinition = deduceParameterTypeFromObject(config,CONFIG_INIT+"."+methodName, registerDefinition, methodName);
		method = ExtReflectUtils.getEffectiveMethod(registerDefinition.getRegisterClass(), methodName, methodDefinition.getArgsType());
		methodDefinition.setMethod(method);
		registerDefinition.addAfterInstanceExecuteMethod(methodDefinition);
	}

	private static MethodDefinition deduceInstanitionMethod(Config config,RegisterDefinition registerDefinition) throws NoSuchMethodException {
		String methodName = config.getString(CONFIG_METHOD);
		MethodDefinition methodDefinition = deduceParameterType(config,registerDefinition,methodName);
		Method method = null;
		method = ExtReflectUtils.getEffectiveMethod(registerDefinition.getRegisterClass(),methodName, methodDefinition.getArgsType());
		methodDefinition.setMethod(method);
		return methodDefinition;
	}
	
	private static void deduceDestoryMethod(Config config,RegisterDefinition registerDefinition) throws NoSuchMethodException {
		if(config.hasPath(CONFIG_DESTORY)) {
			registerDefinition.setDestoryMethod(null);
			Method method;
			String methodName = null;
			ConfigValue configValue = config.getValue(CONFIG_DESTORY);
			if(configValue.valueType() == ConfigValueType.STRING) {
				methodName = (String) configValue.unwrapped();
				method = ExtReflectUtils.getEffectiveMethod(registerDefinition.getRegisterClass(),methodName,new Class<?>[0]);
				registerDefinition.setDestoryMethod(new MethodDefinition(method, null, null,null,null));
			}else if(configValue.valueType() == ConfigValueType.OBJECT) {
				SimpleConfigObject simpleConfigObject = (SimpleConfigObject) configValue;
				Entry<String, ConfigValue> entry = simpleConfigObject.entrySet().iterator().next();
				methodName = entry.getKey();
				MethodDefinition methodDefinition = deduceParameterTypeFromObject(config,CONFIG_INIT+"."+methodName, registerDefinition, methodName);
				method = ExtReflectUtils.getEffectiveMethod(registerDefinition.getRegisterClass(), methodName, methodDefinition.getArgsType());
				methodDefinition.setMethod(method);
				registerDefinition.setDestoryMethod(methodDefinition);
			}
		}
	}
	
	private static ConstructorDefinition deduceInstanitionConstructor(Config config,RegisterDefinition registerDefinition) throws NoSuchMethodException {
		ConstructorDefinition constructorDefinition = (ConstructorDefinition) deduceParameterType(config,registerDefinition,null);
		Constructor<?> constructor = null;
		constructor = ExtReflectUtils.getEffectiveConstructor(registerDefinition.getRegisterClass(), constructorDefinition.getArgsType());
		constructorDefinition.setConstructor(constructor);
		return constructorDefinition;
	}
	/**
	 * 推断出要处理的属性 
	 * @param config 配置
	 * @param registerDefinition 注册定义
	 */
	@SuppressWarnings("unchecked")
	private static void deduceInstanitionField(Config config,RegisterDefinition registerDefinition) {
		if(!config.hasPath(CONFIG_FIELD)) 
			return;
		registerDefinition.setAfterInstanceInitField(null);
		if(config.isList(CONFIG_FIELD)) {
			ConfigList list = config.getList(CONFIG_FIELD);
			list.forEach(configValue->{
				ParameterResolver<ConfigValue> parameterResolver = null;
				if(configValue.valueType()==ConfigValueType.OBJECT) {
					Entry<String, ConfigValue> entry = ((SimpleConfigObject)configValue).entrySet().iterator().next();
					String type = null;
					FieldDefinition fieldDefinition;
					String name = entry.getKey();
					configValue = entry.getValue();
					Object value = configValue.unwrapped();
					if(configValue.valueType() == ConfigValueType.OBJECT) {
						entry = ((SimpleConfigObject)configValue).entrySet().iterator().next();
						type = entry.getKey();
						configValue = entry.getValue();
					}
					Field field;
					try {
						field = registerDefinition.getLoader().getClassHelper().getAnyField(name);
						if(type != null) {
							parameterResolver = PlugsFactory.getPluginsInstanceByAttributeStrict(ParameterResolver.class, type);
							PlugsHandler handler = PlugsFactory.getPluginsHandler(parameterResolver);
							if(ReflectUtils.implementsOf(handler.getRegisterDefinition().getRegisterClass(), DelayParameterResolver.class)) {
								value = configValue;
							}else {
								value = parameterResolver.resove(configValue, type, 0, registerDefinition);
								parameterResolver = null;
							}
						}
						if(field == null)
							throw new NoSuchFieldException(name);
						fieldDefinition = new FieldDefinition(field, type, value,parameterResolver);
						registerDefinition.addAfterInstanceInitField(fieldDefinition);
					} catch (NoSuchFieldException | SecurityException e) {
						throw new PluginInitException("can't found field for "+registerDefinition.getRegisterClass().getName()+"."+name,e);
					}
				}
			});
		}else {
			config = config.getConfig(CONFIG_FIELD);
			Iterator<Entry<String, ConfigValue>> iterator = config.entrySet().iterator();
			while(iterator.hasNext()) {
				ParameterResolver<ConfigValue> parameterResolver = null;
				String type = null;
				FieldDefinition fieldDefinition;
				ConfigValue configValue;
				Entry<String, ConfigValue> entry = iterator.next();
				String name = entry.getKey();
				configValue = entry.getValue();
				
				if(name.indexOf(".") != -1) {
					type = name.substring(name.indexOf(".")+1);
					name = name.substring(0,name.indexOf("."));
				}
				Object value = configValue.unwrapped();
				Field field;
				try {
					field = registerDefinition.getLoader().getClassHelper().getAnyField(name);
					if(type != null) {
						parameterResolver = PlugsFactory.getPluginsInstanceByAttributeStrict(ParameterResolver.class, type);
						PlugsHandler handler = PlugsFactory.getPluginsHandler(parameterResolver);
						if(ReflectUtils.implementsOf(handler.getRegisterDefinition().getRegisterClass(), DelayParameterResolver.class)) {
							value = configValue;
						}else {
							value = parameterResolver.resove(configValue, type, 0, registerDefinition);
							parameterResolver = null;
						}
					}
					if(field == null)
						throw new NoSuchFieldException(name);
					fieldDefinition = new FieldDefinition(field, type, value,parameterResolver);
					registerDefinition.addAfterInstanceInitField(fieldDefinition);
				} catch (NoSuchFieldException | SecurityException e) {
					throw new PluginInitException("can't found field for "+registerDefinition.getRegisterClass().getName()+"."+name,e);
				}
				
			}
		}
	}
	@SuppressWarnings("unchecked")
	public static MethodDefinition deduceParameterType(Config config,RegisterDefinition registerDefinition,String methodName) {
		if(!config.isList(CONFIG_ARGS)) {
			return deduceParameterTypeFromObject(config,CONFIG_ARGS, registerDefinition, methodName);
		}
		ConfigList argsConfigList = config.getList(CONFIG_ARGS);
		ConfigList argsTypesConfigList = config.getList(CONFIG_TYPES);
		Object[] argsValue = null;
		ParameterResolver<ConfigValue>[] parameterResolvers = null;
		if(argsConfigList == null) {
			argsValue = new Object[0];
		}else {
			argsValue = argsConfigList.unwrapped().toArray();
		}
		Class<?>[] argsTypes = new Class<?>[argsValue.length];
		String[] types = null;
		for(int i = 0;i< argsTypes.length;i++) {
			if(i == 0) {
				types = new String[argsTypes.length];
			}
			if(argsTypesConfigList != null)
				types[i] = (String) argsTypesConfigList.get(i).unwrapped();
			argsTypes[i] = argsValue[i].getClass();
		}
		if(argsTypesConfigList != null) {
			for(int i = 0;i< argsTypesConfigList.size();i++) {
				String type = (String) argsTypesConfigList.get(i).unwrapped();
				if(StringUtil.equals(type, "default") || StringUtil.equals(type, "-")){
					continue;
				}
				//获取解析器
				ParameterResolver<ConfigValue> parameterResolver = PlugsFactory.getPluginsInstanceByAttributeStrict(ParameterResolver.class, type);
				ConfigValue configValue = argsConfigList.get(i);
				PlugsHandler handler = PlugsFactory.getPluginsHandler(parameterResolver);
				if(ReflectUtils.implementsOf(handler.getRegisterDefinition().getRegisterClass(), DelayParameterResolver.class)) {
					argsTypes[i] = ((DelayParameterResolver<ConfigValue>)parameterResolver).parameterType(configValue,methodName,argsTypes, type,i, registerDefinition);
					if(parameterResolvers == null) {
						parameterResolvers = new ParameterResolver[argsTypes.length];
					}
					parameterResolvers[i] = parameterResolver;
					argsValue[i] = argsConfigList.get(i);
					continue;
				}
				argsValue[i] = parameterResolver.resove(configValue,type,i,registerDefinition);
				if(argsValue[i] != null)
					argsTypes[i] = argsValue[i].getClass();
			}
		}
		return methodName == null?new ConstructorDefinition(null, argsTypes, argsValue,parameterResolvers,types)
				:new MethodDefinition(null, argsTypes, argsValue,parameterResolvers,types);
	}
	private static MethodDefinition deduceParameterTypeFromObject(Config config,String configName,RegisterDefinition registerDefinition,String methodName) {
		ConfigValue configValue = config.getValue(configName);
		return deduceParameterTypeFromConfigValue(registerDefinition, methodName, configValue);
	}

	@SuppressWarnings("unchecked")
	public static MethodDefinition deduceParameterTypeFromConfigValue(RegisterDefinition registerDefinition,
			String methodName, ConfigValue configValue) {
		Object[] argsValue;
		ParameterResolver<ConfigValue>[] parameterResolvers = null;
		String[] types = null;
		Class<?>[] argsTypes;
		if(configValue == null) {
			argsValue = new Object[0];
			argsTypes = new Class<?>[0];
		}else {
			argsValue = new Object[1];
			argsTypes = new Class<?>[1];
			if(configValue.valueType() == ConfigValueType.OBJECT) {
				SimpleConfigObject simpleConfigObject = (SimpleConfigObject) configValue;
				Entry<String,ConfigValue> entry = simpleConfigObject.entrySet().iterator().next();
				types = new String[] {entry.getKey()};
				argsValue[0] = entry.getValue();
				ParameterResolver<ConfigValue> parameterResolver = PlugsFactory
						.getPluginsInstanceByAttributeStrict(
								new TypeToken<ParameterResolver<ConfigValue>>() {}.getTypeClass()
								, types[0]);
				PlugsHandler handler = PlugsFactory.getPluginsHandler(parameterResolver);
				if(ReflectUtils.implementsOf(handler.getRegisterDefinition().getRegisterClass(), DelayParameterResolver.class)) {
					argsTypes[0] = ((DelayParameterResolver<ConfigValue>)parameterResolver).parameterType(entry.getValue(),methodName,argsTypes, types[0],0, registerDefinition);
					parameterResolvers =new ParameterResolver[1];
					parameterResolvers[0] = parameterResolver;
				}else {
					argsValue[0] = parameterResolver.resove(entry.getValue(), types[0], 0, registerDefinition);
					argsTypes[0] = argsValue[0].getClass();
				}
			}else {
				argsValue[0] = configValue.unwrapped();
				argsTypes[0] = argsValue[0].getClass();
			}
		}
		return methodName == null?new ConstructorDefinition(null, argsTypes, argsValue,parameterResolvers,types)
				:new MethodDefinition(null, argsTypes, argsValue,parameterResolvers,types);
	}

	private static void checkAfterInstanitiationMethod(String methodName, Method method,Class<?> clzz) {
		if(method == null) 
			throw new PluginInitException("can't found init method ["+methodName+"] at class "+clzz.getName());
		if(method.getParameterCount() != 0)
			throw new PluginInitException("the method executed after instantiation does not allow any parameters,but found " + method );
	}

	private static <T> Class<?>[] getPlugs(Class<?> registerClass, String declareRegister) {
		Set<Class<?>> set = new HashSet<Class<?>>();
		String[] strs = declareRegister.split(",");
		for (String str : strs) {
			if (str.trim().equals("*")) {
				Class<?>[] plugs = registerClass.getInterfaces();
				for (Class<?> plug : plugs)
					set.add(plug);
			} else {
				try {
					Class<?> interfacer = Class.forName(str.trim());
//					if (AppClassLoader.implementsOf(registerClass, interfacer))// 判断类及父类是否实现某接口
					set.add(interfacer);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
					continue;
				}
			}
		}
		Class<?>[] plugs = new Class<?>[set.size()];
		int i = 0;
		for (Class<?> plug : set)
			plugs[i++] = plug;
		return plugs;
	}

}