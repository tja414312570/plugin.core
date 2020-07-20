package com.yanan.frame.plugin.decoder;

import java.util.Arrays;

import com.yanan.frame.plugin.Plugin;
import com.yanan.frame.plugin.PlugsFactory;
import com.yanan.frame.plugin.annotations.AfterInstantiation;
import com.yanan.frame.plugin.annotations.Register;
import com.yanan.frame.plugin.annotations.Service;
import com.yanan.frame.plugin.builder.PluginDefinitionBuilderFactory;
import com.yanan.frame.plugin.definition.RegisterDefinition;
import com.yanan.utils.resource.ResourceManager;
import com.yanan.utils.resource.scanner.PackageScanner;

/**
 * 标准扫描资源
 * @author yanan
 *
 */
@Register(attribute="StandScanResource",id="standScanResourceDecoder")
public class StandScanResourceDecoder extends StandAbstractResourceDecoder<StandScanResource> implements ResourceDecoder<StandScanResource>{
	@AfterInstantiation
	public void init() {
		System.out.println("初始化后执行："+this);
//		new RuntimeException().printStackTrace();
	}
	@Override
	public void decodeResource(PlugsFactory factory,StandScanResource resource) {
		System.out.println(this);
		String scanExpress = resource.getPath();
		System.out.println("扫描资源:"+scanExpress);
		String[] realPathArray =  ResourceManager.getPathExress(scanExpress);
		System.out.println("转化后路径:"+Arrays.toString(realPathArray));
		PackageScanner scanner = new PackageScanner();
		scanner.setScanPath(realPathArray[0]);
		scanner.doScanner((cls) -> tryDecodeDefinition(cls));
	}
	private void tryDecodeDefinition(Class<?> cls) {
		
		if(cls.getAnnotation(Service.class)!= null) {
			try {
				Plugin plugin = PluginDefinitionBuilderFactory.getInstance().builderPluginDefinition(cls);
				PlugsFactory.getInstance().addPlugininDefinition(plugin);
			}catch(Throwable t) {
//				t.printStackTrace();
			}
		}
		if(cls.getAnnotation(Register.class)!= null) {
			try {
				RegisterDefinition registerDefinition = PluginDefinitionBuilderFactory.getInstance().builderRegisterDefinition(cls);
				PlugsFactory.getInstance().addRegisterDefinition(registerDefinition);
			}catch(Throwable t) {
//				t.printStackTrace();
			}
			
		}
	}
}
