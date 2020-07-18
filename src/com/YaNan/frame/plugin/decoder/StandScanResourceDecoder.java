package com.yanan.frame.plugin.decoder;

import java.util.Arrays;

import com.yanan.frame.plugin.PlugsFactory;
import com.yanan.utils.resource.ResourceManager;
import com.yanan.utils.resource.scanner.PackageScanner;

/**
 * 标准扫描资源
 * @author yanan
 *
 */
public class StandScanResourceDecoder extends StandAbstractResourceDecoder<StandScanResource> implements ResourceDecoder<StandScanResource>{
	@Override
	public void decodeResource(PlugsFactory factory,StandScanResource resource) {
		String scanExpress = resource.getPath();
		System.out.println("扫描资源:"+scanExpress);
		String[] realPathArray =  ResourceManager.getPathExress(scanExpress);
		System.out.println("转化后路径:"+Arrays.toString(realPathArray));
//		if(scanExpress.indexOf("*")==-1)
//			scanExpress.c
		PackageScanner scanner = new PackageScanner();
		scanner.setScanPath(realPathArray[0]);
//		scanner.doScanner(System.out::println);
		scanner.doScanner((cls) -> addPlugs(cls));
	}
	private void checkDir(String[] dirs) {
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
	}
}
