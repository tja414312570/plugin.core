package com.yanan.frame.plugin;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import com.yanan.frame.plugin.PluginEvent.EventType;
import com.yanan.frame.plugin.builder.PluginDefinitionBuilderFactory;
import com.yanan.frame.plugin.builder.PluginInstanceFactory;
import com.yanan.frame.plugin.builder.PluginInterceptBuilder;
import com.yanan.frame.plugin.decoder.ResourceDecoder;
import com.yanan.frame.plugin.decoder.StandAbstractResourceDecoder;
import com.yanan.frame.plugin.decoder.StandScanResource;
import com.yanan.frame.plugin.decoder.StandScanResourceDecoder;
import com.yanan.frame.plugin.definition.RegisterDefinition;
import com.yanan.frame.plugin.exception.PluginInitException;
import com.yanan.frame.plugin.exception.PluginNotFoundException;
import com.yanan.frame.plugin.exception.PluginRuntimeException;
import com.yanan.frame.plugin.exception.RegisterNotFound;
import com.yanan.frame.plugin.handler.PlugsHandler;
import com.yanan.utils.asserts.Assert;
import com.yanan.utils.reflect.AppClassLoader;
import com.yanan.utils.reflect.TypeToken;
import com.yanan.utils.reflect.cache.ClassInfoCache;
import com.yanan.utils.resource.Resource;
import com.yanan.utils.resource.ResourceManager;
import com.yanan.utils.string.StringUtil;

/**
 * Pluginin factory,initial Pluginin Context and manager & register & get Pluginin Instance 2018 7-27
 * modify MethodHandler initial when RegisterDefinition was initial,cut down proxy execution time 2018 7-12
 * add more than method intercept , update intercept rules ,add intercept handler set
 * 重构组件初始化逻辑 2020 7-14
 * 
 * @author yanan
 *
 */
public class PlugsFactory {
	private Environment environment;
	//Pluginin context loaded configure file list
	private List<Resource> resourceList;
	public Environment getEnvironment() {
		return environment;
	}
	public List<Resource> getResourceList() {
		return resourceList;
	}
	public Set<Resource> getResourceLoadedList() {
		return resourceLoadedList;
	}
	public Map<Class<?>, Plugin> getServiceContatiner() {
		return serviceContatiner;
	}
	public Set<RegisterDefinition> getNewRegisterDefinition() {
		return newRegisterDefinition;
	}
	public boolean isBefore_refresh_ready() {
		return before_refresh_ready;
	}
	private Set<Resource> resourceLoadedList;
	//Pluginin context scan package path
	//Pluginin context plugin pools
	private Map<Class<?>, Plugin> serviceContatiner = new HashMap<Class<?>, Plugin>();
	//Pluginin context Register pools
	private Map<String, RegisterDefinition> registerDefinitionContainer = new HashMap<>();
	private Set<RegisterDefinition> newRegisterDefinition = new CopyOnWriteArraySet<>();
	private volatile boolean before_refresh_ready;
	/**
	 * 组件全局上下文事件源
	 */
	private PluginEventSource eventSource;
	/**
	 * 组件工厂构造器
	 */
	private PlugsFactory() {
		resourceList = new CopyOnWriteArrayList<>();
		environment = Environment.getEnviroment();
		eventSource = new PluginEventSource();
		environment.setVariable(PluginEventSource.class.getName(), eventSource);
		resourceLoadedList = new CopyOnWriteArraySet<>();
		
	}
	/**
	 * 移除注册描述
	 * @param registerClass 注册类
	 */
	public void removeRegister(Class<?> registerClass) {
		synchronized (this) {
			RegisterDefinition registerDescription = getRegisterDefinition(registerClass);
			Class<?>[] plugClassArray = registerDescription.getServices();
			for(Class<?> plugClass : plugClassArray) {
				Plugin plugin = this.serviceContatiner.get(plugClass);
				plugin.getRegisterList().remove(registerDescription);
			}
		}
	}
	public void addScanPath(String... paths) {
		for(String path : paths) {
			addResource(new StandScanResource(path));
		}
	}
	public void addScanPath(Class<?>... clzzs) {
		addScanPath(ResourceManager.getClassPath(clzzs));
	}

