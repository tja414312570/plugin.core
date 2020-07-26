package com.yanan.frame.plugin;
/**
 * DEFAULT 默认代理方式,如果调用对象为接口调用,则采用jdk代理,
 * <p>       如果调用对象为普通对象,则采用cglib代理</p>
 * <p>CGLIB	强制采用cglib代理,对所有调用有效</p>
 * <p>BOTH		同时采用jdk代理和cglib代理,对实现接口的类有效</p>
 * <p>JDK		强制使用jdk代理,仅对接口实现类有效</p>
 * <p>！若果调用类为普通类,强制使用cglib代理</p>
 * @author yanan 20180921
 *
 */
public enum ProxyModel {
	CGLIB,JDK,BOTH,DEFAULT, NONE;
	public static ProxyModel getProxyModel(String model){
		if(model==null)
			return ProxyModel.DEFAULT;
		switch (model.toUpperCase()){
		case "CGLIB":
			return ProxyModel.CGLIB;
		case "JDK":
			return ProxyModel.JDK;
		case "BOTH":
			return ProxyModel.BOTH;
		case "DEFAULT":
			return ProxyModel.DEFAULT;
		case "NONE":
			return ProxyModel.NONE;
		default :
			return ProxyModel.DEFAULT;
		}
	}
}
