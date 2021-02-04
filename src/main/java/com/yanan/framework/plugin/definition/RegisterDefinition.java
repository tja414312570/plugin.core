package com.yanan.framework.plugin.definition;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.typesafe.config.Config;
import com.yanan.framework.plugin.ProxyModel;
import com.yanan.framework.plugin.annotations.Register;
import com.yanan.framework.plugin.handler.FieldHandler;
import com.yanan.framework.plugin.handler.HandlerSet;
import com.yanan.framework.plugin.handler.InstanceHandler;
import com.yanan.framework.plugin.handler.InvokeHandler;
import com.yanan.utils.reflect.AppClassLoader;

/**
 * 组件描述类 用于创建组件时的组件信息 v1.0 支持通过Class的Builder方式 v1.1 支持通过Comp文件的Builder方式 v1.2
 * 支持创建默认的Builder方式 v1.3 支持描述器的属性 v1.4 将Handler的创建迁移到组件初始化时，大幅度提高代理执行效率
 * v1.5 20180910 重新构建Handler的逻辑，提高aop的效率 v1.6 20180921
 * 添加FieldHandler和ConstructorHandler 实现方法拦截与构造器拦截 v1.6 20190319
 * 支持构造器参数，支持初始化后调用方法参数，支持构造器和方法匹配，参数数据结构多种支持，参数类型自动匹配
 * 独立出所有非定义类逻辑 v1.7 20200716
 * 
 * @author yanan
 *
 */
public class RegisterDefinition {
	//代理容器，当组件拥有单例属性，此容器用于存放实例
	private volatile Map<Integer, Object> proxyContainer;
	//组件类，用于描述此定义所属的具体类
	private Class<?> registerClass;
	//引用ID，当此定义引用其他定义时拥有此属性
	private String referenceId;
	//类加载器
	private AppClassLoader loader;
	//注册注解 通过注解注册时有效
	private Register register;
	//组件接口类，普通类（为实现服务接口的类）无效
	private Class<?>[] services;
	//优先级，值越大，优先级越低
	private int priority = 0;
	//属性，此属性为匹配属性
	private String[] attribute = { "*" };
	//是否为单例模式
	private boolean signlton;
	//组件文件，当组件用文件注册时有效
	private File file;
	//描述，用于组件说明等
	private String description = "";
	//代理方式
	private ProxyModel proxyModel = ProxyModel.DEFAULT;
	//方法拦截器 映射
	private Map<Method, HandlerSet> methodInterceptMapping;
	//Config
	private Config config;
	//实例化的方法，当此属性存在，组件使用此方法获取对象
	private MethodDefinition instanceMethod;
	//实例化的方法，当此属性存在，组件使用此方法获取对象
	private MethodDefinition destoryMethod;
	//实例化的构造器，当此属性存在，组件使用此构造器实例化对象
	private ConstructorDefinition instanceConstructor;
	//构造器拦截器链路和构造器的拦截器链
	private Map<Constructor<?>, HandlerSet> constructorInterceptMapping;
	//一个容器，可用于记录，查询该注册器的相关数据
	private Map<String, Object> attributes;
	//属性拦截器链
	private Map<Field, HandlerSet> fieldInterceptMapping;
	//初始化后执行的方法集合
	private List<MethodDefinition> afterInstanceExecuteMethod;
	//初始化后需要处理属性结合
	private List<FieldDefinition> afterInstanceInitField;
	//组件ID
	private String id;
	//链接对象
	private RegisterDefinition linkRegister;
	//链接之后的原代理的代理对象
	private Object linkProxy;
	
	private boolean lazyInit = false;

	public AppClassLoader getLoader() {
		return loader;
	}


