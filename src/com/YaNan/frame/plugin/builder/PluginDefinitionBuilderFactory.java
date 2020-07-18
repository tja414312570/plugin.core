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
import com.yanan.frame.plugin.ConstructorDefinition;
import com.yanan.frame.plugin.ParameterUtils;
import com.yanan.frame.plugin.Plugin;
import com.yanan.frame.plugin.PlugsFactory;
import com.yanan.frame.plugin.ProxyModel;
import com.yanan.frame.plugin.annotations.AfterInstantiation;
import com.yanan.frame.plugin.annotations.Register;
import com.yanan.frame.plugin.annotations.Service;
import com.yanan.frame.plugin.builder.resolver.DelayParameterResolver;
import com.yanan.frame.plugin.builder.resolver.ParameterResolver;
import com.yanan.frame.plugin.definition.FieldDefinition;
import com.yanan.frame.plugin.definition.MethodDefinition;
import com.yanan.frame.plugin.definition.PluginDefinition;
import com.yanan.frame.plugin.definition.RegisterDefinition;
import com.yanan.frame.plugin.exception.PluginInitException;
import com.yanan.frame.plugin.handler.PlugsHandler;
import com.yanan.utils.reflect.AppClassLoader;
import com.yanan.utils.string.StringUtil;

public class PluginDefinitionBuilderFactory {
	public final static class PlugDefinitionBuilderFactoryHolder {
		private static final PluginDefinitionBuilderFactory instance = new PluginDefinitionBuilderFactory();
	}

	private PluginDefinitionBuilderFactory() {
	};

	public static PluginDefinitionBuilderFactory getInstance() {
		return PlugDefinitionBuilderFactoryHolder.instance;
	}
	/**
	 * 构建一个组件或则注册器
	 * @param cls
	 * @return
	 */
	public Object builderPlugsAuto(Class<?> cls) {
		Service service = cls.getAnnotation(Service.class);
		Register register = cls.getAnnotation(Register.class);
		if (service != null || cls.isInterface()) {
				PluginDefinition plugsDescrption = new PluginDefinition(service, cls);
				Plugin plug = new Plugin(plugsDescrption);
				return plug;
		} 
		RegisterDefinition registerDescription = register == null?
				buildByDefault(cls):buildByAnnotation(register,cls);
//		PlugsFactory.getInstance().addRegisterHandlerQueue(registerDescription);
		return registerDescription;
		
	}
	
	/**
	 * 强制将类作为Plugin的定义
	 * @param cls
	 * @return
	 */
	public Plugin builderPluginDefinition(Class<?> cls) {
		Plugin plugin = PlugsFactory.getPlugin(cls);
		if(plugin == null) {
			Service service = cls.getAnnotation(Service.class);
			PluginDefinition plugsDescrption = new PluginDefinition(service, cls);
			plugin = new Plugin(plugsDescrption);
		}
		return plugin;
		
	}
	public static Class<?>[] getPlugs(Class<?> registerClass, Class<?>[] declareRegister) {
		Class<?>[] plugs;
		if (declareRegister.length == 0)
			plugs = registerClass.getInterfaces();
		else {
			plugs = new Class<?>[declareRegister.length];
			int index = 0;
			Class<?>[] cls = registerClass.getInterfaces();
			for (int i = 0; i < cls.length; i++) {
				for (int j = 0; j < plugs.length; j++) {
					if (declareRegister[j].equals(cls[i])) {
						plugs[index++] = cls[i];
					}
				}
			}
		}
		return plugs;
	}
	public RegisterDefinition buildByDefault(Class<?> clzz) {
		RegisterDefinition register = new RegisterDefinition();
		register.setRegisterClass(clzz);
		// 读取属性
		register.setPriority( Integer.MAX_VALUE);
		register.setSignlton(true);
		register.setDescription("default register description :" + clzz.getName());
		// 获取实现类
		register.setLoader(new AppClassLoader(clzz, false));
		// 获取实现类所在的接口
		register.setServices(clzz.getInterfaces());
		return register;
	}
	public RegisterDefinition buildByAnnotation(Register register, Class<?> clzz) {
		RegisterDefinition registerDefinition = new RegisterDefinition();
		AppClassLoader loader = new AppClassLoader(register.declare().equals(Object.class) ? 
				clzz : register.declare(), false);
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
	 * 处理参数中Field字段
	 * 
	 * @param field
	 */
	@SuppressWarnings("unchecked")
	private void configFields(Config config,RegisterDefinition registerDefinition) {
		if(!config.hasPath("field")) 
			return;
		if(config.isList("field")) {
			ConfigList list = config.getList("field");
			System.out.println(list);
			list.forEach(configValue->{
				ParameterResolver<ConfigValue> parameterResolver = null;
				System.out.println(configValue.valueType()+"==>"+configValue);
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
						registerDefinition.addFieldDefinition(fieldDefinition);
					} catch (NoSuchFieldException | SecurityException e) {
						throw new PluginInitException("could not found field for "+registerDefinition.getRegisterClass().getName()+"."+name,e);
					}
				}
			});
		}else {
			config = config.getConfig("field");
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
					registerDefinition.addFieldDefinition(fieldDefinition);
				} catch (NoSuchFieldException | SecurityException e) {
					throw new PluginInitException("could not found field for "+registerDefinition.getRegisterClass().getName()+"."+name,e);
				}
				
			}
		}
