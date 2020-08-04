package com.yanan.framework.plugin;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import com.yanan.framework.plugin.PluginEvent.EventType;
import com.yanan.framework.plugin.annotations.Service;
import com.yanan.framework.plugin.builder.PluginDefinitionBuilderFactory;
import com.yanan.framework.plugin.builder.PluginInstanceFactory;
import com.yanan.framework.plugin.builder.PluginInterceptBuilder;
import com.yanan.framework.plugin.decoder.ResourceDecoder;
import com.yanan.framework.plugin.decoder.StandAbstractResourceDecoder;
import com.yanan.framework.plugin.decoder.StandScanResource;
import com.yanan.framework.plugin.decoder.StandScanResourceDecoder;
import com.yanan.framework.plugin.definition.RegisterDefinition;
import com.yanan.framework.plugin.exception.PluginInitException;
import com.yanan.framework.plugin.exception.PluginNotFoundException;
import com.yanan.framework.plugin.exception.PluginRuntimeException;
import com.yanan.framework.plugin.exception.RegisterNotFound;
import com.yanan.framework.plugin.handler.PlugsHandler;
import com.yanan.utils.asserts.Assert;
import com.yanan.utils.reflect.ReflectUtils;
import com.yanan.utils.reflect.TypeToken;
import com.yanan.utils.reflect.cache.ClassInfoCache;
import com.yanan.utils.resource.Resource;
import com.yanan.utils.resource.ResourceManager;
import com.yanan.utils.string.StringUtil;

/**
 * Pluginin factory,initial Pluginin Context and manager ， register ， get Pluginin Instance 2018 7-27
 * modify MethodHandler initial when RegisterDefinition was initial,cut down proxy execution time 2018 7-12
 * add more than method intercept , update intercept rules ,add intercept handler set
 * 重构组件初始化逻辑 2020 7-14
 * 
 * @author yanan
 *
 */