	/**
	 * 初始化组件工厂
	 * @param resources initial configure file path 
	 */
	public static void init(String... resources) {
		PlugsFactory factory = getInstance();
		if(resources == null || resources.length == 0){
			resources = new String[]{"classpath:plugin.yc"};
		}
		for (String res : resources) {
			ResourceManager.getResourceList(res).forEach((resource -> 
			factory.addResource(resource)));
		}
		factory.refresh();
	}
	public static void init() {
		PlugsFactory factory = getInstance();
		String resourceDesc = "classpath:plugin.yc";
		ResourceManager.getResourceList(resourceDesc).forEach((resource -> 
		factory.addResource(resource)));
		factory.refresh();
	}
	public static void init(Resource... resources) {
		PlugsFactory factory = getInstance();
		for(Resource resource : resources) {
			 factory.addResource(resource);
		}
		factory.refresh();
	}
	public RegisterDefinition getRegisterDefinition(Class<?> registerClass) {
		RegisterDefinition registerDefinition = null ;
		try {
			registerDefinition = getRegisterDefinition(registerClass.getName());
		}catch (Exception e) {
			Plugin plugin = getPlugin(registerClass);
			if(plugin == null) {
				registerDefinition = PluginDefinitionBuilderFactory.builderRegisterDefinition(registerClass);
				this.addRegisterDefinition(registerDefinition);
			}else {
				registerDefinition = plugin.getDefaultRegisterDefinition();
			}
		}
		Assert.isNull(registerDefinition,"the register definition is null for ["+registerClass.getName()+"]");
		return registerDefinition;
	}
	public RegisterDefinition getRegisterDefinition(String registerId) {
		RegisterDefinition registerDefinition = registerDefinitionContainer.get(registerId);
		Assert.isNull(registerDefinition,"the register definition is null for ["+registerId+"]");
		return registerDefinition;
	}
	public void addPlugininDefinition(Class<?> serviceClass) {
		Assert.isNull(serviceClass);
		Plugin plugin = PluginDefinitionBuilderFactory.builderPluginDefinition(serviceClass);
		this.addPlugininDefinition(plugin);
	}
	public void addRegisterDefinition(Class<?> registerClass) {
		Assert.isNull(registerClass);
		RegisterDefinition registerDefinition = PluginDefinitionBuilderFactory.builderRegisterDefinition(registerClass);
		this.addRegisterDefinition(registerDefinition);
	}
	public void addRegisterDefinition(RegisterDefinition registerDefinition) {
		newRegisterDefinition.add(registerDefinition);
		environment.distributeEvent(eventSource, new PluginEvent(EventType.add_registerDefinition,registerDefinition));
		String id = registerDefinition.getId();
		if(StringUtil.isEmpty(id)) {
			id = registerDefinition.getRegisterClass().getName();
		}
		if(this.registerDefinitionContainer.containsKey(id)) {
			if(StringUtil.isEmpty(registerDefinition.getReferenceId())) 
				throw new PluginInitException("the register is exists for ["+id+"]");
			else {
				while(this.registerDefinitionContainer
						.containsKey(id = id + UUID.randomUUID().toString()));
			}
				
		}
		this.registerDefinitionContainer.put(id, registerDefinition);
		Class<?>[] serviceClassArray = registerDefinition.getServices();
		for(Class<?> serviceClass : serviceClassArray) {
			Plugin plugin = this.serviceContatiner.get(serviceClass);
			if(plugin == null) {
				plugin = PluginDefinitionBuilderFactory.builderPluginDefinition(serviceClass);
				this.serviceContatiner.put(serviceClass, plugin);
			}
			plugin.addRegister(registerDefinition);
		}
	}
	private void addResource(Resource resource) {
		environment.distributeEvent(eventSource, new PluginEvent(EventType.add_resource,resource));
		if(!resourceList.contains(resource)) {
			synchronized (resourceList) {
				resourceList.add(resource);
			}
		}
	}
	/**
	 * get all register pools 
	 * @return a map container register class with description
	 */
	public Map<String, RegisterDefinition> getAllRegisterDefinition() {
		return registerDefinitionContainer;
	}

