package com.YaNan.frame.plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
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

import com.YaNan.frame.plugin.annotations.Register;
import com.YaNan.frame.plugin.annotations.Service;
import com.YaNan.frame.plugin.beans.BeanContainer;
import com.YaNan.frame.plugin.handler.InvokeHandler;
import com.YaNan.frame.plugin.handler.PlugsHandler;
import com.YaNan.frame.plugin.interfacer.PlugsListener;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.impl.SimpleConfigObject;

import com.YaNan.frame.utils.reflect.ClassLoader;
import com.YaNan.frame.utils.reflect.cache.ClassInfoCache;
import com.YaNan.frame.utils.resource.PackageScanner;
import com.YaNan.frame.utils.resource.PackageScanner.ClassInter;
import com.YaNan.frame.utils.resource.Path;
import com.YaNan.frame.utils.resource.Path.PathInter;
import com.YaNan.frame.utils.resource.ResourceManager;
import com.YaNan.frame.utils.resource.ResourceNotFoundException;

/**
 * Plugin factory,initial Plugin Context and manager & register & get Plugin Instance 2018 7-27
 * <p>modify MethodHandler initial when RegisterDescription was initial,cut down proxy execution time 2018 7-12
 * <p>add more than method intercept , update intercept rules ,add intercept handler set</br>
 * 
 * @author yanan
 *
 */
public class PlugsFactory {
	/**
	 * Stream type
	 * <p>support stream as Class,Hoconf or Comps
	 * @author yanan
	 *
	 */
	public static enum STREAM_TYPT {
		CLASS,CONF,COMP
	}
	//Plugin instance is signal
	private static volatile PlugsFactory instance;
	//plugin context is available
	private boolean available = false;
	//Plugin context loaded configure file list
	private List<File> configureLocation;
	//Plugin context scan package path
	private String[] packageDirs;
	//Plugin context plug pools
	private Map<Class<?>, Plug> plugsList = new HashMap<Class<?>, Plug>();
	//Plugin context Register pools
	private Map<Class<?>, RegisterDescription> RegisterContatiner = new HashMap<Class<?>, RegisterDescription>();
	private volatile List<RegisterDescription> registerList = new LinkedList<RegisterDescription>();
	private Map<Object, RegisterDescription> objectRegister = new HashMap<Object, RegisterDescription>();
	private Map<String, RegisterDescription> objectIdRegister = new HashMap<String, RegisterDescription>();
	/**
	 * get plugin context package scan paths
	 * @return package path 
	 */
	public String[] getScanPath() {
		return packageDirs == null?null:Arrays.copyOf(packageDirs, packageDirs.length);
	}
	public void addScanPath(String... paths) {
		if(paths.length>0) {
			if(packageDirs==null) {
				packageDirs =paths;
			}else {
				String[] pathCache = new String[paths.length+packageDirs.length];
				System.arraycopy(packageDirs, 0, pathCache, 0, packageDirs.length);
				System.arraycopy(paths, 0, pathCache, packageDirs.length, paths.length);
				this.packageDirs = pathCache;
			}
		}
	}
	public void addScanPath(Class<?>... clzzs) {
		addScanPath(ResourceManager.getClassPath(clzzs));
	}
	/**
	 * add a instance as bean
	 * @param object bean instance
	 * @param description bean register description info
	 */
	public static void addBeanRegister(Object object, RegisterDescription description) {
		instance.objectRegister.put(object, description);
		instance.objectIdRegister.put(description.getConfig().getString("id"), description);
	}
	/**
	 * get bean(instance)'s register description info
	 * @param object bean instance
	 * @return RegisterDescription info
	 */
	public static RegisterDescription getBeanRegister(Object object) {
		return instance.objectRegister.get(object);
	}
	
	public static RegisterDescription getBeanRegister(String beanId) {
		return instance.objectIdRegister.get(beanId);
	}

	public void addRegisterHandlerQueue(RegisterDescription registerDescription) {
		synchronized (registerList) {
			if(!registerList.contains(registerDescription))
				this.registerList.add(registerDescription);
		}
		this.initRegisterDescriptionHandler();
	}

	private void initRegisterDescriptionHandler() {
		if (this.available && !this.registerList.isEmpty()) {
			synchronized (registerList) {
				Iterator<RegisterDescription> registerDesIterator = registerList.iterator();
				while (registerDesIterator.hasNext()) {
					RegisterDescription registerDescription = registerDesIterator.next();
					registerDescription.initHandler();
					registerDesIterator.remove();
				}
			}
		}
	}

