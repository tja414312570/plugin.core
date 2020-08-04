package com.yanan.framework.plugin.hot;

import java.io.File;

import com.yanan.framework.plugin.annotations.Register;

@Register
public class ClassUpdateListenerImpl implements ClassUpdateListener{

	@Override
	public void updateClass(Class<?> originClass, Class<?> updateClass, Class<?> updateOrigin, File updateFile) {
			System.out.println("原始类:"+originClass);
			System.out.println("更新类:"+updateClass);
			System.out.println("更新文件:"+updateFile);
	}

}