	/**
	 * get all service interface map
	 * 
	 * @return a map contain all service ,the key is service class
	 */
	public Map<Class<?>, Plugin> getAllPlugin() {
		return serviceContatiner;
	}
	/**
	 * get PlugsFactory instance
	 * <p> the PluginsFacotry type is a sign instance
	 * @return a instance
	 */
	public static PlugsFactory getInstance() {
		return PlugsFactorysInstanceHolder.instance;
	}
	private static class PlugsFactorysInstanceHolder{
		static private PlugsFactory instance = new PlugsFactory();
	}
	/**
	 * initial plugin context ,need use {@link #associate()} to associate all service
	 * with register when plugin context scan all class
	 * 
	 */
	public void refresh() {
		beforeRefreshCheck();
		environment.distributeEvent(eventSource, new PluginEvent(EventType.refresh,this));
		// 判断资源文件是否存在,如果无资源文件，直接扫描所有的plugs文件
		if (this.resourceList.isEmpty()) {
			// 如果文件不存在，扫描所有的文件
			throw new PluginInitException("no any resource");
		} 
		for (Resource resource : this.resourceList) {
			if(resourceLoadedList.contains(resource))
				continue;
//			ResourceDecoder<Resource> resourceDecoder = this.getResourceDecoder(resource.getClass());
			ResourceDecoder<Resource> resourceDecoder = getPluginsInstanceByAttributeStrict(new TypeToken<ResourceDecoder<Resource>>() {}.getTypeClass(), resource.getClass().getSimpleName());
			resourceDecoder.decodeResource(this, resource);
			resourceLoadedList.add(resource);
		}
		this.newRegisterDefinition.removeIf(registerDefinition->{
			registerDefinitionInit(registerDefinition);
			return true;
		});
		environment.distributeEvent(eventSource, new PluginEvent(EventType.inited,this));
	}
	/**
	 * 检查默认的资源解析是否已经装载
	 */
	private void beforeRefreshCheck() {
		if(!before_refresh_ready) {
			synchronized (this) {
				if(!before_refresh_ready) {
					//抽象资源解析
					RegisterDefinition registerDefinitions = PluginDefinitionBuilderFactory
							.builderRegisterDefinition(StandAbstractResourceDecoder.class);
					this.addRegisterDefinition(registerDefinitions);
					//扫描资源解析
					registerDefinitions = PluginDefinitionBuilderFactory
							.builderRegisterDefinition(StandScanResourceDecoder.class);
					this.addRegisterDefinition(registerDefinitions);
					before_refresh_ready = true;
				}
			}
		}
	}
	private void registerDefinitionInit(RegisterDefinition registerDefinition) {
		environment.distributeEvent(eventSource, new PluginEvent(EventType.register_init,registerDefinition));
		PluginInterceptBuilder.builderRegisterIntercept(registerDefinition);
		if(StringUtil.isNotEmpty(registerDefinition.getId())) {
			PluginInstanceFactory.getRegisterInstance(registerDefinition, registerDefinition.getServices()[0]);
		}
	}
	/**
	 * 
	 * @param plugClass 组件类 即接口
	 * @return Plugin实例
	 */
	public static Plugin getPlugin(Class<?> serviceClass) {
		return getInstance().serviceContatiner.get(serviceClass);
	}
	public synchronized void addPlugininDefinition(Plugin plugin) {
		if(!this.serviceContatiner.containsKey(plugin.getDefinition().getPlugClass())) {
			this.serviceContatiner.put(plugin.getDefinition().getPlugClass(), plugin);
		}
	}
	public void clear() {
		this.registerDefinitionContainer.clear();
		this.serviceContatiner.clear();
	}
	
	
	public List<RegisterDefinition> getRegisterList(Class<?> serviceClass) {
		Assert.isNull(serviceClass);
		Plugin plugin = serviceContatiner.get(serviceClass);
		if (plugin == null) {
			throw new PluginRuntimeException("could found plugin for " + serviceClass.getName());
		}
		return plugin.getRegisterDefinitionList();
	}
	public static RegisterDefinition getRegisterDefinitionAllowNull(Class<?> serviceClass) {
		RegisterDefinition registerDefinition = null;
		Plugin plugin = getPlugin(serviceClass);
		if (plugin != null)
			registerDefinition = plugin.getDefaultRegisterDefinition();
		return registerDefinition;
	}
	public RegisterDefinition getRegisterDefinition(Class<?> serviceClass, String attribute, boolean strict){
		RegisterDefinition registerDescription = null;
		Plugin plugin = getPlugin(serviceClass);
		if (plugin == null) 
			throw new PluginNotFoundException("service interface " + serviceClass.getName() + " could not found or not be regist");
		if (strict) {
			registerDescription = plugin.getRegisterDefinitionByAttributeStrict(attribute);
		}else {
			registerDescription = plugin.getRegisterDefinitionByAttribute(attribute);
		}
		return registerDescription;
	}
	public static <T> RegisterDefinition getRegisterDefinition(Class<T> serviceClass, Class<?> insClass){
		RegisterDefinition registerDescription = null;
		if (serviceClass.isInterface()) {
			Plugin plugin = getPlugin(serviceClass);
			if (plugin == null) {
				throw new PluginNotFoundException("service interface " + serviceClass.getName() + " could not found or not be regist");
			}
			registerDescription = plugin.getRegisterDefinitionByInsClass(insClass);
		} else {
			registerDescription = getInstance().getRegisterDefinition(serviceClass);
		}
		return registerDescription;
	}
	

