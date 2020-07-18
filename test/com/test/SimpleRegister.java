package com.test;

import java.util.Date;
import java.util.List;

import com.yanan.frame.plugin.annotations.AfterInstantiation;
import com.yanan.frame.plugin.annotations.Register;
import com.yanan.frame.plugin.autowired.property.Property;
import com.yanan.frame.plugin.handler.InvokeHandler;
import com.yanan.frame.plugin.PlugsFactory;

@Register(afterInstance="init2")
public class SimpleRegister {
//	@Override
//	public String toString() {
//		return "SimpleRegister [name=" + name + ", date=" + date + "]";
//	}
//	@Property()
	private String name;
	private Date date;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
//	public SimpleRegister(String name) {
//		this.name = name;
//	}
	public SimpleRegister() {
		System.out.println("实例化"+this);
//		new RuntimeException().printStackTrace();
	}
	public static SimpleRegister getInstance() {
		return new SimpleRegister();
	}
	public static SimpleRegister getInstance(String str,List<String> list,boolean ok) {
		System.out.println("多种参数-string-list-booelan");
		System.out.println(str);
		System.out.println(list);
		SimpleRegister simpleRegister = new SimpleRegister();
		simpleRegister.setName("are you ok 1");
		return simpleRegister;
	}
	public static SimpleRegister getInstance(String str,String[] list,String ok) {
		System.out.println("多种参数-string-array-booelan");
		System.out.println(str);
		System.out.println(list);
		System.out.println(ok);
		SimpleRegister simpleRegister = new SimpleRegister();
		simpleRegister.setName("are you ok 2");
		return simpleRegister;
	}
	public static SimpleRegister getInstance(String str,String[] list,SimpleRegister ref) {
		System.out.println("多种参数-string-array-ref");
		System.out.println(str);
		System.out.println(list);
		System.out.println(ref);
		SimpleRegister simpleRegister = new SimpleRegister();
		simpleRegister.setName("are you ok 3");
		simpleRegister.setDate(new Date());
		return simpleRegister;
	}
	@AfterInstantiation
	public void init() {
		System.out.println(PlugsFactory.getInstance().getPlugin(InvokeHandler.class).getRegisterList());
		System.out.println("执行init:"+this+"[name=" + name + ", date=" + date + "]");
	}
	public void init2() {
		System.out.println("执行init2:"+this+"[name=" + name + ", date=" + date + "]");
	}
	public void init3() {
		System.out.println("执行init3:"+this+"[name=" + name + ", date=" + date + "]");
	}

}
