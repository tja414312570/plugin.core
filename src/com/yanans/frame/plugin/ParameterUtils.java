package com.yanan.frame.plugin;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import com.yanan.frame.plugin.annotations.Service;
import com.yanan.frame.plugin.exception.PluginInitException;
import com.yanan.utils.asserts.Assert;
import com.yanan.utils.reflect.AppClassLoader;
import com.yanan.utils.reflect.cache.ClassHelper;

public class ParameterUtils {
	public static class MethodDesc {
		private Method method;
		private Object[] parameter;

		public MethodDesc(Method method, Object[] parameter) {
			super();
			this.method = method;
			this.parameter = parameter;
		}

		public Method getMethod() {
			return method;
		}

		public void setMethod(Method method) {
			this.method = method;
		}

		public Object[] getParameter() {
			return parameter;
		}

		public void setParameter(Object[] parameter) {
			this.parameter = parameter;
		}
	}

	/**
	 * 获取参数的类型
	 * 
	 * @param parmType
	 * @return
	 */
	public static Class<?> getParameterType(String parmType) {
		parmType = parmType.trim();
		switch (parmType.toLowerCase()) {
		case "string":
			return String.class;
		case "int":
			return int.class;
		case "integer":
			return int.class;
		case "float":
			return float.class;
		case "double":
			return double.class;
		case "boolean":
			return boolean.class;
		case "file":
			return File.class;
		case "ref":
			return Service.class;
		}
		try {
			return Class.forName(parmType);
		} catch (ClassNotFoundException e) {
			throw new PluginInitException(e);
		}
	}

	/**
	 * 获取一个有效的构造器
	 * 
	 * @param constructorList
	 * @param values
	 * @return
	 */
	public static Constructor<?> getEffectiveConstructor(List<Constructor<?>> constructorList,
			List<? extends Object> values) {
		Constructor<?> constructor = null;
		// 遍历所有的构造器
		con: for (Constructor<?> cons : constructorList) {
			// 获取构造器的参数类型的集合
			Class<?>[] parameterType = cons.getParameterTypes();
			if(values.size() != cons.getParameterCount())
				continue con;
			// 遍历构造器
			for (int i = 0; i < parameterType.length; i++) {
				Class<?> type = parameterType[i];
				Object value = values.get(i);
				if (!isEffectiveParameter(type, value)) {
					continue con;
				}
			}
			constructor = cons;
		}
		return constructor;
	}