	@SuppressWarnings("unchecked")
	public static <T> T getPluginsInstance(String id) {
		// 获取一个注册描述
		RegisterDefinition registerDefinition = getInstance().getRegisterDefinition(id);
		if (registerDefinition == null) {
			throw new RegisterNotFound("could not found any register for bean id " + id);
		}
		getInstance().checkRegisterDefinition(registerDefinition);
		return (T) PluginInstanceFactory.getRegisterInstance(registerDefinition,registerDefinition.getRegisterClass());
	}
	/**
	 * 获取组件实例，当组件中有多个组件实现实例时，返回一个默认组件
	 * 具体选择某个组件实例作为默认组件实例依赖其优先级(priority)，当所有优先级相同时选第一个 优先级数值越低，优先级越高
	 * 
	 * @param serviceClass
	 * @param args
	 * @return
	 */
	public static <T> T getPluginsInstance(Class<T> serviceClass, Object... args) {
		// 获取一个注册描述
		RegisterDefinition registerDescription = getInstance().getRegisterDefinition(serviceClass);
		if (registerDescription == null) {
			throw new RegisterNotFound("service interface " + serviceClass.getName() + " could not found any register");
		}
		getInstance().checkRegisterDefinition(registerDescription);
		return PluginInstanceFactory.getRegisterInstance(registerDescription,serviceClass, args);
	}
	
