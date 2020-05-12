package com.YaNan.frame.plugin;

import com.YaNan.frame.utils.asserts.Assert;

public class ParameterInfo {
	Class<?>[] parameterTypes = null;
	Object[] parameters = null;
	private int next = 0;
	public ParameterInfo(int size){
		parameterTypes = new Class<?>[size];
		parameters = new Object[size];
	}
	public void addParameter(Class<?> type,Object value){
		Assert.isTrue(next+1>=parameterTypes.length,new IndexOutOfBoundsException("parameter index more than parameter array size"));
		parameterTypes[next] = type;
		parameters[next++] = value;
	}
	public Class<?>[] getParameterTypes() {
		return parameterTypes;
	}
	public Object[] getParameters() {
		return parameters;
	}
	
}