	// registerClass >> registerInstance
	// Register >> Service array
	/**
	 * plugin initial method 
	 * @param resources initial configure file path 
	 */
	public static void init(String... resources) {
		if(instance==null){
			synchronized (PlugsFactory.class) {
				if(instance==null){
					PlugsFactory instance = getInstance();
					if(resources==null||resources.length==0){
						try {
							List<File> file = ResourceManager.getResource(ResourceManager.classPath()+File.separatorChar+"plugin.conf");
							if(file!=null&&file.size()>0)
							instance.configureLocation.add(file.get(0));
						}catch (ResourceNotFoundException r) {
						}
					}
				}
			}
		}
		for (String res : resources) {
			List<File> resourceFiles = ResourceManager.getResource(res);
			for (File file : resourceFiles)
				instance.configureLocation.add(file);
		}
		instance.init0();
	}

	/**
	 * get all register pools 
	 * @return a map container register class with description
	 */
	public Map<Class<?>, RegisterDescription> getAllRegister() {
		return RegisterContatiner;
	}

	/**
	 * get all service interface map
	 * 
	 * @return a map contain all service ,the key is service class
	 */
	public Map<Class<?>, Plug> getAllPlugs() {
		return plugsList;
	}
	/**
	 * get PlugsFactory instance
	 * <p> the PlugsFacotry type is a sign instance
	 * @return a instance
	 */
	public static PlugsFactory getInstance() {
		if (instance == null) {
			synchronized (PlugsFactory.class) {
				if (instance == null)
					synchronized (PlugsFactory.class) {
						instance = new PlugsFactory();
						instance.configureLocation = new ArrayList<File>();
					}
			}
		}
		return instance;
	}
	/**
	 * initial plug context ,need use {@link #associate()} to associate all service
	 * with register when plug context scan all class
	 * 
	 */
	public void init0() {
		PlugsInitLock.tryLock();
		this.addPlugsByDefault(PlugsListener.class);// 添加两个Plugin自身支持需要的组件接口
													// PlugsListener
													// 用于组件初始化完成时的监听
		this.addPlugsByDefault(InvokeHandler.class);// InvokeHandler用于提供方法拦截接口的支持
		// 判断资源文件是否存在,如果无资源文件，直接扫描所有的plugs文件
		if (this.configureLocation.isEmpty()) {
			// 获取基础的配置文件
			File baseFile = new File(this.getClass().getClassLoader().getResource("").getPath().replace("%20", " "),
					"plugin.conf");
			// 如果文件不存在，扫描所有的文件
			if (!baseFile.exists()) {
				Path path = new Path(this.getClass().getClassLoader().getResource("").getPath().replace("%20", " "));
				path.filter("**.plugs", "**.comps", "**.conf");
				path.scanner(new PathInter() {
					@Override
					public void find(File file) {
						addPlugs(file);
					}
				});
			} else {// 否则加入文件
				addPlugs(baseFile);
			}
		} else {
			for (File file : this.configureLocation) {
				addPlugs(file);
			}
		}
		Config conf = ConfigContext.getInstance().getGlobalConfig();
		if(conf!=null) {
			conf = conf.getConfig("Plugin");
		}
		if (conf == null) {
			if(this.packageDirs==null)
				this.addScanPath(".");
		} else {
			conf.allowKeyNull();
			if (conf.hasPath("ScanPackage")) {
				if (conf.isList("ScanPackage")) {
					List<String> dirs = conf.getStringList("ScanPackage");
					if (dirs.size() > 1) {
						for (int i = 0; i < dirs.size() - 1; i++) {
							for (int j = i + 1; j < dirs.size(); j++) {
								if (dirs.get(i) != null && dirs.get(j) != null) {
									if (dirs.get(i).startsWith(dirs.get(j))) {
										dirs.set(i, null);
									} else if (dirs.get(j).startsWith(dirs.get(i))) {
										dirs.set(j, null);
									}
								}
							}
						}
					}
					packageDirs = dirs.toArray(new String[dirs.size()]);
				} else {
					String confDirs = conf.getString("ScanPackage", ".");
					confDirs = confDirs.replace("*", "");
					String[] dirs = confDirs.split(",");
					if (dirs.length > 1) {
						for (int i = 0; i < dirs.length - 1; i++) {
							for (int j = i + 1; j < dirs.length; j++) {
								if (dirs[i] != null && dirs[j] != null) {
									if (dirs[i].startsWith(dirs[j])) {
										dirs[i] = null;
									} else if (dirs[j].startsWith(dirs[i])) {
										dirs[j] = null;
									}
								}
							}
						}
					}
					this.addScanPath(dirs);
				}
			} else {
				if(this.packageDirs==null)
					this.addScanPath(".");
			}
			if (conf.hasPath("includes")) {
				if (conf.isList("includes")) {
					List<String> dirs = conf.getStringList("includes");
					for (String dir : dirs) {
						List<File> resource = ResourceManager.getResource(dir);
						for (File file : resource) {
							addPlugs(file);
						}
					}
				} else {
					String confDirs = conf.getString("includes");
					String[] dirs = confDirs.split(",");
					for (String dir : dirs) {
						List<File> resource = ResourceManager.getResource(dir);
						for (File file : resource) {
							addPlugs(file);
						}
					}
				}
			}
		}

		// 扫描类
			PackageScanner scanner = new PackageScanner();
			scanner.setScanPath(packageDirs);
			scanner.doScanner(new ClassInter() {
				@Override
				public void find(Class<?> cls) {
					addPlugs(cls);
				}
			});
		this.associate();
		available = true;
		this.initRegisterDescriptionHandler();
		this.inited();
		PlugsInitLock.unLock();
	}
	public void rebuild() {
		PlugsInitLock.checkLock();
		PlugsInitLock.tryLock();
		this.associate();
		this.initRegisterDescriptionHandler();
		this.inited();
		PlugsInitLock.unLock();
	}
	private void inited() {
		List<PlugsListener> listeners = PlugsFactory.getPlugsInstanceList(PlugsListener.class);
		for (PlugsListener listen : listeners)
			listen.execute(this);
	}