	/**
	 * 获取组件实例，当组件不存在时返回传入的组件实现类的简单组件服务 如果获取组件时服务未完全初始化，则不会对其进行拦截
	 * 
	 * @param serviceClass
	 *            组件接口类
	 * @param defaultClass
	 *            默认实现类
	 * @param args
	 *            组件参数
	 * @return 代理对象
	 */
	public static <T> T getPluginsInstanceWithDefault(Class<T> serviceClass, Class<? extends T> defaultClass, Object... args) {
		try {
			RegisterDefinition registerDescription = getRegisterDefinitionAllowNull(serviceClass);
			if (registerDescription == null) {
				registerDescription = getInstance().getRegisterDefinition(defaultClass);
			}
			return PluginInstanceFactory.getRegisterInstance(registerDescription,serviceClass, args);// instance.getRegisterInstance(serviceClass,registerDescription,args);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 获取组件实例，当组件中有多个组件实现实例时，返回一个默认组件，当组件实例为空时，返回null，并不抛出异常
	 * 具体选择某个组件实例作为默认组件实例依赖其优先级(priority)，当所有优先级相同时选第一个 优先级数值越低，优先级越高
	 * 
	 * @param serviceClass
	 * @param args
	 * @return
	 */
	public static <T> T getPluginsInstanceAllowNull(Class<T> serviceClass, Object... args) {
		// 获取一个注册描述
		RegisterDefinition registerDescription = getInstance().getRegisterDefinition(serviceClass);
		if (registerDescription != null) {
			getInstance().checkRegisterDefinition(registerDescription);
			return PluginInstanceFactory.getRegisterInstance(registerDescription,serviceClass, args);
		}
		return null;
	}

	public static <T> T getPluginsInstanceNew(Class<T> serviceClass, Object... args) {
		try {
			// 获取一个注册描述
			RegisterDefinition registerDescription = getInstance().getRegisterDefinition(serviceClass);
			if (registerDescription == null)
				throw new RegisterNotFound("service interface " + serviceClass.getName() + " could not found any register");
			getInstance().checkRegisterDefinition(registerDescription);
			return PluginInstanceFactory.getRegisterNewInstance(registerDescription,serviceClass, args,null);
		} catch (Exception e) {
			throw new PluginRuntimeException("failed to get plugin instance at plugin class " + serviceClass, e);
		}
	}

	/**
	 * 获取组件实例，当组件中有多个组件实现实例时，返回一个默认组件
	 * 具体选择某个组件实例作为默认组件实例依赖其优先级(priority)，当所有优先级相同时选第一个 优先级数值越低，优先级越高
	 * 
	 * @param serviceClass
	 * @param args
	 * @return
	 */
	public static <T> T getPluginsInstanceByInsClass(Class<T> serviceClass, Class<?> insClass, Object... args) {
		RegisterDefinition registerDescription = getRegisterDefinition(serviceClass, insClass);
		if (registerDescription == null)
			throw new RegisterNotFound("service interface " + serviceClass.getName() + " could not found any register");
		getInstance().checkRegisterDefinition(registerDescription);
		return PluginInstanceFactory.getRegisterInstance(registerDescription,serviceClass, args);
	}
	/**
	 * 获取组件实例，当组件中有多个组件实现实例时，返回一个默认组件
	 * 具体选择某个组件实例作为默认组件实例依赖其优先级(priority)，当所有优先级相同时选第一个 优先级数值越低，优先级越高
	 * 
	 * @param serviceClass
	 * @param args
	 * @return
	 */
	public static <T> T getPluginsInstanceByInsClass(Class<T> serviceClass, Class<?> insClass,Class<?>[] types, Object... args) {
		RegisterDefinition registerDescription = getRegisterDefinition(serviceClass, insClass);
		if (registerDescription == null)
			throw new RegisterNotFound("service interface " + serviceClass.getName() + " could not found any register");
		getInstance().checkRegisterDefinition(registerDescription);
		return PluginInstanceFactory.getRegisterInstanceByParamType(registerDescription,serviceClass,types, args);
	}
	/**
	 * 通过组件实例的属性（attribute）获取组件实例，当组件中有多个组件实例与之匹配时，返回一个优先级组件
	 * 如果没有匹配的组件，返回一个默认组件，因此这是一种不严谨的组件获取方式，如果需要使用严谨模式（当 匹配值不通过时，返回null），需要使用方法
	 * {@link #getPluginsInstanceByAttributeStrict()}
	 * 具体选择某个组件实例作为返回组件实例依赖其优先级，当所有优先级相同时选第一个 优先级数值越低，优先级越高
	 * 
	 * @param serviceClass
	 * @param args
	 * @return
	 */
	public static <T> T getPluginsInstanceByAttribute(Class<T> serviceClass, String attribute, Object... args) {
		RegisterDefinition registerDescription = getInstance().getRegisterDefinition(serviceClass, attribute, false);
		if (registerDescription == null) {
			throw new RegisterNotFound("service interface " + serviceClass.getName() + " could not found any register for attr ["+attribute+"]");
		}
		getInstance().checkRegisterDefinition(registerDescription);
		return PluginInstanceFactory.getRegisterInstance(registerDescription,serviceClass, args);// instance.getRegisterInstance(serviceClass,registerDescription,args);
	}
//
	/**
	 * 通过组件实例的属性（attribute）获取组件实例，当组件中有多个组件实例与之匹配时，返回一个优先级组件
	 * 如果没有匹配的组件，会返回null，因此这是一种不严谨的组件获取方式，如果想当匹配不通过时，返回一个 默认组件，需要使用方法
	 * {@link #getPluginsInstanceByAttribute())
	 * 具体选择某个组件实例作为返回组件实例依赖其优先级，当所有优先级相同时选第一个 优先级数值越低，优先级越高
	 * 
	 * @param serviceClass
	 * @param args
	 * @return
	 */
	public static <T> T getPluginsInstanceByAttributeStrict(Class<T> serviceClass, String attribute, Object... args) {
		RegisterDefinition registerDescription = getInstance().getRegisterDefinition(serviceClass, attribute, true);
		if (registerDescription == null) {
			throw new RegisterNotFound("service interface " + serviceClass.getName() + " could not found any register for attr ["+attribute+"]");
		}
		getInstance().checkRegisterDefinition(registerDescription);
		return PluginInstanceFactory.getRegisterInstance(registerDescription,serviceClass, args);
	}
	private void checkRegisterDefinition(RegisterDefinition registerDefinition) {
		if(this.newRegisterDefinition.contains(registerDefinition)) {
			registerDefinitionInit(registerDefinition);
		}
	}
	
	public <T> Plugin getPluginNonNull(Class<T> serviceClass) {
		Plugin plugin = getPlugin(serviceClass);
		if (plugin == null)
			throw new PluginNotFoundException("service interface " + serviceClass.getName() + " could not found or not be regist");
		return plugin;
	}
	/**
	 * 获取组件的实例列表 返回的list按其优先级排序 优先级数值越低，优先级越高
	 * 
	 * @param serviceClass
	 * @param args
	 * @return
	 */
	public static <T> List<T> getPluginsInstanceListByAttribute(Class<T> serviceClass, String attribute, Object... args) {
		try {
			List<RegisterDefinition> registerDescriptionList = getInstance().getPluginNonNull(serviceClass).getRegisterDefinitionListByAttribute(attribute);
			return getPluginsInstanceList(serviceClass,registerDescriptionList, args);
		} catch (Exception e) {
			throw new PluginRuntimeException("failed to get plugin instance at plugin class " + serviceClass, e);
		}
	}
	public static <T> List<T> getPluginsInstanceList(Class<T> serviceClass,List<RegisterDefinition> registerDescriptionList,Object... args){
		List<T> objectList = new ArrayList<T>();
		registerDescriptionList.forEach(registerDescription->{
			getInstance().checkRegisterDefinition(registerDescription);
			objectList.add(PluginInstanceFactory.getRegisterInstance(registerDescription,serviceClass, args));
		});
		return objectList;
	}
	/**
	 * 获取组件的实例列表 返回的list按其优先级排序 优先级数值越低，优先级越高
	 * 
	 * @param serviceClass
	 * @param args
	 * @return
	 */
	public static <T> List<T> getPluginsInstanceList(Class<T> serviceClass, Object... args) {
			List<RegisterDefinition> registerDescriptionList = getInstance().getPluginNonNull(serviceClass)
					.getRegisterDefinitionList();
			return getPluginsInstanceList(serviceClass,registerDescriptionList, args);
	}

	

	public static Map<Class<Annotation>, List<Annotation>> getAnnotationGroup(Annotation[] annotations,List<Class<Annotation>> annoTypes){
		if (annotations.length == 0) {
			return null;
		}
		Map<Class<Annotation>, List<Annotation>> annoGroup = new HashMap<Class<Annotation>, List<Annotation>>();
		if (annoTypes.size() == 0) {// 没有指定注解参数时，返回空，因为没有意义
			return null;
		}
		// 遍历所有注解，进行分组添加
		for (Annotation annotation : annotations) {
			for (Class<Annotation> annoType : annoTypes) {
				Annotation annoMark = annotation.annotationType().getAnnotation(annoType);
				if (annoMark != null) {
					List<Annotation> list = annoGroup.get(annoType);
					if (list == null) {
						list = new LinkedList<Annotation>();
						list.add(annotation);
						annoGroup.put(annoType, list);
					} else {
						list.add(annotation);
					}
				}
			}
		}
		return annoGroup;
	}
	public static Map<Class<Annotation>, List<Annotation>> getAnnotationGroup(Parameter parameter,
			@SuppressWarnings("unchecked") Class<Annotation>... annoTypes) {
		return getAnnotationGroup(parameter, Arrays.asList(annoTypes));
	}

	public static Map<Class<Annotation>, List<Annotation>> getAnnotationGroup(Parameter parameter,
			List<Class<Annotation>> annoTypes) {
		if (parameter == null) {
			return null;
		}
		Annotation[] annotations = parameter.getAnnotations();
		return getAnnotationGroup(annotations, annoTypes);
	}

	public static Map<Class<Annotation>, List<Annotation>> getAnnotationGroup(Method method,
			@SuppressWarnings("unchecked") Class<Annotation>... annoTypes) {
		return getAnnotationGroup(method, Arrays.asList(annoTypes));
	}

	public static Map<Class<Annotation>, List<Annotation>> getAnnotationGroup(Method method,
			List<Class<Annotation>> annoTypes) {
		if (method == null) {
			return null;
		}
		Annotation[] annotations = method.getAnnotations();
		return getAnnotationGroup(annotations, annoTypes);
	}
	public static Map<Class<Annotation>, List<Annotation>> getAnnotationGroup(Class<?> clzz,
			List<Class<Annotation>> annoTypes) {
		if (clzz == null) {
			return null;
		}
		Annotation[] annotations = clzz.getAnnotations();
		return getAnnotationGroup(annotations, annoTypes);
	}

	public static List<Annotation> getAnnotationGroup(Field field, Class<? extends Annotation> annotationType) {
		if (field == null || annotationType == null) {
			return null;
		}
		Annotation[] annotations = field.getAnnotations();
		return getAnnotationGroup(annotations,annotationType);
	}
	public static List<Annotation> getAnnotationGroup(Annotation[] annotations,Class<? extends Annotation> annotationType){
		if (annotations.length == 0) {
			return null;
		}
		List<Annotation> annoGroup = new LinkedList<Annotation>();
		for (Annotation annotation : annotations) {
			Annotation annoMark = annotation.annotationType().getAnnotation(annotationType);
			if (annoMark != null) {
				annoGroup.add(annotation);
			}
		}
		return annoGroup;
	}
	public static List<Annotation> getAnnotationGroup(Parameter parameter, Class<? extends Annotation> annotationType) {
		if (parameter == null || annotationType == null) {
			return null;
		}
		Annotation[] annotations = parameter.getAnnotations();
		return getAnnotationGroup(annotations,annotationType);
	}
	
	/**
	 * 获取代理对象的PluginsHandler对象
	 * 
	 * @param proxyInstance
	 * @return
	 */
	public static PlugsHandler getPluginsHandler(Object proxyInstance) {
		Field field = ClassInfoCache.getClassHelper(proxyInstance.getClass()).getAnyField("h");
		if (field == null) {
			field = ClassInfoCache.getClassHelper(proxyInstance.getClass()).getDeclaredField("CGLIB$CALLBACK_0");
		}
		if (field == null) {
			throw new PluginRuntimeException("instance \"" + proxyInstance + "\" is not proxy object");
		}
		PlugsHandler plugsHandler = null;
		try {
			plugsHandler = AppClassLoader.getFieldValue(field, proxyInstance);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new PluginInitException("failed to get instance's PluginsHandler", e);
		}
		return plugsHandler;
	}
//
	public static <T> T getPluginsInstanceNewByParamType(Class<T> serviceClass, Class<?>[] parameterType, Object... arguments) {
		RegisterDefinition registerDescription =  getRegisterDefinitionNoneNull(serviceClass);
		getInstance().checkRegisterDefinition(registerDescription);
		return PluginInstanceFactory.getRegisterNewInstanceByParamType(registerDescription,serviceClass, parameterType, arguments);
	}
	public static<T> RegisterDefinition getRegisterDefinitionNoneNull(Class<T> serviceClass){
		RegisterDefinition registerDescription = getInstance().getRegisterDefinition(serviceClass);
		if (registerDescription == null) {
			throw new RegisterNotFound("service interface " + serviceClass.getName() + " could not found any register");
		}
		return registerDescription;
	}
	public static <T> T getPluginsInstanceByParamType(Class<T> serviceClass, Class<?>[] parameterType, Object... arguments) {
		RegisterDefinition registerDescription = getRegisterDefinitionNoneNull(serviceClass);
		getInstance().checkRegisterDefinition(registerDescription);
		return PluginInstanceFactory.getRegisterInstanceByParamType(registerDescription,serviceClass, parameterType, arguments);
	}
	
}
