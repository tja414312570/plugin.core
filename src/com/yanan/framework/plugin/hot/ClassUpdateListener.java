package com.yanan.framework.plugin.hot;

import java.io.File;

import com.yanan.framework.plugin.annotations.Register;

@Register
public interface ClassUpdateListener {
	void updateClass(Class<?> originClass,Class<?> updateClass,Class<?> updateOrigin,File updateFile);
}