	/**
	 * 建立个组件和注册器之间的关联
	 */
	public void associate() {
		Iterator<RegisterDescription> iterator = RegisterContatiner.values().iterator();
		while (iterator.hasNext()) {
			RegisterDescription registerDescription = iterator.next();
			Class<?>[] plugs = registerDescription.getPlugs();
			if (plugs != null)
				for (Class<?> plugInterface : plugs) {
					Plug plug = this.plugsList.get(plugInterface);
					if (plug == null) {
						this.addPlugsService(plugInterface);
						plug = this.plugsList.get(plugInterface);
						if (plug == null)
							try {
								throw new Exception("register " + registerDescription.getRegisterClass().getName()
										+ " implements " + plugInterface.getName() + " not exists ");
							} catch (Exception e) {
								throw new PluginInitException(e);
							}
					}
					plug.addRegister(registerDescription);
				}
		}
	}

	/**
	 * 
	 * @param plugClass
	 *            组件类 即接口
	 * @return
	 */
	public static Plug getPlug(Class<?> plugClass) {
		return instance.plugsList.get(plugClass);
	}

	/**
	 * 添加组件 当通过扫描comps文件与plugs文件时，需要通过此方法将组件添加到容器中
	 * 
	 * @param file
	 *            组件描述文件
	 */
	public void addPlugs(File file) {
		String fileName = file.getName();
		String type = fileName.substring(fileName.lastIndexOf("."));
		try {
			if (type.equals(".plugs")) {
				PlugsDescription plugsDescription = new PlugsDescription(file);
				Plug plug = new Plug(plugsDescription);
				this.plugsList.put(plugsDescription.getPlugClass(), plug);
			}
			if (type.equals(".comps")) {
				RegisterDescription registerDescription = new RegisterDescription(file);
				RegisterContatiner.put(registerDescription.getRegisterClass(), registerDescription);
			}
			if (type.equals(".conf")) {
				Config config = ConfigFactory.parseFile(file);
				ConfigContext.getInstance().mergeConfig(config);
				config.allowKeyNull(true);
				List<? extends Object> list = config.getValueListUnwrapper("plugins");
				addPlugsByConfigList(list);
			}
		} catch (Exception e) {
			throw new PluginRuntimeException("failed to add plug at file " + file, e);
		}
	}
	/**
	 * 添加组件通过配置列表
	 * @param list
	 */
	public void addPlugsByConfigList(List<? extends Object> list) {
		for (Object conf : list) {
			addPlugByConfig(conf);
		}
	}
	/**
	 * 添加组件通过配置
	 * @param conf
	 */
	public void addPlugByConfig(Object conf) {
		if (conf == null)
			throw new PluginInitException("conf is null");
		try {
			if (conf.getClass().equals(String.class)) {
				Class<?> clzz = Class.forName((String) conf);
				if (clzz.isInterface()) {
					this.addPlugsService(clzz);
				} else
					this.addPlugsAuto(clzz);
			} else if (conf.getClass().equals(SimpleConfigObject.class)) {
				RegisterDescription registerDescription = new RegisterDescription(
						((SimpleConfigObject) conf).toConfig());
				RegisterContatiner.put(registerDescription.getRegisterClass(), registerDescription);
			}
		} catch (Exception e) {
			throw new PluginInitException("failed to add plug at conf  " + conf, e);
		}
	}
	/**
	 * add InputStream as plug
	 * 
	 * @param stream data stream
	 * @param type data type
	 * @param className if is class ,type class name ,other null.
	 */
	public void addPlugs(InputStream stream,STREAM_TYPT type,String className) {
		try {
			if(!isAvailable())
				this.init0();
			if (type==STREAM_TYPT.CLASS) {
				//read bytes and close stream
				byte[] temp = new byte[stream.available()];
				stream.read(temp,0,temp.length);
				stream.close();
				//load class
				Class<?> clzz =new com.YaNan.frame.utils.reflect.ClassLoader().loadClass(className,temp);
				//Register
				PlugsDescription plugsDescription = new PlugsDescription(clzz);
				Plug plug = new Plug(plugsDescription);
				this.plugsList.put(plugsDescription.getPlugClass(), plug);
			}
			if (type==STREAM_TYPT.CONF) {
				InputStreamReader reader = new InputStreamReader(stream);
				Config config = ConfigFactory.parseReader(reader);
				ConfigContext.getInstance().mergeConfig(config);
				config.allowKeyNull(true);
				List<? extends Object> list = config.getValueListUnwrapper("plugins");
				addPlugsByConfigList(list);
			}
		} catch (Exception e) {
			throw new PluginRuntimeException("parse conf failed", e);
		}finally{
			rebuild();
		}
	}
	/**
	 * 添加组件 当通过扫描类文件（注解）方式时，需要通过此方法将组件添加到容器中
	 * 
	 * @param cls
	 */
	public void addPlugs(Class<?> cls) {
		Service service = cls.getAnnotation(Service.class);
		Register register = cls.getAnnotation(Register.class);
		if (service != null) {// 如果是Service
			PlugsDescription plugsDescrption = new PlugsDescription(service, cls);
			Plug plug = new Plug(plugsDescrption);
			this.plugsList.put(cls, plug);
		}
		if (register != null) {
			try {
				RegisterDescription registerDescription = new RegisterDescription(register, cls);
				RegisterContatiner.put(cls, registerDescription);
			} catch (Throwable e) {
				throw e;
			}
		}
	}

