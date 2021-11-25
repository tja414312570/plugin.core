package com.yanan.framework.plugin.autowired.plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import com.yanan.framework.plugin.FactoryRefreshProcess;
import com.yanan.framework.plugin.PlugsFactory;
import com.yanan.framework.plugin.annotations.Register;
import com.yanan.framework.plugin.annotations.Support;
import com.yanan.framework.plugin.definition.RegisterDefinition;
import com.yanan.utils.reflect.ReflectUtils;

@Register
@Support(Configuration.class)
public class ConfigurationHandler implements FactoryRefreshProcess{
	@SuppressWarnings("unchecked")
	@Override
	public void process(PlugsFactory plugsFactory) {
		Set<RegisterDefinition> defined  = plugsFactory.getNewRegisterDefinitionList();
		defined.forEach(def->{
			if(def.getRegisterClass().getAnnotation(Configuration.class) != null) {
				Object instance = PlugsFactory.getPluginsInstance(def.getRegisterClass());
				Method[] methods = ReflectUtils.getAllMethods(def.getRegisterClass());
				for(Method method : methods) {
					try {
						Object value = ReflectUtils.invokeMethod(instance, method);
						Class<Object> clzz = (Class<Object>) method.getReturnType();
						if(method.getReturnType() != void.class) {
							PlugsFactory.proxyInstance(clzz,value);
						}
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						throw new RuntimeException(e);
					}
				}
			}
		});
	}
}