//		
//		ParamDesc paraDesc = getParameterDescription(fieldConfigList);
//		if (paraDesc.getName() == null)
//			throw new RuntimeException("plugins property field name is null at \"" + fieldConfigList.origin().url() + "\" at line "
//					+ fieldConfigList.origin().lineNumber());
//		Field field = registerDefinition.getLoader().getDeclaredField(paraDesc.getName());
//		if (field == null)
//			throw new RuntimeException("Field \"" + paraDesc.getName() + "\" is not exist at plug class "
//					+ registerDefinition.getRegisterClass().getName() + " at \"" + fieldConfigList.origin().url() + "\" at line " + fieldConfigList.origin().lineNumber());
//		registerDefinition.addFiledDesc(field,new FieldDesc(paraDesc.getType(), paraDesc.getValue(), field));
	}
	public RegisterDefinition buildByConfig(Config config){
		RegisterDefinition registerDefinition = null;
		try {
			config.allowKeyNull(true);
			String className = config.getString("class");
			String ref = config.getString("ref");
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
					registerDefinition = buildByAnnotation(register, loader.getLoadedClass());
				else {
					registerDefinition = new RegisterDefinition();
				}
				registerDefinition.setLoader(loader);
				registerDefinition.setRegisterClass(loader.getLoadedClass());
			}
			
			registerDefinition.setConfig(config);
			// 读取属性
			String id = config.getString("id");
			if(id != null)
				registerDefinition.setId(id);
			registerDefinition.setPriority(config.getInt("priority",registerDefinition.getPriority()));
			registerDefinition.setSignlton(config.getBoolean("signlton",registerDefinition.isSignlton()));
			String[] atts = config.hasPath("attribute")?config.getString("attribute").split(","):registerDefinition.getAttribute();
			registerDefinition.setAttribute(atts);
			registerDefinition.setDescription(config.getString("description",registerDefinition.getDescription()));
			String model = config.getString("model", registerDefinition.getProxyModel().toString());
			registerDefinition.setProxyModel(ProxyModel.getProxyModel(model));
			//属性的赋值
			configFields(config, registerDefinition);
			// 获取实现类所在的接口
			String services =  config.getString("service");
			if(services != null) {
				registerDefinition.setServices(getPlugs(registerDefinition.getRegisterClass(), services));
			}
			//获取实例后执行的方法
			Method afterInstaceMethod;
			if(config.hasPath("init")) {
				String[] methods;
				if(config.isList("init")) {
					methods = config.getStringList("init").toArray(new String[]{});
				}else {
					methods = new String[] {config.getString("init")};
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
			String instanitionMethodStr = config.getString("method");
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

	private MethodDefinition deduceInstanitionMethod(Config config,RegisterDefinition registerDefinition) throws NoSuchMethodException {
		String methodName = config.getString("method");
		MethodDefinition methodDefinition = deduceParameterType(config,registerDefinition,methodName);
		Method method = null;
		method = ParameterUtils.getEffectiveMethod(registerDefinition.getRegisterClass(),methodName, methodDefinition.getArgsType());
		methodDefinition.setMethod(method);
		return methodDefinition;
	}
	
	private ConstructorDefinition deduceInstanitionConstructor(Config config,RegisterDefinition registerDefinition) throws NoSuchMethodException {
		ConstructorDefinition constructorDefinition = (ConstructorDefinition) deduceParameterType(config,registerDefinition,null);
		Constructor<?> constructor = null;
		constructor = ParameterUtils.getEffectiveConstructor(registerDefinition.getRegisterClass(), constructorDefinition.getArgsType());
		constructorDefinition.setConstructor(constructor);
		return constructorDefinition;
	}

	@SuppressWarnings("unchecked")
	public MethodDefinition deduceParameterType(Config config,RegisterDefinition registerDefinition,String methodName) {
		ConfigList argsConfigList = config.getList("args");
		ConfigList argsTypesConfigList = config.getList("types");
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
	

	private void checkAfterInstanitiationMethod(String methodName, Method method,Class<?> clzz) {
		if(method == null) 
			throw new PluginInitException("could not found init method ["+methodName+"] at class "+clzz.getName());
		if(method.getParameterCount() != 0)
			throw new PluginInitException("the method executed after instantiation does not allow any parameters,but found " + method );
	}

	public static <T> Class<?>[] getPlugs(Class<?> registerClass, String declareRegister) {
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
