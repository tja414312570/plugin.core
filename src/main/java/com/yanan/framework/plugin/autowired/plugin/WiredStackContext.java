package com.yanan.framework.plugin.autowired.plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Stack;

import com.yanan.framework.plugin.definition.RegisterDefinition;
import com.yanan.framework.plugin.handler.MethodHandler;

/**
 * 调用栈记录，用于记录当前栈的数据
 * 当前被注入实例仅能感知到上一个依赖类的数据
 * @author yanan
 *
 */
public class WiredStackContext {
	public static final int FIELD = 0;
	public static final int METHOD = 1;
	public static final int CONSTRUCT = 2;
	private static InheritableThreadLocal<Stack<Object[]>> currentContext = new InheritableThreadLocal<>();
	static void push(Object... datas) {
		Stack<Object[]> stack = currentContext.get();
		if(stack == null) {
			stack = new Stack<>();
			currentContext.set(stack);
		}
		stack.push(datas);
	}
	public static int stackType() {
		Object[] stacks = peek();
		if(stacks == null ) 
			return -1;
		return (int)stacks[0];
	}
	public static String stackString() {
		int stackType = stackType();
		switch(stackType) {
			case 0:
				return "Field:"+getField();
			case 1:
				return "Method"+getMethod();
			case 2:
				return "Construector"+getConstructor();
			default:
				return "Unknow";
		}
	}
	static void pop() {
		Stack<Object[]> stack = currentContext.get();
		if(stack != null) 
			stack.pop();
	}
	public static Object[] peek() {
		Stack<Object[]> stack = currentContext.get();
		if(stack != null && stack.peek().length > 0) 
			return stack.peek();
		return null;
	}
	public static MethodHandler getMethodHandler() {
		Object[] stacks = peek();
		if(stacks != null && (int)stacks[0] == METHOD) {
			return (MethodHandler) stacks[1];
		}
		return null ; 
	}
	public static RegisterDefinition getRegisterDefintion() {
		Object[] stacks = peek();
		if(stacks == null)
			return null;
		if((int)stacks[0] == METHOD) {
			return ((MethodHandler) stacks[1]).getPlugsProxy().getRegisterDefinition();
		}
		if((int)stacks[0] == CONSTRUCT) {
			return (RegisterDefinition) stacks[1];
		}
		if((int)stacks[0] == FIELD) {
			return (RegisterDefinition) stacks[1];
		}
		return null ; 
	}
	@SuppressWarnings("unchecked")
	public static <T> T getProxyInstance() {
		Object[] stacks = peek();
		if(stacks == null)
			return null;
		if((int)stacks[0] == METHOD) {
			return ((MethodHandler) stacks[1]).getPlugsProxy().getProxyObject();
		}
//		if((int)stacks[0] == CONSTRUCT) {
//			return (RegisterDefinition) stacks[1];
//		}
		if((int)stacks[0] == FIELD) {
			return (T) stacks[2];
		}
		return null ; 
	}
	public static Method getMethod() {
		Object[] stacks = peek();
		if(stacks != null && (int)stacks[0] == METHOD) {
			return ((MethodHandler) stacks[1]).getMethod();
		}
		return null;
	}
	public static Constructor<?> getConstructor() {
		Object[] stacks = peek();
		if(stacks != null && (int)stacks[0] == CONSTRUCT) {
			return (Constructor<?>) stacks[3];
		}
		return null;
	}
	public static Field getField() {
		Object[] stacks = peek();
		if(stacks != null && (int)stacks[0] == FIELD) {
			return (Field) stacks[5];
		}
		return null;
	}
	public static Object[] getParameters() {
		Object[] stacks = peek();
		if(stacks == null)
			return null;
		if((int)stacks[0] == METHOD) {
			return ((MethodHandler) stacks[1]).getParameters();
		}
		if((int)stacks[0] == CONSTRUCT) {
			return (Object[]) stacks[4];
		}
//		if((int)stacks[0] == FIELD) {
//			return (T) stacks[2];
//		}
		return null ; 
	}
}