	/**
	 * 获取一个有效的构造器
	 * 
	 * @param constructorList
	 * @param values
	 * @return
	 */
	public static Constructor<?> getEffectiveConstructor(Constructor<?>[] constructorList,
			Class<?>[] parameterTypes) {
		Constructor<?> constructor = null;
		// 遍历所有的构造器
		con: for (Constructor<?> cons : constructorList) {
			if((parameterTypes == null && cons.getParameterCount() == 0) 
					|| parameterTypes.length != cons.getParameterCount())
				continue con;
			// 获取构造器的参数类型的集合
			Class<?>[] argsTypes = cons.getParameterTypes();
			// 遍历构造器
			for (int i = 0; i < argsTypes.length; i++) {
				if(parameterTypes[i] == null)
					continue;
				Class<?> argType = argsTypes[i];
				Class<?> parameterType = parameterTypes[i];
				if (!argType.equals(parameterType) && !AppClassLoader.extendsOf(parameterType,argType)
						&& !AppClassLoader.implementsOf(parameterType,argType))
					continue con;
				}
			return cons;
		}
		return constructor;
	}
	/**
	 * 获取一个有效的构造器
	 * @param targetClass
	 * @param argsTypes
	 * @return
	 */
	public static Constructor<?> getEffectiveConstructor(Class<?> targetClass, Class<?>[] argsTypes) {
		Constructor<?>[] constructors = targetClass.getConstructors();
		for(Constructor<?> constructor : constructors) {
			if(constructor.getParameterCount() == argsTypes.length 
					&& isEffectiveTypes(constructor.getParameterTypes(),argsTypes))
				return constructor;
		}
		StringBuilder errorMsg = new StringBuilder("cloud not found an effective constructor ");
		errorMsg.append(targetClass.getName()).
		append(".").
		append(targetClass.getSimpleName()).
		append("(");
		for(int i = 0;i<argsTypes.length;i++) {
			errorMsg.append(argsTypes[i].getName());
			if(i<argsTypes.length-1)
				errorMsg.append(",");
		}
		errorMsg.append(")");
		throw new NoSuchMethodError(errorMsg.toString());
	}
	/**
	 * 获取一个合适的方法。匹配规则是参数可以转换为对应的参数
	 * @param methods
	 * @param parameters
	 * @return
	 */
	public static Method getEffectiveMethod(Method[] methods,
			Object[] parameters) {
		Method method = null;
		// 遍历所有的构造器
		con: for (Method cons : methods) {
			if(cons.getParameterCount()!=parameters.length)
				continue con;
			// 获取构造器的参数类型的集合
			Class<?>[] parameterType = cons.getParameterTypes();
			// 遍历构造器
			for (int i = 0; i < parameterType.length; i++) {
				Class<?> type = parameterType[i];
				Object value = parameters[i];
				if (!isEffectiveParameter(type, value)) {
					continue con;
				}
			}
			method = cons;
		}
		return method;
	}
	/**
	 * 获取一个合适的方法。匹配规则是参数可以转换为对应的参数
	 * @param methods
	 * @param parameters
	 * @return
	 */
	public static Method getEffectiveMethod(Method[] methods,
			Class<?>[] parameterTypes) {
		Method method = null;
		// 遍历所有的构造器
		con: for (Method cons : methods) {
			if(cons.getParameterCount()!=parameterTypes.length)
				continue con;
			// 获取构造器的参数类型的集合
			Class<?>[] parameterTypeInMethod = cons.getParameterTypes();
			// 遍历构造器
			for (int i = 0; i < parameterTypeInMethod.length; i++) {
				Class<?> currentMethodIndexType = parameterTypeInMethod[i];
				Class<?> currentParameterIndexType = parameterTypes[i];
				if (!isEffectiveType(currentMethodIndexType, currentParameterIndexType)) {
					continue con;
				}
			}
			method = cons;
		}
		return method;
	}
	public static Method getEffectiveMethod(Class<?> targetClass, String methodName, Class<?>[] argsTypes) throws NoSuchMethodException {
		Method[] methods = ClassHelper.getClassHelper(targetClass).getMethods();
		for(Method method : methods) {
			if(method.getName().equals(methodName) ) {
				if(method.getParameterCount() == argsTypes.length 
						&& isEffectiveTypes(method.getParameterTypes(),argsTypes))
					return method;
			}
		}
		StringBuilder errorMsg = new StringBuilder("cloud not found an effective method ");
		errorMsg.append(targetClass.getName()).
		append(".").
		append(methodName).
		append("(");
		for(int i = 0;i<argsTypes.length;i++) {
			errorMsg.append(argsTypes[i].getName());
			if(i<argsTypes.length-1)
				errorMsg.append(",");
		}
		errorMsg.append(")");
		throw new NoSuchMethodException(errorMsg.toString());
	}
	private static boolean isEffectiveTypes(Class<?>[] parameterTypes, Class<?>[] argsTypes) {
		for(int i = 0 ;i < parameterTypes.length;i++) {
			if(!isEffectiveType(parameterTypes[i], argsTypes[i]))
				return false;
		}
		return true;
	}

	/**
	 * 判断两个类型是否匹配
	 * @param type
	 * @param valueType
	 * @return
	 */
	public static boolean isEffectiveType(Class<?> type, Class<?> valueType) {
		if(valueType.isArray() && type.isArray()) {
			valueType = AppClassLoader.getArrayType(valueType);
			type = AppClassLoader.getArrayType(type);
		}
		if (type.equals(valueType) || AppClassLoader.extendsOf(valueType, type)
				|| AppClassLoader.implementsOf(valueType, type))
			return true;
		if (type == byte.class) {
			return Assert.equalsAny(valueType, byte.class,Byte.class);
		} 
		if (type == short.class) {
			return Assert.equalsAny(valueType, short.class,Short.class);
		} 
		if (type == int.class) {
			return Assert.equalsAny(valueType, int.class,Integer.class);
		} 
		if (type == long.class) {
			return Assert.equalsAny(valueType, long.class,Long.class);
		} 
		if (type == float.class ) {
			return Assert.equalsAny(valueType, float.class,Float.class);
		}
		if (type == double.class ) {
			return Assert.equalsAny(valueType, double.class,Double.class);
		}
		if (type == boolean.class ) {
			return Assert.equalsAny(valueType, boolean.class,Boolean.class);
		}
		if (type == char.class ) {
			return Assert.equalsAny(valueType, char.class,Character.class);
		}
		return false;
	}
	/**
	 * 判断参数和类型是否匹配
	 * @param type
	 * @param value
	 * @return
	 */
	public static boolean isEffectiveParameter(Class<?> type, Object value) {
		if (value == null && Assert.equalsAny(type,
				int.class,long.class,short.class,
				boolean.class,
				float.class,double.class,
				byte.class,char.class)) {
			return false;
		}
		return isEffectiveType(type, value.getClass());
	}
}