	public ProxyModel getProxyModel() {
		return proxyModel;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public MethodDefinition getDestoryMethod() {
		return destoryMethod;
	}


	public void setDestoryMethod(MethodDefinition destoryMethod) {
		this.destoryMethod = destoryMethod;
	}

	public Class<?> getClzz() {
		return registerClass;
	}

	public void setLoader(AppClassLoader loader) {
		this.loader = loader;
	}

	public void setConstructorMapping(Map<Constructor<?>, HandlerSet> constructorMapping) {
		this.constructorInterceptMapping = constructorMapping;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	public void setLinkRegister(RegisterDefinition linkRegister) {
		this.linkRegister = linkRegister;
	}

	public void setLinkProxy(Object linkProxy) {
		this.linkProxy = linkProxy;
	}

	
	public Map<Method, HandlerSet> getMethodInterceptMapping() {
		return methodInterceptMapping;
	}

	public void setMethodInterceptMapping(Map<Method, HandlerSet> methodInterceptMapping) {
		this.methodInterceptMapping = methodInterceptMapping;
	}

	public Map<Constructor<?>, HandlerSet> getConstructorInterceptMapping() {
		return constructorInterceptMapping;
	}

	public void setConstructorInterceptMapping(Map<Constructor<?>, HandlerSet> constructorInterceptMapping) {
		this.constructorInterceptMapping = constructorInterceptMapping;
	}

	public Map<Field, HandlerSet> getFieldInterceptMapping() {
		return fieldInterceptMapping;
	}

	public void setFieldInterceptMapping(Map<Field, HandlerSet> fieldInterceptMapping) {
		this.fieldInterceptMapping = fieldInterceptMapping;
	}

	public Register getRegister() {
		return register;
	}


	public RegisterDefinition getLinkRegister() {
		return linkRegister;
	}

	public Object getLinkProxy() {
		return linkProxy;
	}

	public AppClassLoader getProxyAppClassLoader() {
		return loader;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RegisterDefinition other = (RegisterDefinition) obj;
		if (!Arrays.equals(attribute, other.attribute))
			return false;
		if (attributes == null) {
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		if (registerClass == null) {
			if (other.registerClass != null)
				return false;
		} else if (!registerClass.equals(other.registerClass))
			return false;
		if (config == null) {
			if (other.config != null)
				return false;
		} else if (!config.equals(other.config))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (file == null) {
			if (other.file != null)
				return false;
		} else if (!file.equals(other.file))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (!Objects.equals(afterInstanceExecuteMethod, other.afterInstanceExecuteMethod))
			return false;
		if (!Arrays.equals(services, other.services))
			return false;
		if (priority != other.priority)
			return false;
		if (proxyModel != other.proxyModel)
			return false;
		if (register == null) {
			if (other.register != null)
				return false;
		} else if (!register.equals(other.register))
			return false;
		if (signlton != other.signlton)
			return false;
		return true;
	}

	public synchronized void createProxyContainer() {
		if (this.proxyContainer == null)
			this.proxyContainer = new HashMap<Integer, Object>();
	}
	@SuppressWarnings("unchecked") 
	public <T> T getProxyInstance(int hashKey) {
		Object proxy = null;
		if (this.proxyContainer == null)
			this.createProxyContainer();
		proxy = this.proxyContainer.get(hashKey);
		return (T) proxy;
	}

	public void setProxyInstance( int hashKey, Object proxy) {
		if (this.proxyContainer == null)
			this.createProxyContainer();
		proxy = this.proxyContainer.put(hashKey, proxy);
	}
	public void setAttribute(String name, Object value) {
		if (this.attributes == null)
			this.attributes = new HashMap<String, Object>();
		this.attributes.put(name, value);
	}

	@SuppressWarnings("unchecked")
	public <T> T getAttribute(String name) {
		return this.attribute == null ? null : (T) this.attributes.get(name);
	}
	
	public synchronized void addFieldHandler(Field field, FieldHandler handler) {
		addFieldHandler(field, new HandlerSet(handler));
	}
	/**
	 * 给属性添加Handler
	 *  
	 * @param field 属性
	 * @param invokeHandlerSet 属性
	 */
	public synchronized void addFieldHandler(Field field, HandlerSet invokeHandlerSet) {
		if (fieldInterceptMapping == null) {
			fieldInterceptMapping = new HashMap<Field, HandlerSet>();
		}
		HandlerSet ihs = fieldInterceptMapping.get(field);
		if (ihs == null) {
			fieldInterceptMapping.put(field, invokeHandlerSet);
		} else if(!ihs.hasHandlerSet(invokeHandlerSet)) {
			ihs.getLast().addHandlerSet(invokeHandlerSet);
		}

	}
	public HandlerSet getFieldHandler(Field field) {
		if(fieldInterceptMapping != null)
			return fieldInterceptMapping.get(field);
		return null;
	}
	/**
	 * 给方法添加Handler
	 * 
	 * @param method 方法
	 * @param handler handler
	 */
	public synchronized void addMethodHandler(Method method, InvokeHandler handler) {
		addMethodHandler(method, new HandlerSet(handler));
	}
	public HandlerSet getMethodHandler(Method method) {
		if(methodInterceptMapping != null)
			return methodInterceptMapping.get(method);
		return null;
	}
	/**
	 * 给方法添加Handler
	 * 
	 * @param method 方法
	 * @param invokeHandlerSet handler
	 */
	public synchronized void addMethodHandler(Method method, HandlerSet invokeHandlerSet) {
		if (methodInterceptMapping == null) {
			methodInterceptMapping = new HashMap<Method, HandlerSet>();
		}
		HandlerSet ihs = methodInterceptMapping.get(method);
		if (ihs == null) {
			methodInterceptMapping.put(method, invokeHandlerSet);
		} else if(!ihs.hasHandlerSet(invokeHandlerSet)){
			ihs.getLast().addHandlerSet(invokeHandlerSet);
		}

	}

	/**
	 * 给构造器添加Handler
	 *  
	 * @param constructor 构造器
	 * @param handler handler
	 */
	public synchronized void addConstructorHandler(Constructor<?> constructor, InstanceHandler handler) {
		addConstructorHandler(constructor, new HandlerSet(handler));
	}
	public HandlerSet getConstructorHandler(Constructor<?> constructor) {
		if(constructorInterceptMapping != null)
			return constructorInterceptMapping.get(constructor);
		return null;
	}
	/**
	 * 给方法添加Handler
	 * 
	 * @param constructor 构造器
	 * @param invokeHandlerSet invokeHandlerSet
	 */
	public synchronized void addConstructorHandler(Constructor<?> constructor, HandlerSet invokeHandlerSet) {
		if (constructorInterceptMapping == null) {
			constructorInterceptMapping = new HashMap<Constructor<?>, HandlerSet>();
		}
		HandlerSet ihs = constructorInterceptMapping.get(constructor);
		if (ihs == null) {
			constructorInterceptMapping.put(constructor, invokeHandlerSet);
		} else if(!ihs.hasHandlerSet(invokeHandlerSet)) {
			ihs.getLast().addHandlerSet(invokeHandlerSet);
		}

	}
	public void setProxyModel(ProxyModel proxyModel) {
		this.proxyModel = proxyModel;
	}
	@Override
	public String toString() {
		return "RegisterDefinition [proxyContainer=" + proxyContainer + ", registerClass=" + registerClass
				+ ", referenceId=" + referenceId + ", loader=" + loader + ", register=" + register + ", services="
				+ Arrays.toString(services) + ", priority=" + priority + ", attribute=" + Arrays.toString(attribute)
				+ ", signlton=" + signlton + ", file=" + file + ", description="
				+ description + ", proxyModel=" + proxyModel + ", methodInterceptMapping=" + methodInterceptMapping
				+ ", config=" + config + ", instanceMethod=" + instanceMethod + ", instanceConstructor="
				+ instanceConstructor + ", constructorInterceptMapping=" + constructorInterceptMapping + ", attributes="
				+ attributes + ", fieldInterceptMapping=" + fieldInterceptMapping + ", afterInstanceExecuteMethod="
				+ afterInstanceExecuteMethod + ", afterInstanceInitField=" + afterInstanceInitField + ", id=" + id
				+ ", linkRegister=" + linkRegister + ", linkProxy=" + linkProxy + "]";
	}

	public Class<?> getRegisterClass() {
		return registerClass;
	}

	public void setRegister(Register register) {
		this.register = register;
	}

	public Class<?>[] getServices() {
		return services;
	}

	public void setServices(Class<?>[] services) {
		this.services = services;
	}

	public void setRegisterClass(Class<?> registerClass) {
		this.registerClass = registerClass;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public String[] getAttribute() {
		return attribute;
	}

	public void setAttribute(String... attribute) {
		this.attribute = attribute;
	}

	public boolean isSignlton() {
		return signlton;
	}

	public void setSignlton(boolean signton) {
		this.signlton = signton;
	}

	public Map<Constructor<?>, HandlerSet> getConstructorMapping() {
		return constructorInterceptMapping;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public String getDescription() {
		return description;
	}

	public Map<Integer, Object> getProxyContainer() {
		return proxyContainer;
	}

	public void setProxyContainer(Map<Integer, Object> proxyContainer) {
		this.proxyContainer = proxyContainer;
	}


	public void setDescription(String description) {
		this.description = description;
	}


	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}

//	public void updateRegister(Class<?> registerClass) {
//		this.signlton = true;// 启用单例
//		if (this.proxyContainer != null)
//			this.proxyContainer.clear();// 清理代理容器
//		this.linkRegister = new RegisterDefinition(registerClass);// 设置链接注册器
//		PlugsFactory.getInstance().addPlugs(registerClass);
//		PlugsFactory.getInstance().associate();
//		this.linkProxy = null;// 此时应将代理重置，以更新具体对象
//	}

	/**
	 * 获取对象实例化后执行的方法
	 * 
	 * @return 方法的集合
	 */
	public List<MethodDefinition> getAfterInstanceExecuteMethod() {
		return afterInstanceExecuteMethod;
	}

	public List<FieldDefinition> getAfterInstanceInitField() {
		return afterInstanceInitField;
	}

	public void setAfterInstanceInitField(List<FieldDefinition> afterInstanceInitField) {
		this.afterInstanceInitField = afterInstanceInitField;
	}

	/**
	 * 添加对象实例化后执行的方法
	 * 
	 * @param methodDefinition 要执行的方法的定义
	 */
	public void addAfterInstanceExecuteMethod(MethodDefinition methodDefinition) {
		if (this.afterInstanceExecuteMethod == null) {
			afterInstanceExecuteMethod = new ArrayList<>();
			if (this.proxyModel != ProxyModel.BOTH && this.proxyModel != ProxyModel.CGLIB)
				this.proxyModel = ProxyModel.CGLIB;
		}
		if(!afterInstanceExecuteMethod.contains(methodDefinition))
			this.afterInstanceExecuteMethod.add(methodDefinition);
	}

	public void setAfterInstanceExecuteMethod(List<MethodDefinition> afterInstanceExecuteMethod) {
		this.afterInstanceExecuteMethod = afterInstanceExecuteMethod;
	}

	public String getReferenceId() {
		return referenceId;
	}

	public void setReferenceId(String referenceId) {
		this.referenceId = referenceId;
	}

	public MethodDefinition getInstanceMethod() {
		return instanceMethod;
	}

	public void setInstanceMethod(MethodDefinition instanceMethod) {
		this.instanceMethod = instanceMethod;
	}

	public ConstructorDefinition getInstanceConstructor() {
		return instanceConstructor;
	}

	public void setInstanceConstructor(ConstructorDefinition instanceConstructor) {
		this.instanceConstructor = instanceConstructor;
	}
	/**
	 * 当实例化后，需要处理的属性
	 * @param fieldDefinition 属性定义
	 */
	public void addAfterInstanceInitField(FieldDefinition fieldDefinition) {
		if (this.afterInstanceInitField == null) {
			afterInstanceInitField = new ArrayList<>();
		}
		if(!afterInstanceInitField.contains(fieldDefinition))
			this.afterInstanceInitField.add(fieldDefinition);
	}


	public boolean isLazyInit() {
		return lazyInit;
	}


	public void setLazyInit(boolean lazyInit) {
		this.lazyInit = lazyInit;
	}
}