public class PlugsFactory {
	private Environment environment;
	//容器资源列表
	private List<Resource> resourceList;
	//已加载的资源列表
	private Set<Resource> resourceLoadedList;
	//服务定义容器
	private Map<Class<?>, Plugin> serviceContatiner = new HashMap<Class<?>, Plugin>();
	//注册定义容器
	private Map<String, RegisterDefinition> registerDefinitionContainer = new HashMap<>();
	//新加入的注册定义集合
	private Set<RegisterDefinition> newRegisterDefinitionList = new CopyOnWriteArraySet<>();
	//是否已经容器初始化，主要用于检查容器的一些必要组件是否已经准备妥当
	private volatile boolean before_refresh_ready;
	//组件全局上下文事件源
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
	 * 初始化容器
	 * @param resources initial configure file path 
	 */
	public static void init(String... resources) {
		PlugsFactory factory = getInstance();
		for (String res : resources) {
			ResourceManager.getResourceList(res).forEach((resource -> 
			factory.addResource(resource)));
		}
		init();
	}
	/**
	 * 初始化容器
	 */
	public static void init() {
		PlugsFactory factory = getInstance();
		if(factory.getResourceList() == null || factory.getResourceList().isEmpty()) {
			String resourceDesc = "classpath:plugin.yc";
			ResourceManager.getResourceList(resourceDesc).forEach((resource -> 
			factory.addResource(resource)));
		}
		factory.refresh();
	}
	/**
	 * 初始化容器
	 * @param resources 需要加入容器的资源
	 */
	public static void init(Resource... resources) {
		PlugsFactory factory = getInstance();
		for(Resource resource : resources) {
			 factory.addResource(resource);
		}
		init();
	}
	/**
	 * 增加扫描路径，支持扫描jar文件
	 * @param paths 扫描路径表达式
	 */
	public void addScanPath(String... paths) {
		for(String path : paths) {
			//将路劲转化为抽象扫描资源
			addResource(new StandScanResource(path));
		}
	}
	/**
	 * 将资源添加到容器
	 * @param resources 资源数据
	 */
	public void addResource(Resource... resources) {
		for(Resource resource : resources) {
			environment.distributeEvent(eventSource, new PluginEvent(EventType.add_resource,resource));
			synchronized (resourceList) {
				if(!resourceList.contains(resource)) {
					resourceList.add(resource);
				}
			}
		}
	}
	/**
	 * 将某个类所属的类路径加入扫描队列
	 * @param clzzs 需要扫描的类
	 */
	public void addScanPath(Class<?>... clzzs) {
		addScanPath(ResourceManager.getClassPath(clzzs));
	}
	/**
	 * 将一个类作为服务定义添加到容器
	 * @param serviceClass 服务定义
	 */
	public void addPlugininDefinition(Class<?> serviceClass) {
		Assert.isNull(serviceClass);
		Plugin plugin = PluginDefinitionBuilderFactory.builderPluginDefinition(serviceClass);
		this.addPlugininDefinition(plugin);
	}
	/**
	 * 添加定义，会自动将类转化为注解定义或则注册定义
	 * @param pluginClass 定义类
	 */
	public void addDefinition(Class<?> pluginClass) {
		if(pluginClass.isInterface() || pluginClass.getAnnotation(Service.class) != null) {
			addPlugininDefinition(pluginClass);
		}else {
			addRegisterDefinition(pluginClass);
		}
	}
	/**
	 * 将一个类做为注册定义添加到容器
	 * @param registerClass 注册定义类
	 */
	public void addRegisterDefinition(Class<?> registerClass) {
		Assert.isNull(registerClass);
		RegisterDefinition registerDefinition = PluginDefinitionBuilderFactory.builderRegisterDefinition(registerClass);
		this.addRegisterDefinition(registerDefinition);
	}
	/**
	 * 将服务定义添加到容器
	 * @param plugin 服务定义类
	 */
	public synchronized void addPlugininDefinition(Plugin plugin) {
		if(!this.serviceContatiner.containsKey(plugin.getDefinition().getPlugClass())) {
			this.serviceContatiner.put(plugin.getDefinition().getPlugClass(), plugin);
		}
	}
	/**
	 * 将注册定义添加到容器
	 * @param registerDefinition 注册定义
	 */
	public void addRegisterDefinition(RegisterDefinition registerDefinition) {
		newRegisterDefinitionList.add(registerDefinition);
		environment.distributeEvent(eventSource, new PluginEvent(EventType.add_registerDefinition,registerDefinition));
		String id = getID(registerDefinition);
		synchronized (id) {
			if(this.registerDefinitionContainer.containsKey(id)) {
				throw new PluginInitException("the register is exists for ["+id+"]");
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
	}
	public String getID(RegisterDefinition registerDefinition) {
		StringBuilder idBuilder = new StringBuilder();
		if(StringUtil.isEmpty(registerDefinition.getId())) {
			idBuilder.append(registerDefinition.getRegisterClass().getName());
		}else {
			idBuilder.append(registerDefinition.getId());
			if(StringUtil.equals(registerDefinition.getId(),registerDefinition.getReferenceId())) {
				idBuilder.append("-")
				.append(registerDefinition.getReferenceId())
				.append(registerDefinition.hashCode());
			}
		}
		return idBuilder.toString();
	}
	public PluginEventSource getEventSource() {
		return eventSource;
	}
	/**
	 * 获取某个类的注册定义
	 * @param registerClass 查找的类
	 * @return 注册定义类
	 */
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
	/**
	 * 通过ID的方式查找注册定义
	 * @param registerId 注册的ID
	 * @return 注册定义
	 */
	public RegisterDefinition getRegisterDefinition(String registerId) {
		RegisterDefinition registerDefinition = registerDefinitionContainer.get(registerId);
		Assert.isNull(registerDefinition,"the register definition is null for ["+registerId+"]");
		return registerDefinition;
	}
	/**
	 * 获取服务类的默认实例，允许返回null
	 * @param serviceClass 服务类
	 * @return 注册定义
	 */
	public static RegisterDefinition getRegisterDefinitionAllowNull(Class<?> serviceClass) {
		RegisterDefinition registerDefinition = null;
		Plugin plugin = getPlugin(serviceClass);
		if (plugin != null)
			registerDefinition = plugin.getDefaultRegisterDefinition();
		return registerDefinition;
	}
	/**
	 * 通过服务类，属性，以及严格模式返回注册器，如果为严格模式，容器会以严格模式匹配，不会返回不匹配的注册器
	 * @param serviceClass 服务类
	 * @param attribute 查找的属性
	 * @param strict 严格模式
	 * @return 注册定义
	 */
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
	/**
	 * 获取一个确定的注册定义
	 * @param serviceClass 服务类
	 * @param insClass 实现类
	 * @param <T> 目标类型
	 * @return 注册定义
	 */
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
	/**
	 * 获取注册定义和不允许空值
	 * @param serviceClass 服务类
	 * @param <T> 目标类型
	 * @return 注册定义
	 */
	public static<T> RegisterDefinition getRegisterDefinitionNoneNull(Class<T> serviceClass){
		RegisterDefinition registerDescription = getInstance().getRegisterDefinition(serviceClass);
		if (registerDescription == null) {
			throw new RegisterNotFound("service interface " + serviceClass.getName() + " could not found any register");
		}
		return registerDescription;
	}
	/**
	 * 移除注册描述
	 * @param registerClass 注册类
	 */
	public void removeRegister(Class<?> registerClass) {
		synchronized (this) {
			RegisterDefinition registerDefinition = getRegisterDefinition(registerClass);
			this.removeRegisterService(registerDefinition);
		}
	}
	/**
	 * 移除注册器
	 * @param registerDefinition 注册定义
	 */
	public void removeRegister(RegisterDefinition registerDefinition) {
		synchronized (this) {
			String id = getID(registerDefinition);
			this.registerDefinitionContainer.remove(id);
			this.removeRegisterService(registerDefinition);
		}
	}
	/**
	 * 移除注册器
	 * @param id 注册id
	 */
	public void removeRegister(String id) {
		synchronized (this) {
			RegisterDefinition registerDefinition = this.registerDefinitionContainer.get(id);
			if(registerDefinition == null)
				return;
			this.registerDefinitionContainer.remove(id);
			this.removeRegisterService(registerDefinition);
		}
	}
	/**
	 * 移除注册描述的服务中当前注册描述
	 * @param registerDefinition 注册定义
	 */
	public synchronized void removeRegisterService(RegisterDefinition registerDefinition) {
		PluginInstanceFactory.destory(registerDefinition);
		Class<?>[] serviceClassArray = registerDefinition.getServices();
		for(Class<?> serviceClass : serviceClassArray) {
			Plugin plugin = this.serviceContatiner.get(serviceClass);
			plugin.getRegisterList().remove(registerDefinition);
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
	 * initial plugin context ,need use {@link #refresh()} to associate all service
	 * with register when plugin context scan all class
	 * 
	 */
	public synchronized void refresh() {
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
		Iterator<RegisterDefinition> iterator = this.newRegisterDefinitionList.iterator();
		while(iterator.hasNext()) {
			RegisterDefinition currentRegisterDefinition = iterator.next();
			registerDefinitionInit(currentRegisterDefinition);
		}
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
	private void checkRegisterDefinition(RegisterDefinition registerDefinition) {
		if(this.newRegisterDefinitionList.contains(registerDefinition)) {
			registerDefinitionInit(registerDefinition);
		}
	}
	private void registerDefinitionInit(RegisterDefinition registerDefinition) {
		newRegisterDefinitionList.remove(registerDefinition);
		environment.distributeEvent(eventSource, new PluginEvent(EventType.register_init,registerDefinition));
		PluginInterceptBuilder.builderRegisterIntercept(registerDefinition);
		if(StringUtil.isNotEmpty(registerDefinition.getId())) {
			PluginInstanceFactory.getRegisterInstance(registerDefinition, registerDefinition.getServices()[0]);
		}
	}
	/**
	 * @param serviceClass 组件类 即接口
	 * @return Plugin实例
	 */
	public static Plugin getPlugin(Class<?> serviceClass) {
		return getInstance().serviceContatiner.get(serviceClass);
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
	/**
	 * 通过ID的方式获取实例
	 * @param id ID
	 * @param <T> 目标类型
	 * @return 实例
	 */
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
	 * @param serviceClass 服务类
	 * @param args 参数
	 * @param <T> 目标类型
	 * @return 实例
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
	 * @param serviceClass  组件接口类
	 * @param defaultClass  默认实现类
	 * @param args 组件参数
	 * @param <T> 目标类型
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
	 * @param serviceClass 服务类
	 * @param args 参数
	 * @param <T> 目标类型
	 * @return 实例
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
	/**
	 * 强制生成一个新的实例并返回
	 * @param serviceClass 服务类
	 * @param args 参数
	 * @param <T> 目标类型
	 * @return 实例
	 */
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
	 * @param serviceClass 服务接口
	 * @param args 参数
	 * @param insClass 内部实现
	 * @param <T> 目标类型
	 * @return 实例
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
	 * @param serviceClass 服务类
	 * @param insClass 内部类
	 * @param types 参数类型
	 * @param args 参数
	 * @param <T> 目标类型
	 * @return 实例
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
	 * 具体选择某个组件实例作为返回组件实例依赖其优先级，当所有优先级相同时选第一个 优先级数值越低，优先级越高
	 * 
	 * @param serviceClass 服务类
	 * @param args 参数
	 * @param attribute 属性
	 * @param <T> 目标类型
	 * @return 实例
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
	 * 如果没有匹配的组件，会抛出异常，因此这是一种严谨的组件获取方式
	 * 具体选择某个组件实例作为返回组件实例依赖其优先级，当所有优先级相同时选第一个 优先级数值越低，优先级越高
	 * 
	 * @param serviceClass 服务类
	 * @param attribute 属性
	 * @param args  参数
	 * @param <T> 目标类型
	 * @return 实例
	 */
	public static <T> T getPluginsInstanceByAttributeStrict(Class<T> serviceClass, String attribute, Object... args) {
		RegisterDefinition registerDescription = getInstance().getRegisterDefinition(serviceClass, attribute, true);
		if (registerDescription == null) {
			throw new RegisterNotFound("service interface " + serviceClass.getName() + " could not found any register for attr ["+attribute+"]");
		}
		getInstance().checkRegisterDefinition(registerDescription);
		return PluginInstanceFactory.getRegisterInstance(registerDescription,serviceClass, args);
	}
	
	public <T> Plugin getPluginNonNull(Class<T> serviceClass) {
		Plugin plugin = getPlugin(serviceClass);
		if (plugin == null)
			throw new PluginNotFoundException("service interface " + serviceClass.getName() + " could not found or not be regist");
		return plugin;
	}
	/**
	 * 获取组件中匹配属性的实例列表 
	 * 返回的list按其优先级排序 优先级数值越低，优先级越高
	 * 
	 * @param serviceClass 服务类
	 * @param args 参数
	 * @param attribute 属性
	 * @param <T> 目标类型
	 * @return 服务列表
	 */
	public static <T> List<T> getPluginsInstanceListByAttribute(Class<T> serviceClass, String attribute, Object... args) {
		try {
			List<RegisterDefinition> registerDescriptionList = getInstance().getPluginNonNull(serviceClass).getRegisterDefinitionListByAttribute(attribute);
			return getPluginsInstanceList(serviceClass,registerDescriptionList, args);
		} catch (Exception e) {
			throw new PluginRuntimeException("failed to get plugin instance at plugin class " + serviceClass, e);
		}
	}
	/**
	 * 获取组件的实例列表，容器会将Plugin的所有RegisterDefinition实例化后加入一个集合返回
	 * 返回的list按其优先级排序 优先级数值越低，优先级越高
	 * @param serviceClass 服务类
	 * @param registerDescriptionList 组件秒速列表
	 * @param args 参数
	 * @param <T> 目标类型
	 * @return 实例的集合
	 */
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
	 * @param serviceClass 服务类
	 * @param args 参数
	 * @param <T> 目标类型
	 * @return 实例列表
	 */
	public static <T> List<T> getPluginsInstanceList(Class<T> serviceClass, Object... args) {
			List<RegisterDefinition> registerDescriptionList = getInstance().getPluginNonNull(serviceClass)
					.getRegisterDefinitionList();
			return getPluginsInstanceList(serviceClass,registerDescriptionList, args);
	}
	/**
	 * 获取一个新的实例
	 * @param serviceClass 服务类型
	 * @param parameterType 参数类型
	 * @param arguments 参数
	 * @param <T> 目标类型
	 * @return 实例
	 */
	public static <T> T getPluginsInstanceNewByParamType(Class<T> serviceClass, Class<?>[] parameterType, Object... arguments) {
		RegisterDefinition registerDescription =  getRegisterDefinitionNoneNull(serviceClass);
		getInstance().checkRegisterDefinition(registerDescription);
		return PluginInstanceFactory.getRegisterNewInstanceByParamType(registerDescription,serviceClass, parameterType, arguments);
	}
	/**
	 * 获取实例通过参数类型
	 * @param serviceClass 服务类
	 * @param parameterType 参数类型
	 * @param arguments 参数
	 * @param <T> 目标类型
	 * @return 实例
	 */
	public static <T> T getPluginsInstanceByParamType(Class<T> serviceClass, Class<?>[] parameterType, Object... arguments) {
		RegisterDefinition registerDescription = getRegisterDefinitionNoneNull(serviceClass);
		getInstance().checkRegisterDefinition(registerDescription);
		return PluginInstanceFactory.getRegisterInstanceByParamType(registerDescription,serviceClass, parameterType, arguments);
	}
	/**
	 * 代理一个已经存在的实例
	 * @param instance 实例对象
	 * @param args 构造实例的参数
	 * @return 代理的实例
	 */
	@SuppressWarnings("unchecked")
	public static <T> T proxyInstance(T instance,Object...args) {
		Class<T> registerClass = (Class<T>) instance.getClass();
		RegisterDefinition registerDefinition = PlugsFactory.getInstance().getRegisterDefinition(registerClass);
		getInstance().checkRegisterDefinition(registerDefinition);
		instance = PluginInstanceFactory.getRegisterNewInstance(registerDefinition, registerClass,args,instance);
		return instance;
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
	 * @param proxyInstance 代理实例
	 * @return PlugsHandler
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
			plugsHandler = ReflectUtils.getFieldValue(field, proxyInstance);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new PluginInitException("failed to get instance's PluginsHandler", e);
		}
		return plugsHandler;
	}
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
	public Set<RegisterDefinition> getNewRegisterDefinitionList() {
		return newRegisterDefinitionList;
	}
	public boolean isBefore_refresh_ready() {
		return before_refresh_ready;
	}
	/**
	 * 销毁上下文环境
	 */
	public void destory() {
		this.before_refresh_ready = false;
		this.environment.desotry();
		this.newRegisterDefinitionList.clear();
		this.resourceList.clear();
		this.registerDefinitionContainer.values().forEach(registerDefinition->{
			PluginInstanceFactory.destory(registerDefinition);
		});
		this.registerDefinitionContainer.clear();
		this.resourceLoadedList.clear();
		this.serviceContatiner.clear();
	}
}