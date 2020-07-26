package com.yanan.frame.plugin.definition;

import java.lang.reflect.Method;
import java.util.Arrays;

import com.yanan.frame.plugin.builder.resolver.ParameterResolver;
import com.typesafe.config.ConfigValue;

/**
 * 方法定义，用于描述一个方法的执行策略的定义
 * @author yanan
 *
 */
public class MethodDefinition {
	//定义需要执行的方法
	private Method method;
	//需要执行方法或构造器的参数的类型
	protected Class<?>[] argsType;
	//需要执行方法或构造器的参数
	protected Object[] args;
	//当一些参数不能在解析时执行，需要在实例化时才能获取到的具体指时的参数解析器集合
	protected ParameterResolver<ConfigValue>[] resolvers;
	//当存在实例化时解析参数是，为配置文件中定义的类型
	protected String[] type;
	public String[] getType() {
		return type;
	}
	public void setType(String[] type) {
		this.type = type;
	}
	public ParameterResolver<ConfigValue>[] getResolvers() {
		return resolvers;
	}
	public void setResolvers(ParameterResolver<ConfigValue>[] resolvers) {
		this.resolvers = resolvers;
	}
	int hash(){
		int hash = 0x16;
		hash += method == null ? 1 :method.hashCode();
		hash += argsType == null ? 2 : Arrays.hashCode(argsType);
		hash += args == null ? 3 :Arrays.hashCode(args);
		return hash;
	}
	/**
	 * 方法定义，用于描述方法的执行策略
	 * @param method 需要执行的方法
	 * @param argsType 参数类型
	 * @param args 参数
	 * @param resolvers 解析器集合
	 * @param type 原始类型
	 */
	public MethodDefinition(Method method, Class<?>[] argsType, Object[] args,
			ParameterResolver<ConfigValue>[] resolvers, String[] type) {
		super();
		this.method = method;
		this.argsType = argsType;
		this.args = args;
		this.resolvers = resolvers;
		this.type = type;
	}
	@Override
	public boolean equals(Object obj) {
		return obj==null?false:this.hash() == ((MethodDefinition)obj).hash();
	}
	public Method getMethod() {
		return method;
	}
	public void setMethod(Method method) {
		this.method = method;
	}
	public Class<?>[] getArgsType() {
		return argsType;
	}
	public void setArgsType(Class<?>[] argsType) {
		this.argsType = argsType;
	}
	public Object[] getArgs() {
		return args;
	}
	public void setArgs(Object[] args) {
		this.args = args;
	}
	@Override
	public String toString() {
		return "MethodDefinition [method=" + method + ", argsType=" + Arrays.toString(argsType) + ", args="
				+ Arrays.toString(args) + ", resolvers=" + Arrays.toString(resolvers) + ", type="
				+ Arrays.toString(type) + "]";
	}
}
