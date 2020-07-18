## [WIKI](https://github.com/tja414312570/plugin.core/wiki/home)
# plugin.core Plugin框架的核心
	* 提供配置上下文（ConfigContext）
	* 提供组件代理 （Proxy）
	* 提供调用栈信息记录 （InvokeStack）
	* 提供参数、方法、构造器等工具 （ParameterUtils）
	* 简单的注解方式 （@Register、@Service）
	* 提供错误处理
	* 提供依赖注入（DI @Service）
	* 基于方法、构造器、以及属性的拦截器（AOP）
	* 提供属性注入 （@Property）
	* 提供资源注入 （@Resource）
	* 提供bean管理与注入（和组件不同）
	* jdk和cglib代理以及双代理模式
	* 单例与非单例支持
	* 热更新功能（ClassHotUpdater）
	* 调用加密功能（@Encrypt）
![avatar](https://ufomedia.oss-cn-beijing.aliyuncs.com/QQ20191014-190318.png)
# 20200718 
* 重构plugin核心，分离出实例工厂，组件定义工厂，拦截器构建工厂
* 提供Environment，用于提供plugin环境的全局变量，全局事件，全局配置
* 重新定义plugins参数规范
* 提供参数转化器 ParameterResolver
```java
###################### plugin list
#组件列表
plugins:[
		com.YaNan.frame.plugin.autowired.plugin.PluginWiredHandler, ##服务注入提供
		com.YaNan.frame.plugin.autowired.resource.ResourceWiredHandler, ##资源类注入
		com.YaNan.frame.plugin.autowired.property.PropertyWiredHandler, ##属性注入
		com.YaNan.frame.plugin.autowired.exception.ErrorPlugsHandler, ##错误记录
		##	 com.YaNan.frame.util.quartz.QuartzManager,##Quartz corn注解服务
		##com.YaNan.frame.plugin.hot.ClassHotUpdater,##动态更新服务
		com.YaNan.frame.plugin.builder.resolver.ArrayParameterResolver,#数组解析
		com.YaNan.frame.plugin.builder.resolver.ReferenceParameterResolver,#引用解析
		com.YaNan.frame.plugin.builder.resolver.DateParameterResolver,#日期解析 解析参数为date的参数，实现了ParameterResolver
		{
			#没有args,且没有method，表明使用无参构造器
			class:com.test.SimpleRegister, #定义类
			id:test1,#将register定义一个id，拥有ID的register将强制转换
			init:init2#register初始化后调用init2方法 只能使用数组
			field.name:test1 from plug #将name1赋值为string类型的内容 field字段支持object和数组-->格式为field.属性名.内容 或则为 field.属性名.格式命名.内容
			field.date.date:"2020-07-17 09:51:35"#将date字段赋值为data类型的日期 --使用com.YaNan.frame.plugin.builder.resolver.DateParameterResolver这个解析器
		},
		{
			class:com.test.SimpleRegister,
			id:test2,
			method:getInstance #表明使用getInstance的方法构造，如果强制cglib代理，则会将getInstance的对象克隆为一个cglib对象
			args:[hellow world,[as,b,c,d],true] #构造时采用三个参数，此字段只支持数组
			types:[default,-,-] #参数类型，可选字段，-和default都表示采用默认的类型，这里表示采用string,list,boolean的getInstance方法，getInstance(string,list,boolean)
			init:[init,init2]
			field:[ #使用数组类型的属性声明
				{name:test2 hello world sss},#将name字段赋值为string类型的内容
				{date.date:"2020-07-17 14:18:35"}#将date字段用DateParameterResolver转化后赋值到date字段
			]
		},
		{
			ref:test2, #引用test2的定义，此组件声明将继承test2的所有属性，自己的定义将覆盖test2的定义
			id:testRef,#将id属性覆盖
			method:getInstance
			args:[test ref hellow world,[a,b,c,d],test2]
			types:[default,arrayS,ref]#使用ArrayParameterResolver将第二个参数转化为array格式,第三个参数使用ReferenceParameterResolver引用，使用getInstance(string,array,SimpleRegister)的方法构造实例
		}
	]

```
```java 测试类
package com.test;

import java.util.Date;
import java.util.List;

import com.YaNan.frame.plugin.PlugsFactory;
import com.YaNan.frame.plugin.annotations.AfterInstantiation;
import com.YaNan.frame.plugin.annotations.Register;
import com.YaNan.frame.plugin.autowired.property.Property;
import com.YaNan.frame.plugin.handler.InvokeHandler;

@Register(afterInstance="init2")
public class SimpleRegister {
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
	@AfterInstantiation #构造后执行此方法
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

```
# 参数解析器
* 参数解析器分为解析时参数ParameterResolver和实例化时参数DelayParameterResolver
* DelayParameterResolver继承自ParameterResolver
* 解析时参数表示参数在解析RegisterDefinition时就会初始化调用的值
* 实例化时参数解析器表示参数在RegisterDefinition实例化时初始化调用值

```java
package com.YaNan.frame.plugin.builder.resolver;
import java.util.List;

import com.YaNan.frame.plugin.annotations.Register;
import com.YaNan.frame.plugin.definition.RegisterDefinition;
import com.typesafe.config.ConfigList;
@Register(attribute= {"array","arrayS"}) //表明处理格式命名为array,arrayS的参数
public class ArrayParameterResolver implements ParameterResolver<ConfigList>{
	@Override
	public Object resove(ConfigList configValue, String type, int index, RegisterDefinition registerDefinition) {
		List<Object> unwrappedList = configValue.unwrapped();
		switch (type) {
		case "arrayS":
			return unwrappedList.toArray(new String[unwrappedList.size()]);
		default:
			return unwrappedList.toArray();
		}
	}
}

```
# 事件管理
* 事件分为三部分，1：事件源(InterestedEventSource)，2：事件（AbstractEvent），3：监听器（EventListener<T extends AbstractEvent>）
* 事件源表明事件分发的类型，或则事件监听感兴趣的类型
* 事件为事件分发时的事件定义
* 注册事件监听 Environment.registEventListener(InterestedEventSource,EventListener)
* 分发事件 Environment.distributeEvent(InterestedEventSource,AbstractEvent)
```java
	Environment.getEnviroment().registEventListener(new PluginEventSource(), new EventListener<PluginEvent>() {
		@Override
		public void onEvent(PluginEvent abstractEvent) {
			System.out.println("事件:"+abstractEvent.getEventType()+"==>"+abstractEvent.getEventContent());
		}
	});
	PlugsFactory.init("classpath:plugin.yc");
	
事件:add_resource==>AbstractResourceEntry
事件:refresh==>com.YaNan.frame.plugin.PlugsFactory@3d4eac69
事件:add_registerDefinition==>RegisterDefinition
事件:register_init==>RegisterDefinition
事件:inited==>com.YaNan.frame.plugin.PlugsFactory@3d4eac69
```
