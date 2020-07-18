package com.YaNan.frame.plugin.builder.resolver;
import java.util.List;

import com.YaNan.frame.plugin.annotations.Register;
import com.YaNan.frame.plugin.definition.RegisterDefinition;
import com.typesafe.config.ConfigList;
@Register(attribute= {"array","arrayS"})
public class ArrayParameterResolver implements ParameterResolver<ConfigList>{
	@Override
	public Object resove(ConfigList configValue, String type, int index, RegisterDefinition registerDefinition) {
		List<Object> unwrappedList = configValue.unwrapped();
		System.out.println("数组解析:"+type);
		switch (type) {
		case "arrayS":
			return unwrappedList.toArray(new String[unwrappedList.size()]);
		default:
			return unwrappedList.toArray();
		}
	}
}
