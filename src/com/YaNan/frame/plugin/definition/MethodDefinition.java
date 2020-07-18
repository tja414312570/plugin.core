package com.YaNan.frame.plugin.definition;

import java.lang.reflect.Method;
import java.util.Arrays;

import com.YaNan.frame.plugin.builder.resolver.ParameterResolver;
import com.typesafe.config.ConfigValue;

public class MethodDefinition {
	private Method method;
	protected Class<?>[] argsType;
	protected Object[] args;
	protected ParameterResolver<ConfigValue>[] resolvers;
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
