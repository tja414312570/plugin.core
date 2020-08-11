package com.yanan.framework.plugin.autowired.resource;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import com.yanan.framework.plugin.annotations.Register;
import com.yanan.framework.plugin.annotations.Support;
import com.yanan.framework.plugin.definition.RegisterDefinition;
import com.yanan.framework.plugin.handler.FieldHandler;
import com.yanan.framework.plugin.handler.HandlerSet;
import com.yanan.utils.reflect.AppClassLoader;

@Support(Resource.class)
@Register
public class ResourceWiredHandler implements FieldHandler{
	final static String CLASSPATH = "classpath:";
	@Override
	public void preparedField(RegisterDefinition registerDefinition, Object proxy, Object target,
			HandlerSet handlerSet, Field field) {
		Resource resource = handlerSet.getAnnotation(Resource.class);
		String path = resource.value();
		if(path==null) {
			throw new RuntimeException("Resource value is null !\r\nat class : "+target.getClass().getName()
					+"\r\nat field : "+field.getName());
		}
		int cpIndex = path.indexOf(CLASSPATH);
		File file =null;
		try {
			if(cpIndex==-1){
				file= new File(path);
			}else{
				file = new File(this.getClass().getClassLoader().getResource("").getPath().replace("%20"," "),path.substring(cpIndex+CLASSPATH.length()));
			}
			new AppClassLoader(target).set(field, file);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			throw new RuntimeException("Resource wired failed !\r\nat class : "+target.getClass().getName()
					+"\r\nat field : "+field.getName()
					+"\r\nat file : "+file);
		}
	}
	
}