	public void addPlugsAuto(Class<?> cls) {
		Service service = cls.getAnnotation(Service.class);
		Register register = cls.getAnnotation(Register.class);
		if (service != null || register != null) {
			if (service != null) {// 如果是Service
				PlugsDescription plugsDescrption = new PlugsDescription(service, cls);
				Plug plug = new Plug(plugsDescrption);
				this.plugsList.put(cls, plug);
			}
			if (register != null) {
				try {
					RegisterDescription registerDescription = new RegisterDescription(register, cls);
					RegisterContatiner.put(cls, registerDescription);
				} catch (Throwable e) {
					throw e;
				}
			}
		} else if (cls.isInterface()) {
			PlugsDescription plugsDescrption = new PlugsDescription(service, cls);
			Plug plug = new Plug(plugsDescrption);
			this.plugsList.put(cls, plug);
		} else {
			try {
				RegisterDescription registerDescription = new RegisterDescription(cls);
				RegisterContatiner.put(cls, registerDescription);
			} catch (Throwable e) {
				throw e;
			}
		}

	}

	public void addPlugsService(Class<?> cls) {
		Service service = cls.getAnnotation(Service.class);
		PlugsDescription plugsDescrption = new PlugsDescription(service, cls);
		Plug plug = new Plug(plugsDescrption);
		this.plugsList.put(cls, plug);
	}

	public void addPlugsRegister(Class<?> cls) {
		Register register = cls.getAnnotation(Register.class);
		try {
			RegisterDescription registerDescription = new RegisterDescription(register, cls);
			RegisterContatiner.put(cls, registerDescription);
		} catch (Throwable e) {
			throw e;
		}
	}

	/**
	 * 通过默认的方式添加组件
	 * 
	 * @param plugClass
	 */
	public void addPlugsByDefault(Class<?> plugClass) {
		PlugsDescription plugsDescrption = new PlugsDescription(plugClass);
		Plug plug = new Plug(plugsDescrption);
		this.plugsList.put(plugClass, plug);
	}

