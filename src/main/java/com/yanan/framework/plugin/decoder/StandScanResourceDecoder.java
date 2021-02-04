package com.yanan.framework.plugin.decoder;

import com.yanan.framework.plugin.Plugin;
import com.yanan.framework.plugin.PlugsFactory;
import com.yanan.framework.plugin.annotations.AfterInstantiation;
import com.yanan.framework.plugin.annotations.Register;
import com.yanan.framework.plugin.annotations.Service;
import com.yanan.framework.plugin.builder.PluginDefinitionBuilderFactory;
import com.yanan.framework.plugin.definition.RegisterDefinition;
import com.yanan.utils.resource.ResourceManager;
import com.yanan.utils.resource.scanner.PackageScanner;

/**
 * 标准扫描资源
 * @author yanan
 *
 */
@Register(attribute="StandScanResource",id="standScanResourceDecoder")
public class StandScanResourceDecoder implements ResourceDecoder<StandScanResource>{
	@AfterInstantiation
	public void init() {
	}
	@Override
	public void decodeResource(PlugsFactory factory,StandScanResource resource) {
		String scanExpress = resource.getPath();
		String[] realPathArray =  ResourceManager.getPathExress(scanExpress);
		PackageScanner scanner = new PackageScanner();
		scanner.setScanPath(realPathArray);
		scanner.doScanner((cls) -> tryDecodeDefinition(cls));
	}
	private void tryDecodeDefinition(Class<?> cls) {
		if(cls.getAnnotation(Service.class)!= null) {
			try {
				Plugin plugin = PluginDefinitionBuilderFactory.builderPluginDefinition(cls);
				PlugsFactory.getInstance().addPlugininDefinition(plugin);
			}catch(Throwable t) {
//				t.printStackTrace();
			}
		}
		if(cls.getAnnotation(Register.class)!= null) {
			try {
				RegisterDefinition registerDefinition = PluginDefinitionBuilderFactory.builderRegisterDefinition(cls);
				registerDefinition.setLazyInit(true);
				PlugsFactory.getInstance().addRegisterDefinition(registerDefinition);
			}catch(Throwable t) {
//				t.printStackTrace();
			}
			
		}
	}
}