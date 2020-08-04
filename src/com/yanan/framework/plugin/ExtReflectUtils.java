package com.yanan.framework.plugin;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import com.yanan.framework.plugin.annotations.Service;
import com.yanan.framework.plugin.definition.ConstructorDefinition;
import com.yanan.framework.plugin.exception.PluginInitException;
import com.yanan.framework.plugin.exception.PluginRuntimeException;
import com.yanan.utils.ArrayUtils;
import com.yanan.utils.asserts.Assert;
import com.yanan.utils.reflect.ParameterUtils;
import com.yanan.utils.reflect.ReflectUtils;
import com.yanan.utils.reflect.cache.ClassHelper;
import com.yanan.utils.string.StringUtil;

/**
 * 参数工具，主要用于查找方法和构造器 因为我们从配置文件拿到的数据类型往往不是我们希望的类型 我们通过对参数的类型进行匹配来获取一个有效调用的方法或构造器
 * 比如 {int.class,Integer.class,short.class,Short.class}是可以匹配的（null除外）
 * 
 * @author yanan
 *
 */
public class ExtReflectUtils {
	public static final String CONSTRUCTOR = " constructor ";
	public static final String METHOD = " method ";

	/**
	 * 获取参数的类型
	 * 
	 * @param parmType 参数类型字符串
	 * @return 参数类型
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
	 * @param constructorList 构造器列表
	 * @param values          参数列表
	 * @return 构造器
	 */
	public static Constructor<?> getEffectiveConstructor(List<Constructor<?>> constructorList,
			List<? extends Object> values) {
		Constructor<?> constructor = null;
		// 遍历所有的构造器
		con: for (Constructor<?> cons : constructorList) {
			// 获取构造器的参数类型的集合
			Class<?>[] parameterType = cons.getParameterTypes();
			if (values.size() != cons.getParameterCount())
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
	 * 获取一个有效的构造器并将构造器放入到构造器定义中
	 * <p>
	 * 主要用于处理内部非静态类的问题
	 * 
	 * @param constructorDefinition 构造器器定义
	 * @param targetClass           目标类
	 */
	public static void getEffectiveConstructor(ConstructorDefinition constructorDefinition, Class<?> targetClass) {
		Constructor<?>[] constructors = targetClass.getDeclaredConstructors();
		System.out.println(Arrays.toString(constructors));
		//内部类没有static和public修饰
		if (!Modifier.isStatic(targetClass.getModifiers()) && !Modifier.isPublic(targetClass.getModifiers())
				//内部类为非匿名内部类
				&& StringUtil.isNotEmpty(targetClass.getSimpleName())) {
			Class<?> outClass;
			try {
				
				outClass = ReflectUtils.getOuterClass(targetClass);
				Object outIns = PlugsFactory.getPluginsInstance(outClass);
				// 将第一参数设置为实例类
				Object[] args = constructorDefinition.getArgs();
				args = ArrayUtils.add(args, outIns, 0);
				constructorDefinition.setArgs(args);
				// 将第一参数类型设置为父类
				Class<?>[] types = constructorDefinition.getArgsType();
				types = ArrayUtils.add(types, outClass, 0);
				constructorDefinition.setArgsType(types);
			} catch (ClassNotFoundException e) {
				throw new PluginRuntimeException("failed to get out class for " + targetClass.getName(), e);
			}
		}
		Constructor<?> constructor = getEffectiveConstructor(constructors, constructorDefinition.getArgsType());
		if (constructor == null) {
			StringBuilder errorMsg = buildNoSuchMsg(targetClass, CONSTRUCTOR, constructorDefinition.getArgsType());
			throw new NoSuchMethodError(errorMsg.toString());
		}
		constructorDefinition.setConstructor(constructor);
	}

	/**
	 * 构建没有方法或构造器的异常信息
	 * 
	 * @param targetClass 目标类
	 * @param type        说明
	 * @param types       参数类型
	 * @return 构造的消息
	 */
	public static StringBuilder buildNoSuchMsg(Class<?> targetClass, String type, Class<?>[] types) {
		StringBuilder errorMsg = new StringBuilder("cloud not found an effective").append(type)
				.append(targetClass.getName()).append(".").append(targetClass.getSimpleName()).append("(");
		for (int i = 0; i < types.length; i++) {
			errorMsg.append(types[i].getName());
			if (i < types.length - 1)
				errorMsg.append(",");
		}
		errorMsg.append(")");
		return errorMsg;
	}

	/**
	 * 获取一个有效的构造器
	 * 
	 * @param constructorList 构造器列表
	 * @param parameterTypes  参数类型列表
	 * @return 找到的构造器
	 */
	public static Constructor<?> getEffectiveConstructor(Constructor<?>[] constructorList, Class<?>[] parameterTypes) {
		// 遍历所有的构造器
		con: for (Constructor<?> cons : constructorList) {
			if ((parameterTypes == null && cons.getParameterCount() == 0)
					|| parameterTypes.length != cons.getParameterCount())
				continue con;
			// 获取构造器的参数类型的集合
			Class<?>[] argsTypes = cons.getParameterTypes();
			// 遍历构造器
			for (int i = 0; i < argsTypes.length; i++) {
				if (parameterTypes[i] == null && !ParameterUtils.isBaseUnwrapperType(argsTypes[i]))
					continue;
				Class<?> argType = argsTypes[i];
				Class<?> parameterType = parameterTypes[i];
				if (!argType.equals(parameterType) && !ReflectUtils.extendsOf(parameterType, argType)
						&& !ReflectUtils.implementsOf(parameterType, argType))
					continue con;
			}
			return cons;
		}
		return null;
	}

	/**
	 * 获取一个有效的构造器
	 * 
	 * @param targetClass 要查找的类
	 * @param argsTypes   参数类型
	 * @return 找到的构造器
	 */
	public static Constructor<?> getEffectiveConstructor(Class<?> targetClass, Class<?>[] argsTypes) {
		Constructor<?>[] constructors = targetClass.getConstructors();
		for (Constructor<?> constructor : constructors) {
			if (constructor.getParameterCount() == argsTypes.length
					&& isEffectiveTypes(constructor.getParameterTypes(), argsTypes))
				return constructor;
		}
		StringBuilder errorMsg = buildNoSuchMsg(targetClass, CONSTRUCTOR, argsTypes);
		throw new NoSuchMethodError(errorMsg.toString());
	}

	/**
	 * 获取一个合适的方法。匹配规则是参数可以转换为对应的参数
	 * 
	 * @param methods    方法集合
	 * @param parameters 参数集合
	 * @return 找到的方法
	 */
	public static Method getEffectiveMethod(Method[] methods, Object[] parameters) {
		Method method = null;
		// 遍历所有的构造器
		con: for (Method cons : methods) {
			if (cons.getParameterCount() != parameters.length)
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
	 * 
	 * @param methods        方法集合
	 * @param parameterTypes 参数类型集合
	 * @return 匹配的合适的方法
	 */
	public static Method getEffectiveMethod(Method[] methods, Class<?>[] parameterTypes) {
		Method method = null;
		// 遍历所有的构造器
		con: for (Method cons : methods) {
			if (cons.getParameterCount() != parameterTypes.length)
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

	/**
	 * 查找一个有效的方法
	 * 
	 * @param targetClass 目标类
	 * @param methodName  方法名称
	 * @param argsTypes   参数类型
	 * @return 方法
	 * @throws NoSuchMethodException 没有找到方法异常
	 */
	public static Method getEffectiveMethod(Class<?> targetClass, String methodName, Class<?>[] argsTypes)
			throws NoSuchMethodException {
		Method[] methods = ClassHelper.getClassHelper(targetClass).getMethods();
		for (Method method : methods) {
			if (method.getName().equals(methodName)) {
				if (method.getParameterCount() == argsTypes.length
						&& isEffectiveTypes(method.getParameterTypes(), argsTypes))
					return method;
			}
		}
		StringBuilder errorMsg = buildNoSuchMsg(targetClass, METHOD, argsTypes);
		throw new NoSuchMethodException(errorMsg.toString());
	}

	private static boolean isEffectiveTypes(Class<?>[] parameterTypes, Class<?>[] argsTypes) {
		for (int i = 0; i < parameterTypes.length; i++) {
			if (!isEffectiveType(parameterTypes[i], argsTypes[i]))
				return false;
		}
		return true;
	}

	/**
	 * 判断两个类型是否匹配
	 * 
	 * @param type      待匹配的类型
	 * @param valueType 匹配的类型
	 * @return 是否匹配
	 */
	public static boolean isEffectiveType(Class<?> type, Class<?> valueType) {
		if (valueType.isArray() && type.isArray()) {
			valueType = ReflectUtils.getArrayType(valueType);
			type = ReflectUtils.getArrayType(type);
		}
		if (type.equals(valueType) || ReflectUtils.extendsOf(valueType, type)
				|| ReflectUtils.implementsOf(valueType, type))
			return true;
		if (type == byte.class) {
			return Assert.equalsAny(valueType, byte.class, Byte.class);
		}
		if (type == short.class) {
			return Assert.equalsAny(valueType, short.class, Short.class);
		}
		if (type == int.class) {
			return Assert.equalsAny(valueType, int.class, Integer.class);
		}
		if (type == long.class) {
			return Assert.equalsAny(valueType, long.class, Long.class);
		}
		if (type == float.class) {
			return Assert.equalsAny(valueType, float.class, Float.class);
		}
		if (type == double.class) {
			return Assert.equalsAny(valueType, double.class, Double.class);
		}
		if (type == boolean.class) {
			return Assert.equalsAny(valueType, boolean.class, Boolean.class);
		}
		if (type == char.class) {
			return Assert.equalsAny(valueType, char.class, Character.class);
		}
		return false;
	}

	/**
	 * 判断参数和类型是否匹配
	 * 
	 * @param type  目标类型
	 * @param value 值类型
	 * @return 是否有效
	 */
	public static boolean isEffectiveParameter(Class<?> type, Object value) {
		if (value == null && Assert.equalsAny(type, int.class, long.class, short.class, boolean.class, float.class,
				double.class, byte.class, char.class)) {
			return false;
		}
		return isEffectiveType(type, value.getClass());
	}

}