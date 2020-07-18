package com.YaNan.frame.plugin;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import com.YaNan.frame.plugin.builder.resolver.ParameterResolver;
import com.YaNan.frame.plugin.definition.MethodDefinition;
import com.typesafe.config.ConfigValue;

/**
 * 构造器定义
 * @author yanan
 */
public class ConstructorDefinition extends MethodDefinition {
	private Constructor<?> constructor;
	public ConstructorDefinition(Constructor<?> constructor, Class<?>[] argsType, Object[] args,
			ParameterResolver<ConfigValue>[] resolvers, String[] type) {
		super(null,argsType,args,resolvers,type);
		this.constructor = constructor;
	}
	public Constructor<?> getConstructor() {
		return constructor;
	}
	public void setConstructor(Constructor<?> constructor) {
		this.constructor = constructor;
	}
	@Override
	public String toString() {
		return "ConstructorDefinition [constructor=" + constructor + ", argsType=" + Arrays.toString(argsType) + ", args="
				+ Arrays.toString(args) + ", resolvers=" + Arrays.toString(resolvers) + ", type="
				+ Arrays.toString(type) + "]";
	}
}
