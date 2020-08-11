package com.yanan.framework.plugin.builder.resolver;
import java.lang.reflect.Array;

import java.util.List;

import com.yanan.framework.plugin.annotations.Register;
import com.yanan.framework.plugin.definition.RegisterDefinition;
import com.typesafe.config.ConfigList;
/**
 * config的list转array
 * @author yanan
 */
@Register(attribute= {"array","arrayS"})
public class ArrayParameterResolver implements ParameterResolver<ConfigList>{
	@Override
	public Object resove(ConfigList configValue, String type, int index, RegisterDefinition registerDefinition) {
		List<Object> unwrappedList = configValue.unwrapped();
		if(unwrappedList.size() == 0 )
			return null;
		switch (type) {
		case "arrayS":
			return unwrappedList.toArray(new String[unwrappedList.size()]);
		default:
			Object arrays = Array.newInstance(unwrappedList.get(0).getClass(), unwrappedList.size());
			for(int i = 0;i<unwrappedList.size();i++) {
				Array.set(arrays, i, unwrappedList.get(i));
			}
			return arrays;
		}
	}
}