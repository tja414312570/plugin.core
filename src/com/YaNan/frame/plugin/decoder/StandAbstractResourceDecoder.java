package com.yanan.frame.plugin.decoder;

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
import com.yanan.frame.plugin.Environment;
import com.yanan.frame.plugin.Plugin;
import com.yanan.frame.plugin.PlugsFactory;
import com.yanan.frame.plugin.annotations.Register;
import com.yanan.frame.plugin.builder.PluginDefinitionBuilderFactory;
import com.yanan.frame.plugin.definition.RegisterDefinition;
import com.yanan.frame.plugin.exception.PluginInitException;
import com.yanan.frame.plugin.exception.PluginRuntimeException;
import com.yanan.utils.reflect.AppClassLoader;
import com.yanan.utils.resource.Resource;

/**
 * 标准抽象资源解析
 * 
 * @author yanan
 * @param <T> 抽象资源实现
 */
@Register(attribute = { "com.yanan.utils.resource.AbstractResourceEntry", "AbstractResourceEntry" })
public class StandAbstractResourceDecoder<K extends Resource> implements ResourceDecoder<K> {

	private String resourceName;

	public void buildPlugsByConfigList(List<? extends Object> list) {
		for (Object conf : list) {
			buildPlugByConfig(conf);
		}
	}

	public void buildPlugByConfig(Object conf) {
		if (conf == null)
			throw new PluginInitException("conf is null");
		if(!AppClassLoader.implementsOf(conf.getClass(), ConfigValue.class))
			throw new UnsupportedOperationException("the config type is not support");
		ConfigValue configValue = (ConfigValue) conf;
		PluginDefinitionBuilderFactory builder = PluginDefinitionBuilderFactory.getInstance();
		Object plugin = null;
		try {
			if (configValue.valueType() == ConfigValueType.STRING) {
				Class<?> clzz = Class.forName((String) configValue.unwrapped());
				plugin = builder.builderPluginDefinitionAuto(clzz);
			} else if (configValue.valueType() == ConfigValueType.OBJECT) {
				plugin = builder.buildRegisterDefinitionByConfig(((SimpleConfigObject) configValue).toConfig());

			}
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
			Config config = ConfigFactory.parseReader(reader);
			Environment.getEnviroment().mergeConfig(config);
			config.allowKeyNull(true);
			List<? extends Object> list = config.getValueList("plugins");
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
