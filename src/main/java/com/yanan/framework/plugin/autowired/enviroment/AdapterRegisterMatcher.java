package com.yanan.framework.plugin.autowired.enviroment;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Arrays;

import com.yanan.framework.plugin.annotations.Register;
import com.yanan.framework.plugin.matcher.RegisterMatcher;
import com.yanan.utils.reflect.ReflectUtils;

@Register(attribute = "com.yanan.framework.plugin.autowired.enviroment.ResourceAdapter")
public class AdapterRegisterMatcher implements RegisterMatcher {

	@Override
	public String[] attributes(Class<?> registerClass) {
		Adapter adapter = registerClass.getAnnotation(Adapter.class);
		if (adapter.input().length == 0) {
			if (adapter.target().length == 0) {
				Class<?>[] clzz = ReflectUtils
						.getActualType(ReflectUtils.getGenericInterface(registerClass, ResourceAdapter.class));
				return new String[] { clzz[0].getSimpleName() + "_" + clzz[1].getSimpleName() };
			}
			String[] attrs = new String[adapter.target().length];
			for (int i = 0; i < adapter.target().length; i++)
				attrs[i] = "*_" + adapter.target()[i].getSimpleName();
			return attrs;
		} else {
			if (adapter.target().length == 0) {
				String[] attrs = new String[adapter.input().length];
				for (int i = 0; i < adapter.input().length; i++)
					attrs[i] = adapter.input()[i].getSimpleName() + "_*";
				return attrs;
			}
			String[] attrs = new String[adapter.input().length * adapter.target().length];
			int c = 0;
			for (int i = 0; i < adapter.input().length; i++) {
				for (int j = 0; j < adapter.target().length; j++)
					attrs[c++] = adapter.input()[i].getSimpleName() + "_" + adapter.target()[j].getSimpleName();
			}
			return attrs;
		}
	}

	@Override
	public String parseAttribute(Object... args) {
		return args[0].getClass().getSimpleName() + "_" + args[1].getClass().getSimpleName();
	}

	@Override
	public String parseAttribute(AnnotatedElement args) {
		Adapter adapter = args.getAnnotation(Adapter.class);
		if (adapter == null) {
			Class<?>[] types = ReflectUtils.getParameterizedType(args);
			return types[0].getSimpleName() + "_" + types[1].getSimpleName();
		} else {
			return adapter.input()[0].getSimpleName() + "_" + adapter.target()[0].getSimpleName();
		}
	}

}
