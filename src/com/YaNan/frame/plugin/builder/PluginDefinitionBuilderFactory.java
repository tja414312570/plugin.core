package com.yanan.frame.plugin.builder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import com.typesafe.config.impl.SimpleConfigObject;
import com.yanan.frame.plugin.ParameterUtils;
import com.yanan.frame.plugin.Plugin;
import com.yanan.frame.plugin.PlugsFactory;
import com.yanan.frame.plugin.ProxyModel;
import com.yanan.frame.plugin.annotations.AfterInstantiation;
import com.yanan.frame.plugin.annotations.Register;
import com.yanan.frame.plugin.annotations.Service;
import com.yanan.frame.plugin.builder.resolver.DelayParameterResolver;
import com.yanan.frame.plugin.builder.resolver.ParameterResolver;
import com.yanan.frame.plugin.definition.ConstructorDefinition;
import com.yanan.frame.plugin.definition.FieldDefinition;
import com.yanan.frame.plugin.definition.MethodDefinition;
import com.yanan.frame.plugin.definition.PluginDefinition;
import com.yanan.frame.plugin.definition.RegisterDefinition;
import com.yanan.frame.plugin.exception.PluginInitException;
import com.yanan.frame.plugin.handler.PlugsHandler;
import com.yanan.utils.reflect.AppClassLoader;
import com.yanan.utils.reflect.TypeToken;
import com.yanan.utils.string.StringUtil;

/**
 * 组件定义构造工厂
 * @author yanan
 */
public class PluginDefinitionBuilderFactory {
	private static final String CONFIG_ARGS = "args";
	private static final String CONFIG_TYPES = "types";
	private static final String CONFIG_ID = "id";
	private static final String CONFIG_PRIORITY = "priority";
	private static final String CONFIG_ATTRIBUTE = "attribute";
	private static final String CONFIG_REF = "ref";
	private static final String CONFIG_CLASS = "class";
	private static final String CONFIG_MODEL = "model";
	private static final String CONFIG_SIGNITON = "signlton";
	private static final String CONFIG_DESCIPTION = "description";
	private static final String CONFIG_FIELD = "field";
	private static final String CONFIG_SERVICE = "service";
	private static final String CONFIG_INIT = "init";
	private static final String CONFIG_METHOD = "method";

