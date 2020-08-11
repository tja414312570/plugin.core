package com.yanan.framework.plugin.definition;

import java.lang.reflect.Field;

import com.yanan.framework.plugin.builder.resolver.ParameterResolver;

public class FieldDefinition{
	private Field field;
	private String type;
	private Object value;
	private ParameterResolver<?> resolver;
	public FieldDefinition(Field field, String type, Object value, ParameterResolver<?> resolver) {
		super();
		this.field = field;
		this.type = type;
		this.value = value;
		this.resolver = resolver;
	}
	int hash(){
		int hash = 0x16;
		hash += field == null ? 1 :field.hashCode();
		hash += type == null ? 2 :type.hashCode();
		hash += value == null ? 3 :value.hashCode();
		hash += resolver == null ? 4 :resolver.hashCode();
		return hash;
	}
	@Override
	public boolean equals(Object obj) {
		return obj==null?false:this.hash() == ((FieldDefinition)obj).hash();
	}
	@Override
	public String toString() {
		return "FieldDefinition [field=" + field + ", type=" + type + ", value=" + value + ", resolver=" + resolver
				+ "]";
	}
	public Field getField() {
		return field;
	}
	public void setField(Field field) {
		this.field = field;
	}
	public Object getValue() {
		return value;
	}
	public void setValue(Object value) {
		this.value = value;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public ParameterResolver<?> getResolver() {
		return resolver;
	}
	public void setResolver(ParameterResolver<?> resolver) {
		this.resolver = resolver;
	}

}