	/**
	 * 获取组件实例，当组件不存在时返回传入的组件实现类的简单组件服务 如果获取组件时服务未完全初始化，则不会对其进行拦截
	 * 
	 * @param impl
	 *            组件接口类
	 * @param defaultClass
	 *            默认实现类
	 * @param args
	 *            组件参数
	 * @return 代理对象
	 */
	public static <T> T getPlugsInstanceWithDefault(Class<T> impl, Class<? extends T> defaultClass, Object... args) {
		try {
			RegisterDescription registerDescription = getRegisterDescrptionAllowNull(impl);
			if (registerDescription == null)
				registerDescription = getRegisterDescrption(defaultClass);
			return registerDescription.getRegisterInstance(impl, args);// instance.getRegisterInstance(impl,registerDescription,args);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 检测服务是否可用
	 * 
	 * @return
	 * @throws Exception
	 */
	public static boolean checkAvaliable(){
		if (instance == null) {
			PlugsFactory.init();
			if (instance == null)
				throw new PluginRuntimeException("YaNan.plugs service not initd");
		}
		if (!instance.isAvailable())
			instance.init0();
		if (!instance.isAvailable())
			throw new PluginRuntimeException(
					"plugs unavailable ! this error may arise because a static field uses the PlugsFactory's proxy");
		return instance != null && instance.isAvailable();
	}

	/**
	 * 检测服务是否可用
	 * 
	 * @return
	 * @throws Exception
	 */
	public static boolean checkAvaliableNE() {
		return instance != null && instance.isAvailable();
	}

	public static List<RegisterDescription> getRegisterList(Class<?> plugsClass) {
		Plug plug = instance.plugsList.get(plugsClass);
		if (plug == null)
			throw new PluginRuntimeException("could found plug for " + plugsClass.getName());
		return plug.getRegisterDescriptionList();
	}

	/**
	 * 获取注册描述器
	 * 
	 * @param impl
	 * @param b
	 * @param attribute
	 * @param attribute
	 * @return
	 * @throws Exception
	 */
	public static RegisterDescription getRegisterDescrption(Class<?> impl) throws Exception {
		RegisterDescription registerDescription = null;
		checkAvaliable();
		if (impl.isInterface() || impl.getAnnotation(Service.class)!= null) {
			Plug plug = getPlug(impl);
			if (plug == null)
				throw new PlugNotFound("service interface " + impl.getName() + " could not found or not be regist");
			registerDescription = plug.getDefaultRegisterDescription();
		} else {
			registerDescription = instance.getRegisterDescription(impl);
		}
		return registerDescription;
	}

	public static RegisterDescription getRegisterDescrptionAllowNull(Class<?> impl) {
		RegisterDescription registerDescription = null;
		try {
			if (impl.isInterface()) {
				if (checkAvaliableNE()) {
					Plug plug = getPlug(impl);
					if (plug != null)
						registerDescription = plug.getDefaultRegisterDescription();
				}
			} else {
				registerDescription = instance.getRegisterDescription(impl);
			}
		} catch (Exception e) {

		}
		return registerDescription;
	}

	public static RegisterDescription getRegisterDescrption(Class<?> impl, String attribute, boolean strict)
			throws Exception {
		RegisterDescription registerDescription = null;
		if (impl.isInterface() && checkAvaliable()) {
			Plug plug = getPlug(impl);
			if (plug == null)
				throw new PlugNotFound("service interface " + impl.getName() + " could not found or not be regist");
			if (strict)
				registerDescription = plug.getRegisterDescriptionByAttributeStrict(attribute);
			else
				registerDescription = plug.getRegisterDescriptionByAttribute(attribute);
		} else {
			registerDescription = instance.getRegisterDescription(impl);
		}
		return registerDescription;
	}

	public static <T> RegisterDescription getRegisterDescrption(Class<T> impl, Class<?> insClass)
			throws Exception {
		RegisterDescription registerDescription = null;
		if (impl.isInterface() && checkAvaliable()) {
			Plug plug = getPlug(impl);
			if (plug == null)
				throw new PlugNotFound("service interface " + impl.getName() + " could not found or not be regist");
			registerDescription = plug.getRegisterDescriptionByInsClass(insClass);
		} else {
			registerDescription = instance.getRegisterDescription(impl);
		}
		return registerDescription;
	}

	/**
	 * 获取组件实例，当组件中有多个组件实现实例时，返回一个默认组件
	 * 具体选择某个组件实例作为默认组件实例依赖其优先级(priority)，当所有优先级相同时选第一个 优先级数值越低，优先级越高
	 * 
	 * @param impl
	 * @param args
	 * @return
	 */
	public static <T> T getPlugsInstance(Class<T> impl, Object... args) {
		try {
			// 获取一个注册描述
			RegisterDescription registerDescription = getRegisterDescrption(impl);
			if (registerDescription == null)
				throw new RegisterNotFound("service interface " + impl.getName() + " could not found any register");
			return registerDescription.getRegisterInstance(impl, args);
		} catch (Exception e) {
			throw new PluginRuntimeException("failed to get plugin instance at plugin class " + impl, e);
		}
	}

	/**
	 * 获取组件实例，当组件中有多个组件实现实例时，返回一个默认组件，当组件实例为空时，返回null，并不抛出异常
	 * 具体选择某个组件实例作为默认组件实例依赖其优先级(priority)，当所有优先级相同时选第一个 优先级数值越低，优先级越高
	 * 
	 * @param impl
	 * @param args
	 * @return
	 */
	public static <T> T getPlugsInstanceAllowNull(Class<T> impl, Object... args) {
		try {
			// 获取一个注册描述
			RegisterDescription registerDescription = getRegisterDescrption(impl);
			if (registerDescription != null)
				return registerDescription.getRegisterInstance(impl, args);
		} catch (Exception e) {
			throw new PluginRuntimeException("failed to get plugin instance at plugin class " + impl, e);
		}
		return null;
	}

	public static <T> T getPlugsInstanceNew(Class<T> impl, Object... args) {
		try {
			// 获取一个注册描述
			RegisterDescription registerDescription = getRegisterDescrption(impl);
			if (registerDescription == null)
				throw new RegisterNotFound("service interface " + impl.getName() + " could not found any register");
			return registerDescription.getRegisterNewInstance(impl, args);
		} catch (Exception e) {
			throw new PluginRuntimeException("failed to get plugin instance at plugin class " + impl, e);
		}
	}

	/**
	 * 获取组件实例，当组件中有多个组件实现实例时，返回一个默认组件
	 * 具体选择某个组件实例作为默认组件实例依赖其优先级(priority)，当所有优先级相同时选第一个 优先级数值越低，优先级越高
	 * 
	 * @param impl
	 * @param args
	 * @return
	 */
	public static <T> T getPlugsInstanceByInsClass(Class<T> impl, Class<?> insClass, Object... args) {
		try {

			RegisterDescription registerDescription = getRegisterDescrption(impl, insClass);
			if (registerDescription == null)
				throw new RegisterNotFound("service interface " + impl.getName() + " could not found any register");
			return registerDescription.getRegisterInstance(impl, args);
		} catch (Exception e) {
			throw new PluginRuntimeException("failed to get plugin instance at plugin class " + impl, e);
		}
	}

	/**
	 * 通过组件实例的属性（attribute）获取组件实例，当组件中有多个组件实例与之匹配时，返回一个优先级组件
	 * 如果没有匹配的组件，返回一个默认组件，因此这是一种不严谨的组件获取方式，如果需要使用严谨模式（当 匹配值不通过时，返回null），需要使用方法
	 * {@link #getPlugsInstanceByAttributeStrict()}
	 * 具体选择某个组件实例作为返回组件实例依赖其优先级，当所有优先级相同时选第一个 优先级数值越低，优先级越高
	 * 
	 * @param impl
	 * @param args
	 * @return
	 */
	public static <T> T getPlugsInstanceByAttribute(Class<T> impl, String attribute, Object... args) {
		try {

			RegisterDescription registerDescription = getRegisterDescrption(impl, attribute, false);
			if (registerDescription == null)
				throw new RegisterNotFound("service interface " + impl.getName() + " could not found any register");
			return registerDescription.getRegisterInstance(impl, args);// instance.getRegisterInstance(impl,registerDescription,args);
		} catch (Exception e) {
			throw new PluginRuntimeException("failed to get plugin instance at plugin class " + impl, e);
		}
	}

	/**
	 * 通过组件实例的属性（attribute）获取组件实例，当组件中有多个组件实例与之匹配时，返回一个优先级组件
	 * 如果没有匹配的组件，会返回null，因此这是一种不严谨的组件获取方式，如果想当匹配不通过时，返回一个 默认组件，需要使用方法
	 * {@link #getPlugsInstanceByAttribute())
	 * 具体选择某个组件实例作为返回组件实例依赖其优先级，当所有优先级相同时选第一个 优先级数值越低，优先级越高
	 * 
	 * @param impl
	 * @param args
	 * @return
	 */
	public static <T> T getPlugsInstanceByAttributeStrict(Class<T> impl, String attribute, Object... args) {
		try {
			RegisterDescription registerDescription = getRegisterDescrption(impl, attribute, true);
			if (registerDescription != null)
				return registerDescription.getRegisterInstance(impl, args);// instance.getRegisterInstance(impl,registerDescription,args);
		} catch (Throwable e) {
			throw new PluginRuntimeException("failed to get plugin instance at plugin class \"" + impl
					+ "\" where attribute is \"" + attribute + "\"", e);
		}
		return null;
	}

	/**
	 * 获取组件的实例列表 返回的list按其优先级排序 优先级数值越低，优先级越高
	 * 
	 * @param impl
	 * @param args
	 * @return
	 */
	public static <T> List<T> getPlugsInstanceListByAttribute(Class<T> impl, String attribute, Object... args) {
		try {
			if (instance == null)
				throw new Exception("YaNan.plugs service not initd");
			if (!instance.isAvailable())
				throw new Exception(
						"plugs unavailable ! this error may arise because a static field uses the PlugsFactory's proxy");
			Plug plug = getPlug(impl);
			if (plug == null)
				throw new PlugNotFound("service interface " + impl.getName() + " could not found or not be regist");
			List<T> objectList = new ArrayList<T>();
			List<RegisterDescription> registerDescriptionList = plug.getRegisterDescriptionListByAttribute(attribute);
			Iterator<RegisterDescription> iterator = registerDescriptionList.iterator();
			while (iterator.hasNext())
				objectList.add(iterator.next().getRegisterInstance(impl, args));
			return objectList;
		} catch (Exception e) {
			throw new PluginRuntimeException("failed to get plugin instance at plugin class " + impl, e);
		}
	}

	/**
	 * 获取组件的实例列表 返回的list按其优先级排序 优先级数值越低，优先级越高
	 * 
	 * @param impl
	 * @param args
	 * @return
	 */
	public static <T> List<T> getPlugsInstanceList(Class<T> impl, Object... args) {
		try {
			if (instance == null)
				throw new Exception("YaNan.plugs service not initd");
			if (!instance.isAvailable()) 
				throw new Exception(
						"plugs unavailable ! this error may arise because a static field uses the PlugsFactory's proxy");
			Plug plug = getPlug(impl);
			if (plug == null)
				throw new PlugNotFound("service interface " + impl.getName() + " could not found or not be regist");
			List<T> objectList = new ArrayList<T>();
			List<RegisterDescription> registerDescriptionList = plug.getRegisterDescriptionList();
			Iterator<RegisterDescription> iterator = registerDescriptionList.iterator();
			while (iterator.hasNext())
				objectList.add(iterator.next().getRegisterInstance(impl, args));
			return objectList;
		} catch (Exception e) {
			throw new PluginRuntimeException("failed to get plugin instance at plugin class " + impl, e);
		}
	}

	/**
	 * 通过实例类返回实例描述信息
	 * 
	 * @param targetCls
	 * @return
	 */
	public RegisterDescription getRegisterDescription(Class<?> targetCls) {
		if (this.RegisterContatiner.get(targetCls) == null) {
			RegisterDescription registerDescription = new RegisterDescription(targetCls);
			RegisterContatiner.put(targetCls, registerDescription);
		}
		return this.RegisterContatiner.get(targetCls);
	}

	public void clear() {
		this.RegisterContatiner.clear();
		this.plugsList.clear();
	}

	public boolean isAvailable() {
		return available;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}

	@SuppressWarnings("unchecked")
	public static Map<Class<Annotation>, List<Annotation>> getAnnotationGroup(Parameter parameter,
			Class<Annotation>... annoTypes) {
		if (parameter == null)
			return null;
		Annotation[] annotations = parameter.getAnnotations();
		if (annotations.length == 0)
			return null;
		Map<Class<Annotation>, List<Annotation>> annoGroup = new HashMap<Class<Annotation>, List<Annotation>>();
		if (annoTypes.length == 0)// 没有指定注解参数时，返回空，因为没有意义
			return null;
		// 遍历所有注解，进行分组添加
		for (Annotation annotation : annotations) {
			for (Class<Annotation> annoType : annoTypes) {
				Annotation anno = annotation.annotationType().getAnnotation(annoType);
				if (anno != null) {
					List<Annotation> list = annoGroup.get(annoType);
					if (list == null) {
						list = new LinkedList<Annotation>();
						list.add(anno);
						annoGroup.put(annoType, list);
					} else
						list.add(anno);
				}
			}
		}
		return annoGroup;
	}

	public static Map<Class<Annotation>, List<Annotation>> getAnnotationGroup(Parameter parameter,
			List<Class<Annotation>> annoTypes) {
		if (parameter == null)
			return null;
		Annotation[] annotations = parameter.getAnnotations();
		if (annotations.length == 0)
			return null;
		Map<Class<Annotation>, List<Annotation>> annoGroup = new HashMap<Class<Annotation>, List<Annotation>>();
		if (annoTypes.size() == 0)// 没有指定注解参数时，返回空，因为没有意义
			return null;
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
					} else
						list.add(annotation);
				}
			}
		}
		return annoGroup;
	}

	@SuppressWarnings("unchecked")
	public static Map<Class<Annotation>, List<Annotation>> getAnnotationGroup(Method method,
			Class<Annotation>... annoTypes) {
		if (method == null)
			return null;
		Annotation[] annotations = method.getAnnotations();
		if (annotations.length == 0)
			return null;
		Map<Class<Annotation>, List<Annotation>> annoGroup = new HashMap<Class<Annotation>, List<Annotation>>();
		if (annoTypes.length == 0)// 没有指定注解参数时，返回空，因为没有意义
			return null;
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
					} else
						list.add(annotation);
				}
			}
		}
		return annoGroup;
	}

	public static Map<Class<Annotation>, List<Annotation>> getAnnotationGroup(Method method,
			List<Class<Annotation>> annoTypes) {
		if (method == null)
			return null;
		Annotation[] annotations = method.getAnnotations();
		if (annotations.length == 0)
			return null;
		Map<Class<Annotation>, List<Annotation>> annoGroup = new HashMap<Class<Annotation>, List<Annotation>>();
		if (annoTypes.size() == 0)// 没有指定注解参数时，返回空，因为没有意义
			return null;
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
					} else
						list.add(annotation);
				}
			}
		}
		return annoGroup;
	}

	public static Map<Class<Annotation>, List<Annotation>> getAnnotationGroup(Class<?> clzz,
			List<Class<Annotation>> annoTypes) {
		if (clzz == null)
			return null;
		Annotation[] annotations = clzz.getAnnotations();
		if (annotations.length == 0)
			return null;
		Map<Class<Annotation>, List<Annotation>> annoGroup = new HashMap<Class<Annotation>, List<Annotation>>();
		if (annoTypes.size() == 0)// 没有指定注解参数时，返回空，因为没有意义
			return null;
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
					} else
						list.add(annotation);
				}
			}
		}
		return annoGroup;
	}

	public static List<Annotation> getAnnotationGroup(Field field, Class<? extends Annotation> annotationType) {
		if (field == null || annotationType == null)
			return null;
		Annotation[] annotations = field.getAnnotations();
		if (annotations.length == 0)
			return null;
		List<Annotation> annoGroup = new LinkedList<Annotation>();
		for (Annotation annotation : annotations) {
			Annotation annoMark = annotation.annotationType().getAnnotation(annotationType);
			if (annoMark != null)
				annoGroup.add(annotation);
		}
		return annoGroup;
	}
	public static List<Annotation> getAnnotationGroup(Parameter parameter, Class<? extends Annotation> annotationType) {
		if (parameter == null || annotationType == null)
			return null;
		Annotation[] annotations = parameter.getAnnotations();
		if (annotations.length == 0)
			return null;
		List<Annotation> annoGroup = new LinkedList<Annotation>();
		for (Annotation annotation : annotations) {
			Annotation annoMark = annotation.annotationType().getAnnotation(annotationType);
			if (annoMark != null)
				annoGroup.add(annotation);
		}
		return annoGroup;
	}
	
	/**
	 * 获取代理对象的PlugsHandler对象
	 * 
	 * @param proxyInstance
	 * @return
	 */
	public static PlugsHandler getPlugsHandler(Object proxyInstance) {
		Field field = ClassInfoCache.getClassHelper(proxyInstance.getClass()).getAnyField("h");
		if (field == null)
			field = ClassInfoCache.getClassHelper(proxyInstance.getClass()).getDeclaredField("CGLIB$CALLBACK_0");
		if (field == null)
			throw new PluginRuntimeException("instance \"" + proxyInstance + "\" is not proxy object");
		PlugsHandler plugsHandler = null;
		try {
			plugsHandler = ClassLoader.getFieldValue(field, proxyInstance);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new PluginRuntimeException("failed to get instance's PlugsHandler", e);
		}
		return plugsHandler;
	}

	public static <T> T getBean(String beanId) {
		T t = BeanContainer.getContext().getBean(beanId);
		if (t == null)
			throw new PluginRuntimeException("colud not find bean defined id is \"" + beanId + "\"");
		return t;
	}

	public static <T> T getBean(Class<T> beanClass) {
		T t = BeanContainer.getContext().getBean(beanClass);
		if (t == null)
			t = getPlugsInstance(beanClass);
		if (t == null)
			throw new PluginRuntimeException("colud not find bean defined class is \"" + beanClass.getName() + "\"");
		return t;
	}

	public static <T> T getPlugsInstanceNewByParamType(Class<T> impl, Class<?>[] parameterType, Object... arguments) {
		try {
			// 获取一个注册描述
			RegisterDescription registerDescription = getRegisterDescrption(impl);
			if (registerDescription == null)
				throw new RegisterNotFound("service interface " + impl.getName() + " could not found any register");
			return registerDescription.getRegisterNewInstanceByParamType(impl, parameterType, arguments);
		} catch (Exception e) {
			throw new PluginRuntimeException("failed to get plugin instance at plugin class " + impl, e);
		}
	}

	public static <T> T getPlugsInstanceByParamType(Class<T> impl, Class<?>[] parameterType, Object... arguments) {
		try {
			// 获取一个注册描述
			RegisterDescription registerDescription = getRegisterDescrption(impl);
			if (registerDescription == null)
				throw new RegisterNotFound("service interface " + impl.getName() + " could not found any register");
			return registerDescription.getRegisterInstanceByParamType(impl, parameterType, arguments);
		} catch (Exception e) {
			throw new PluginRuntimeException("failed to get plugin instance at plugin class " + impl, e);
		}
	}

	public List<File> getConfigureLocation() {
		return configureLocation;
	}
}
