package com.yanan.frame.plugin.hot;

import java.io.File;

import com.yanan.frame.plugin.annotations.Register;

@Register
public interface ClassUpdateListener {
	void updateClass(Class<?> originClass,Class<?> updateClass,Class<?> updateOrigin,File updateFile);
}
