package com.yanan.framework.plugin.decoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import com.typesafe.config.impl.SimpleConfigObject;
import com.yanan.framework.plugin.Environment;
import com.yanan.framework.plugin.Plugin;
import com.yanan.framework.plugin.PlugsFactory;
import com.yanan.framework.plugin.annotations.Register;
import com.yanan.framework.plugin.builder.PluginDefinitionBuilderFactory;
import com.yanan.framework.plugin.definition.RegisterDefinition;
import com.yanan.framework.plugin.exception.PluginInitException;
import com.yanan.framework.plugin.exception.PluginRuntimeException;
import com.yanan.utils.reflect.ReflectUtils;
import com.yanan.utils.resource.Resource;

/**
 * 标准资源解析，用于解析文件系统资源和类路径资源
 * <p>{@link com.yanan.utils.resource.FileResource}
 * <p>{@link com.yanan.utils.resource.ClassPathResource}
 * 
 * @author yanan
 */
@Register(attribute = { "com.yanan.utils.resource.FileResource", "FileResource", 
		"com.yanan.utils.resource.ClassPathResource", "ClassPathResource"})
public class StandAbstractResourceDecoder implements ResourceDecoder<Resource> {
	//当前资源名称
	private String resourceName;
	
	public void buildPlugsByConfigList(List<? extends Object> list) {
		for (Object conf : list) {
			buildPlugByConfig(conf);
		}
	}
	/**
	 * 调用PluginDefinitionBuilderFactory解析配置，并将生成的Definition添加到容器
	 * @param conf 配置
	 */
	public void buildPlugByConfig(Object conf) {
		if (conf == null)
			throw new PluginInitException("conf is null");
		//检查配置类型
		if(!ReflectUtils.implementsOf(conf.getClass(), ConfigValue.class))
			throw new UnsupportedOperationException("the config type is not support");
		ConfigValue configValue = (ConfigValue) conf;
		Object plugin = null;
		try {
			//若果配置值只是字符串类型，调用默认解析方法即可
			if (configValue.valueType() == ConfigValueType.STRING) {
				Class<?> clzz = Class.forName((String) configValue.unwrapped());
				plugin = PluginDefinitionBuilderFactory.builderPluginDefinitionAuto(clzz);
				//若果是配置类型，调用专用的解析config的方法解析配置
			} else if (configValue.valueType() == ConfigValueType.OBJECT) {
				plugin = PluginDefinitionBuilderFactory.buildRegisterDefinitionByConfig(((SimpleConfigObject) configValue).toConfig());

			}
			//判断解析后的定义类型
			if(Objects.equals(plugin.getClass(), Plugin.class)) {
				PlugsFactory.getInstance().addPlugininDefinition((Plugin) plugin);
			}else {
				PlugsFactory.getInstance().addRegisterDefinition((RegisterDefinition) plugin);
			}
		} catch (Throwable e) {
			throw new PluginInitException("failed to add plug at conf  " + configValue.unwrapped()
			+" at resource "+resourceName+" at line "+configValue.origin().lineNumber(), e);
		}
	}

	@Override
	public void decodeResource(PlugsFactory factory, Resource resource) {
		this.resourceName = resource.getPath();
		InputStream is = null;
		InputStreamReader reader = null;
		try {
			is = resource.getInputStream();
			reader = new InputStreamReader(is);
			//将资源转化为Config
			Config config = ConfigFactory.parseReader(reader);
			//将Config添加到全局Config
			Environment.getEnviroment().mergeConfig(config);
			config.allowKeyNull(true);
			//获取plugins列表
			List<? extends Object> list = config.getValueList("plugins");
			//解析
			buildPlugsByConfigList(list);
		} catch (Exception e) {
			throw new PluginRuntimeException("failed to add plug at file " + resource.getPath(), e);
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
				if (is != null) {
					is.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}
}