	/**
	 * 构建一个组件或则注册器
	 * @param pluginClass
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
		registerDefinition.setServices(register.register().length == 0 ? 
				loader.getLoadedClass().getInterfaces() : register.register());
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
		Method[] methods = registerDefinition.getRegisterClass().getMethods();
		for(Method method : methods) {
			AfterInstantiation afterInstanceAnno = method.getAnnotation(AfterInstantiation.class);
			if(afterInstanceAnno != null) {
				checkAfterInstanitiationMethod(method.getName(),method,registerDefinition.getRegisterClass());
				registerDefinition.addAfterInstanceExecuteMethod(new MethodDefinition(method, null, null,null,null));
			}
		}
		return registerDefinition;
		//查找实例化后的方法
//		checkPlugs(registerDefinition.getPlugs());
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
				throw new RuntimeException("could not fond class property and no reference any at \""
						+ config.origin().url() + "\" at line : " + config.origin().lineNumber());
			if(ref != null) {
				RegisterDefinition	refDefinition = PlugsFactory.getInstance().getRegisterDefinition(ref);
				registerDefinition = AppClassLoader.clone(RegisterDefinition.class, refDefinition);
				registerDefinition.setReferenceId(ref);
			}
			if(className != null) {
				AppClassLoader loader = new AppClassLoader(className, false);
				Register register = loader.getLoadedClass().getAnnotation(Register.class);
				if(register != null)
					registerDefinition = buildRegisterDefinitionByAnnotation(register, loader.getLoadedClass());
				else {
					registerDefinition = new RegisterDefinition();
				}
				registerDefinition.setLoader(loader);
				registerDefinition.setRegisterClass(loader.getLoadedClass());
			}
			
			registerDefinition.setConfig(config);
			// 读取属性
			String id = config.getString(CONFIG_ID);
			if(id != null)
				registerDefinition.setId(id);
			registerDefinition.setPriority(config.getInt(CONFIG_PRIORITY,registerDefinition.getPriority()));
			registerDefinition.setSignlton(config.getBoolean(CONFIG_SIGNITON,registerDefinition.isSignlton()));
			String[] atts = config.hasPath(CONFIG_ATTRIBUTE)?config.getString(CONFIG_ATTRIBUTE).split(","):registerDefinition.getAttribute();
			registerDefinition.setAttribute(atts);
			registerDefinition.setDescription(config.getString(CONFIG_DESCIPTION,registerDefinition.getDescription()));
			String model = config.getString(CONFIG_MODEL, registerDefinition.getProxyModel().toString());
			registerDefinition.setProxyModel(ProxyModel.getProxyModel(model));
			//属性的赋值
			if(config.hasPath(CONFIG_FIELD)) {
				registerDefinition.setAfterInstanceInitField(null);
				deduceInstanitionField(config, registerDefinition);
			}
			// 获取实现类所在的接口
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
			//获取实例后执行的方法
			Method afterInstaceMethod;
			if(config.hasPath(CONFIG_INIT)) {
				registerDefinition.setAfterInstanceExecuteMethod(null);
				String[] methods;
				if(config.isList(CONFIG_INIT)) {
					methods = config.getStringList(CONFIG_INIT).toArray(new String[]{});
				}else {
					methods = new String[] {config.getString(CONFIG_INIT)};
				}
				for (int i = 0; i < methods.length; i++) {
					try {
						afterInstaceMethod = registerDefinition.getLoader().getMethod(methods[i]);
						checkAfterInstanitiationMethod(methods[i],afterInstaceMethod,registerDefinition.getRegisterClass());
						registerDefinition.addAfterInstanceExecuteMethod(new MethodDefinition(afterInstaceMethod, null, null,null,null));
					} catch (SecurityException e) {
						throw new PluginInitException("failed to get init method \"" + methods[i] + "\"", e);
					}
				}
			}
			//获取方法定义
			String instanitionMethodStr = config.getString(CONFIG_METHOD);
			if(!StringUtil.isBlank(instanitionMethodStr)) {
				MethodDefinition instanitionMethod = deduceInstanitionMethod(config,registerDefinition);
				registerDefinition.setInstanceMethod(instanitionMethod);
			}else if(!StringUtil.isBlank(id)){
				//如果id不为空
				ConstructorDefinition constructorDefinition = deduceInstanitionConstructor(config, registerDefinition);
				registerDefinition.setInstanceConstructor(constructorDefinition);
			}
				
//				checkPlugs(register.get);
//			PlugsFactory.getInstance().addRegisterHandlerQueue(this);
			return registerDefinition;
		} catch (Exception e) {
				throw new PluginInitException("plugin exception init at \"" + config + "\" at line "
						+ config.origin().lineNumber(), e);
		}
	}

	private static MethodDefinition deduceInstanitionMethod(Config config,RegisterDefinition registerDefinition) throws NoSuchMethodException {
		String methodName = config.getString(CONFIG_METHOD);
		MethodDefinition methodDefinition = deduceParameterType(config,registerDefinition,methodName);
		Method method = null;
		method = ParameterUtils.getEffectiveMethod(registerDefinition.getRegisterClass(),methodName, methodDefinition.getArgsType());
		methodDefinition.setMethod(method);
		return methodDefinition;
	}
	
	private static ConstructorDefinition deduceInstanitionConstructor(Config config,RegisterDefinition registerDefinition) throws NoSuchMethodException {
		ConstructorDefinition constructorDefinition = (ConstructorDefinition) deduceParameterType(config,registerDefinition,null);
		Constructor<?> constructor = null;
		constructor = ParameterUtils.getEffectiveConstructor(registerDefinition.getRegisterClass(), constructorDefinition.getArgsType());
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
						field = registerDefinition.getLoader().getInfoCache().getAnyField(name);
						if(type != null) {
							parameterResolver = PlugsFactory.getPluginsInstanceByAttributeStrict(ParameterResolver.class, type);
							PlugsHandler handler = PlugsFactory.getPluginsHandler(parameterResolver);
							if(AppClassLoader.implementsOf(handler.getRegisterDefinition().getRegisterClass(), DelayParameterResolver.class)) {
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
						throw new PluginInitException("could not found field for "+registerDefinition.getRegisterClass().getName()+"."+name,e);
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
					field = registerDefinition.getLoader().getInfoCache().getAnyField(name);
					if(type != null) {
						parameterResolver = PlugsFactory.getPluginsInstanceByAttributeStrict(ParameterResolver.class, type);
						PlugsHandler handler = PlugsFactory.getPluginsHandler(parameterResolver);
						if(AppClassLoader.implementsOf(handler.getRegisterDefinition().getRegisterClass(), DelayParameterResolver.class)) {
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
					throw new PluginInitException("could not found field for "+registerDefinition.getRegisterClass().getName()+"."+name,e);
				}
				
			}
		}
	}
	@SuppressWarnings("unchecked")
	public static MethodDefinition deduceParameterType(Config config,RegisterDefinition registerDefinition,String methodName) {
		if(!config.isList(CONFIG_ARGS)) {
			return deduceParameterTypeFromObject(config, registerDefinition, methodName);
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
				if(AppClassLoader.implementsOf(handler.getRegisterDefinition().getRegisterClass(), DelayParameterResolver.class)) {
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
	@SuppressWarnings("unchecked")
	private static MethodDefinition deduceParameterTypeFromObject(Config config,RegisterDefinition registerDefinition,String methodName) {
		ConfigValue configValue = config.getValue(CONFIG_ARGS);
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
				if(AppClassLoader.implementsOf(handler.getRegisterDefinition().getRegisterClass(), DelayParameterResolver.class)) {
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
			throw new PluginInitException("could not found init method ["+methodName+"] at class "+clzz.getName());
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
