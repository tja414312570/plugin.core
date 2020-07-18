package com.YaNan.frame.plugin.hot;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.YaNan.frame.plugin.ConfigContext;
import com.YaNan.frame.plugin.PlugsFactory;
import com.YaNan.frame.plugin.annotations.Register;
import com.YaNan.frame.plugin.annotations.Service;
import com.YaNan.frame.plugin.definition.RegisterDefinition;
import com.YaNan.frame.plugin.interfacer.PlugsListener;
import com.YaNan.frame.utils.reflect.AppClassLoader;
import com.YaNan.frame.utils.reflect.cache.ClassHelper;
import com.YaNan.frame.utils.resource.FileUtils;
import com.YaNan.frame.utils.resource.Path;
import com.YaNan.frame.utils.resource.Path.PathInter;
import com.typesafe.config.Config;

/**
 * 类更新监控
 * 
 * @author yanan
 */
public class ClassHotUpdater implements Runnable, PlugsListener {
	public List<ContextPath> contextPathList = new ArrayList<ContextPath>();//扫描上下文路劲
	private Map<String,String> classNameCache = new HashMap<>();
	public Logger log = LoggerFactory.getLogger(ClassHotUpdater.class);
	private boolean cache;// 是否已经扫描
	private List<String> scanner = new ArrayList<String>(100);// 用于存储已经扫描到的文件
	static Map<String, FileToken> fileToken = new HashMap<String, FileToken>(100);// 用于存文件hash
	static Map<Object, Object> proxy = new HashMap<Object, Object>();// 用于存储映射关系
	Map<String, String> nameMappings = new HashMap<String, String>();// 用于存储已改变的类的与映射名的关系

	/**
	 * 用于获取代理类
	 * 
	 * @param origin
	 * @return
	 */
	public static Class<?> getProxyClass(Class<?> origin) {
		return (Class<?>) proxy.get(origin);
	}

