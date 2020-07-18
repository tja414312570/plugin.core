package com.yanan.frame.plugin.decoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.util.List;

import com.yanan.frame.plugin.annotations.Register;
import com.yanan.frame.plugin.annotations.Service;
import com.yanan.frame.plugin.builder.PluginDefinitionBuilderFactory;
import com.yanan.frame.plugin.definition.PluginDefinition;
import com.yanan.frame.plugin.definition.RegisterDefinition;
import com.yanan.frame.plugin.exception.PluginInitException;
import com.yanan.frame.plugin.exception.PluginRuntimeException;
import com.yanan.utils.resource.AbstractResourceEntry;
import com.yanan.utils.resource.Resource;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.impl.SimpleConfigObject;
import com.yanan.frame.plugin.Environment;
import com.yanan.frame.plugin.Plugin;
import com.yanan.frame.plugin.PlugsFactory;
import com.yanan.frame.plugin.PlugsFactory.STREAM_TYPT;

/**
 * 标准抽象资源解析
 * @author yanan
 * @param <T> 抽象资源实现
 */
public class StandAbstractResourceDecoder<K extends Resource> implements ResourceDecoder<K>{
	private PlugsFactory factory;
	public void buildPlugsByConfigList(List<? extends Object> list) {
		for (Object conf : list) {
			buildPlugByConfig(conf);
		}
	}
	public void buildPlugByConfig(Object conf) {
		if (conf == null)
			throw new PluginInitException("conf is null");
		PluginDefinitionBuilderFactory builder = PluginDefinitionBuilderFactory.getInstance();
		Object plugin = null ;
		try {
			if (conf.getClass().equals(String.class)) {
				Class<?> clzz = Class.forName((String) conf);
				plugin = builder.builderPlugsAuto(clzz);
			} else if (conf.getClass().equals(SimpleConfigObject.class)) {
				plugin = builder.buildByConfig(((SimpleConfigObject) conf).toConfig());
				
			}
			PlugsFactory.getInstance().addRegisterDefinition((RegisterDefinition) plugin);
		} catch (Throwable e) {
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
			if(!isAvailable()) {
				this.init0();
			}
			if (type==STREAM_TYPT.CLASS) {
				//read bytes and close stream
				byte[] temp = new byte[stream.available()];
				stream.read(temp,0,temp.length);
				stream.close();
				//load class
				Class<?> clzz =new com.yanan.utils.reflect.AppClassLoader().loadClass(className,temp);
				//Register
				PluginDefinition plugsDescription = new PluginDefinition(clzz);
				Plugin plug = new Plugin(plugsDescription);
				this.plugsContatiner.put(plugsDescription.getPlugClass(), plug);
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
		Annotation anno;
		if(cls.getAnnotation(Service.class) != null) {
			Plugin plugin = (Plugin) PluginDefinitionBuilderFactory
					.getInstance().builderPlugsAuto(cls);
		}else if((anno = cls.getAnnotation(Register.class)) != null) {
			RegisterDefinition registerDefinition = PluginDefinitionBuilderFactory
					.getInstance().buildByAnnotation((Register) anno, cls);
		}
	}

	public void addPlugsAuto(Class<?> cls) {
		Service service = cls.getAnnotation(Service.class);
		Register register = cls.getAnnotation(Register.class);
		if (service != null || register != null) {
			if (service != null) {// 如果是Service
				PluginDefinition plugsDescrption = new PluginDefinition(service, cls);
				Plugin plug = new Plugin(plugsDescrption);
				this.plugsContatiner.put(cls, plug);
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
			PluginDefinition plugsDescrption = new PluginDefinition(service, cls);
			Plugin plug = new Plugin(plugsDescrption);
			this.plugsContatiner.put(cls, plug);
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
		PluginDefinition plugsDescrption = new PluginDefinition(service, cls);
		Plugin plug = new Plugin(plugsDescrption);
		factory.addPluginDefinition(plug);
		
	}

	public void addPlugsRegister(Class<?> cls) {
		Register register = cls.getAnnotation(Register.class);
		try {
			RegisterDescription registerDescription;
			if(register == null) {
				registerDescription = new RegisterDescription(cls);
			}else {
				registerDescription = new RegisterDescription(register, cls);
			}
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
		PluginDefinition plugsDescrption = new PluginDefinition(plugClass);
		Plugin plug = new Plugin(plugsDescrption);
		this.plugsContatiner.put(plugClass, plug);
	}
	@Override
	public void decodeResource(PlugsFactory factory,Resource resource) {
		this.factory = factory;
		String scanExpress = resource.getPath();
		System.out.println("抽象资源:"+scanExpress);
		InputStream is = null;
		InputStreamReader reader = null;
		try {
			is = resource.getInputStream();
			reader = new InputStreamReader(is);
			Config config = ConfigFactory.parseReader(reader);
			Environment.getEnviroment().mergeConfig(config);
			config.allowKeyNull(true);
			List<? extends Object> list = config.getValueListUnwrapper("plugins");
			buildPlugsByConfigList(list);
		} catch (Exception e) {
			throw new PluginRuntimeException("failed to add plug at file " + resource.getPath(), e);
		}finally {
			try {
				if(reader != null) {
					reader.close();
				}
				if(is != null) {
					is.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
}
