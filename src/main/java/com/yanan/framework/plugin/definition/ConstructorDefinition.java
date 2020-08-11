package com.yanan.framework.plugin.definition;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import com.yanan.framework.plugin.builder.resolver.ParameterResolver;
import com.typesafe.config.ConfigValue;

/**
 * 构造器定义
 * @author yanan
 */
public class ConstructorDefinition extends MethodDefinition {
	//需要执行的构造器
	private Constructor<?> constructor;
	/**
	 * 构造器定义，用于描述构造器的执行策略
	 * @param constructor 需要执行的构造器
	 * @param argsType 参数类型
	 * @param args 参数
	 * @param resolvers 解析器集合
	 * @param type 原始类型
	 */
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