	/**
	 * 文件hash
	 * 
	 * @param file
	 * @return
	 */
	public static long hash(File file, byte[] bytes) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			md.update(bytes, 0, bytes.length);
			BigInteger bigInt = new BigInteger(1, md.digest());
			long h = bigInt.longValue();
			return h<0l?~h+1:h;
		} catch (NoSuchAlgorithmException e) {
			return file.hashCode() + file.lastModified();
		}
	}

	@Override
	public void run() {
		final ContextPath currentContextPath = new ContextPath(null, null);
		PathInter filePathInter = new PathInter() {
			@Override
			public void find(File file) {
				long lastModif = file.lastModified();
				String fileMark = file.getAbsolutePath();
				if (!cache) {// 如果是第一次扫描
					scanner.add(fileMark);
					fileToken.put(fileMark,new FileToken(hash(file, FileUtils.getBytes(file)),lastModif));
				} else {// 其它扫描
					//将扫描的文件推送到已扫描队列
					scanner.add(fileMark);
					//获取类名
					String clzzName = getClassName(file.getAbsolutePath(),currentContextPath.getContextPath());
					try {
						Class<?> clzz = Class.forName(clzzName);
						java.lang.ClassLoader loader = clzz == null ? this.getClass().getClassLoader()
								: clzz.getClassLoader();
						FileToken token = fileToken.get(fileMark);
						if (token!=null) {// 如果文件已经存在
							//判断文件是否修改  先判断最后修改日期  再判断hash  
							//也就是即使修改了文件  但内容完全相同  此时调用之前的类
							if(token.getLastModif()!=lastModif){
								byte[] content = FileUtils.getBytes(file);
								long hash = hash(file, content);
								String className = clzzName + "$Y" + hash;
								//无轮是否加载成功都应该记录
								fileToken.put(fileMark,new FileToken(hash,lastModif));
								Class<?> nc = loadClass(loader, className, clzzName, content);
								if (!checkClass(nc))
									return;
								RegisterDefinition registerDescription = PlugsFactory.getRegisterDescrption(clzz);
								if (registerDescription != null) {
									if (registerDescription.getLinkRegister() != null)
										clzz = registerDescription.getLinkRegister().getRegisterClass();
									registerDescription.updateRegister(nc);
								} else
									tryAddPlugs(nc);
								proxy.put(clzz, nc);
								notifyListener(clzz, nc, new AppClassLoader().loadClass(clzzName, content), file);// 通知修改
								log.debug(clzzName + "  update success!");
							}
							
						} else {// 如果之前没有加载该类
							Class<?> nc = clzz;
							if (nc == null)
								nc = loadClass(loader, clzzName, FileUtils.getBytes(file));
							fileToken.put(fileMark,new FileToken(hash(file, FileUtils.getBytes(file)),lastModif));
							if (!checkClass(nc))
								return;
							tryAddPlugs(nc);
							notifyListener(null, nc, null, file);// 通知添加
							log.debug(clzzName + "  add success!");
						}
					} catch (Throwable e) {
						log.error("failed to update class \"" + clzzName + "\"", e);
					}
				}
			}
		};
		while (true) {
			if(this.contextPathList.isEmpty())
				break;
			for(ContextPath contextPath : contextPathList) {
				Path path = new Path(contextPath.getContextPath());
				path.filter(contextPath.getFilter());
				currentContextPath.setContextPath(contextPath.getContextPath());
				currentContextPath.setFilter(contextPath.getFilter());
				path.scanner(filePathInter);
				//如果是第一次之后的扫描  需要将已删除的文件找出来
				if (cache) {
					Iterator<String> iterator =fileToken.keySet().iterator();
					while (iterator.hasNext()) {
						String file = iterator.next();
						if (!scanner.contains(file) && file.startsWith(contextPath.getContextPath())) {
							String clzzName = getClassName(file,contextPath.getContextPath());
							clzzName = clzzName.substring(0, clzzName.length() - 6);
							Class<?> clzz = null;
							try {
								iterator.remove();
								clzz = Class.forName(clzzName);
								Class<?> cp = (Class<?>) proxy.get(clzz);
								if (cp != null)
									clzz = cp;
								log.debug(clzzName + "  remvoe success!"
										+ (clzz == null ? "" : "proxy class:" + clzz.getName()));
								proxy.remove(clzz);
								notifyListener(clzz, null, null, null);// 通知删除
							} catch (Throwable e) {
								if(clzz != null)
									log.error("failed to update class "+clzz.getName(),e);
								else
									log.error("failed to load class file "+file,e);
							}
						}
					}
					scanner.clear();
					currentContextPath.setContextPath(null);
					currentContextPath.setFilter(null);
				}
			}
			try {
				cache = true;
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private String getClassName(String fileName, String contextPath) {
		String className = classNameCache.get(fileName);
		if(className == null) {
			className = fileName.substring(contextPath.length()+1,fileName.length()-6)
					.replace(File.separator,".");
			classNameCache.put(fileName, className);
		}
		return className;
	}

	protected Class<?> loadClass(java.lang.ClassLoader loader,String className,String clzzName,byte[] content) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Class<?> nc = null;
		try {//这一步为了容错
			nc = Class.forName(className);
		} catch (Throwable e) {
			ClassReader reader = new ClassReader(content, 0, content.length);
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
			nameMappings.put(clzzName.replace(".", "/"), className.replace(".", "/"));
			Remapper mapper = new SimpleRemapper(nameMappings);
			ClassVisitor remapper = new ClassRemapper(cw, mapper);
			reader.accept(remapper, ClassReader.SKIP_FRAMES);
			byte[] bytes = cw.toByteArray();
			nc = loadClass(loader, className, bytes);
		}
		return nc;
	}

	protected boolean checkClass(Class<?> nc) {
		try {
			ClassHelper.getClassHelper(nc);
			return true;
		} catch (Throwable t) {
			t.printStackTrace();
			return false;
		}
	}

	/**
	 * 尝试添加组件
	 * 
	 * @param nc
	 */
	protected void tryAddPlugs(Class<?> nc) {
		if (nc.getAnnotationsByType(Service.class) != null || nc.getAnnotation(Register.class) != null)
			PlugsFactory.getInstance().addPlugs(nc);
	}

	/**
	 * 通知类状态修改监听类
	 * 
	 * @param clzz
	 * @param nc
	 * @param oc
	 * @param file
	 */
	protected void notifyListener(Class<?> clzz, Class<?> nc, Class<?> oc, File file) {
		try {
			List<ClassUpdateListener> updaterList = PlugsFactory.getPlugsInstanceListByAttribute(ClassUpdateListener.class,nc.getName());
			for (ClassUpdateListener listener : updaterList){
				if((clzz!=null&&AppClassLoader.implementOf(clzz, ClassUpdateListener.class))
						||(nc!=null&&AppClassLoader.implementOf(nc, ClassUpdateListener.class))
						||(oc!=null&&AppClassLoader.implementOf(oc, ClassUpdateListener.class)))
					continue;
				listener.updateClass(clzz, nc, oc, file);
			}
		}catch (Throwable t) {
			if(nc != null)
				log.error("could not found updater listener for class "+nc.getName(),t);
		}
		
	}

	/**
	 * 加载类
	 * 
	 * @param loader
	 * @param className
	 * @param bytes
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	protected Class<?> loadClass(java.lang.ClassLoader loader, String className, byte[] bytes)
			throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {
		Method method = java.lang.ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class,
				int.class, int.class);
		method.setAccessible(true);
		Class<?> nc = (Class<?>) method.invoke(loader, className, bytes, 0, bytes.length);
		method.setAccessible(false);
		return nc;
	}

	private void addContextPath(List<String> contextPaths) {
		this.addContextPath(contextPaths.toArray(new String[]{}));
	}
	private void addContextPath(String... contextPaths) {
		for(String contextPath : contextPaths) {
			String context = contextPath;
			String filte = "**.class";
			int sp = context.indexOf(".",context.lastIndexOf("/"));
			if(sp>0) {
				context = contextPath.substring(0,sp);
				filte = contextPath.substring(sp+1);
				if(!filte.endsWith(".class")){
					filte = filte.concat(".class");
				}
			}
			this.contextPathList.add(new ContextPath(context,filte));
		}
	}
	public static class ContextPath{
		public String getContextPath() {
			return contextPath;
		}
		public void setContextPath(String contextPath) {
			this.contextPath = contextPath;
		}
		public String getFilter() {
			return filter;
		}
		public void setFilter(String filter) {
			this.filter = filter;
		}
		public ContextPath(String contextPath, String filter) {
			super();
			this.contextPath = contextPath;
			this.filter = filter;
		}
		private String contextPath;
		private String filter;
	}
	@Override
	public void execute(PlugsFactory plugsFactory) {
		log.debug("enable class hot update deployment service!");
		ClassHotUpdater test = PlugsFactory.getPlugsInstance(ClassHotUpdater.class);
		if(PlugsFactory.getPlug(ClassUpdateListener.class) == null)
			PlugsFactory.getInstance().addPlugs(ClassUpdateListener.class);
		if(test.contextPathList.isEmpty()) {
			Config config = ConfigContext.getInstance().getGlobalConfig();
			config.allowKeyNull();
			config  = config.getConfig("ClassHotUpdater");
			if(config != null) {
				if(config.hasPath("contextPath")) {
					if(config.isList("contextPath")) {
						List<String> contextPaths = config.getStringList("contextPath");
						test.addContextPath(contextPaths);
					}else {
						String contextPath = config.getString("contextPath");
						test.addContextPath(contextPath.split(","));
					}
				}
				
			}
		}
		Thread thread = new Thread(test);
		thread.setName("Plugin class hot update monitor server");
		thread.setDaemon(true);
		thread.start();
	}
}
