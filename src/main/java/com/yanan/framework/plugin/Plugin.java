package com.yanan.framework.plugin;

import java.util.ArrayList;
import java.util.List;

import com.yanan.framework.plugin.definition.PluginDefinition;
import com.yanan.framework.plugin.definition.RegisterDefinition;
import com.yanan.utils.CollectionUtils;
import com.yanan.utils.string.StringUtil;

/**
 * 用于描述组件信息
 * 一个接口对应一个组件
 * @author yanan
 *
 */
public class Plugin {
	/**
	 * 组件的描述类型 0==>注解   1==>配置
	 */
	private PluginDefinition description;
	private List<RegisterDefinition> registerList = new ArrayList<>();
	public Plugin(PluginDefinition descrption){
		this.description = descrption;
	}
	public void addRegister(RegisterDefinition registerDefinition) {
		if(registerList.indexOf(registerDefinition) != -1) {
			return;
		}
		//为了保持与默认注册组件有相同的优先级，采用倒叙对比法进行优先级运算 比如 原始数据 0  0  2  3 现在需要插入 1  则插入后应该为 0 0 1 2 3
		if(this.registerList.size() == 0){
			this.registerList.add(registerDefinition);
			return;
		} 
		for(int i = this.registerList.size()-1;i >= 0;i--){
			if(registerDefinition.getPriority() >= this.registerList.get(i).getPriority()){
				this.registerList.add(i+1,registerDefinition);
				break;
			}
			if(i == 0){
				this.registerList.add(0,registerDefinition);
			}
		}
	}
	public RegisterDefinition getRegisterDefinitionByAttribute(String attribute) {
		RegisterDefinition registerDefinition  = this.getRegisterDefinitionByAttributeStrict(attribute);
		return registerDefinition==null?registerList.get(0):registerDefinition;
	}
	public List<RegisterDefinition> getRegisterDefinitionListByAttribute(String attribute) {
		List<RegisterDefinition> retistDefinitionList = new ArrayList<>();
		for(int i = 0;i<registerList.size();i++) {
			if(StringUtil.match(attribute, registerList.get(i).getAttribute())) {
				retistDefinitionList.add(registerList.get(i));
			}
		}
		return retistDefinitionList;
	}
	public RegisterDefinition getRegisterDefinitionByAttributeStrict(String attribute) {
		for(int i = 0;i<registerList.size();i++){
			if(StringUtil.match(attribute, registerList.get(i).getAttribute())) {
				return registerList.get(i);
			}
		}
		return null;
	}
	public PluginDefinition getDefinition() {
		return description;
	}
	public void setDefinition(PluginDefinition description) {
		this.description = description;
	}
	public List<RegisterDefinition> getRegisterList() {
		return registerList;
	}
	public void setRegisterList(List<RegisterDefinition> registerList) {
		this.registerList = registerList;
	}
	public RegisterDefinition getDefaultRegisterDefinition() {
		return CollectionUtils.isEmpty(this.registerList)?null:this.registerList.get(0);
	}
	public void setDefaultRegisterDefinition(RegisterDefinition defaultRegisterDefinition) {
		this.registerList.add(0, defaultRegisterDefinition);
	}
	public List<RegisterDefinition> getRegisterDefinitionList() {
		return this.registerList;
	}
	public RegisterDefinition getRegisterDefinitionByInsClass(Class<?> insClass) {
		for(RegisterDefinition rd :registerList) {
			if(rd.getRegisterClass().equals(insClass)) {
				return rd;
			}
		}
		return null;
	}
	
	
}