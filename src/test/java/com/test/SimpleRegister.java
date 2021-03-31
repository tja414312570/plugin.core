package com.test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import com.yanan.framework.plugin.annotations.Register;
import com.yanan.utils.resource.Resource;

@Register
public class SimpleRegister {
	static int count;
	private Resource file;
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
	public SimpleRegister() {
		System.out.println("                   "+(++count));
		System.out.println("实例化:"+this);
	}
	public static SimpleRegister getInstance() {
		return new SimpleRegister();
	}
	public static SimpleRegister getInstance(String str,List<String> list,boolean ok) {
		SimpleRegister simpleRegister = new SimpleRegister();
		simpleRegister.setName("g1");
		System.out.println("多种参数-string-list-booelan");
		System.out.println(str);
		System.out.println(list);
		return simpleRegister;
	}
	public static SimpleRegister getInstance(String str,String[] list,boolean ok) {
		SimpleRegister simpleRegister = new SimpleRegister();
		simpleRegister.setName("g2");
		System.out.println("多种参数-string-array-booelan");
		System.out.println(str);
		System.out.println(list);
		System.out.println(ok);
		return simpleRegister;
	}
	public static String getString() {
		System.out.println("获取字符串");
		return "返回的内容";
	}
	public String getString2(String name) {
		System.out.println("获取字符串2:"+name);
		return "您的名字"+name;
	}
	public static SimpleRegister getInstance(String str,String[] list,SimpleRegister ref) {
		SimpleRegister simpleRegister = new SimpleRegister();
		simpleRegister.setName("g3");
		simpleRegister.setDate(new Date());
		System.out.println("多种参数-string-array-ref");
		System.out.println(str);
		System.out.println(list);
		System.out.println(ref);
		return simpleRegister;
	}
	@PostConstruct
	public void init() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dates = this.date == null?null:sdf.format(this.date);
		System.out.println("执行init:"+this+",file:"+this.file+",name:"+this.name+",date:"+dates);
	}
//	public void init2() {
//		System.out.println("执行init2:"+this+"[name=" + name + ", date=" + date + "]");
//	}
//	public void init3() {
//		System.out.println("执行init3:"+this+"[name=" + name + ", date=" + date + "]");
//	}

}
