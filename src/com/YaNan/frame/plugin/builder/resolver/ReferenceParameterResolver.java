package com.yanan.frame.plugin.builder.resolver;

import com.yanan.frame.plugin.annotations.Register;
import com.yanan.frame.plugin.definition.MethodDefinition;
import com.yanan.frame.plugin.definition.RegisterDefinition;
import com.typesafe.config.ConfigValue;
import com.yanan.frame.plugin.PlugsFactory;
import com.yanan.frame.plugin.ProxyModel;

/**
 * 引用参数解析
 * @author yanan
 */
@Register(attribute= {"ref","reference"},register = {ParameterResolver.class,DelayParameterResolver.class},model=ProxyModel.CGLIB)
public class ReferenceParameterResolver implements DelayParameterResolver<ConfigValue>{
	@Override
	public Class<?> parameterType(ConfigValue configValue,String methodName,Class<?>[] argsTypes, String typeName,int parameterIndex, RegisterDefinition registerDefinition) {
		RegisterDefinition refDefinition = PlugsFactory.getInstance().getRegisterDefinition((String)configValue.unwrapped());
		MethodDefinition methodDefinition = refDefinition.getInstanceMethod();
		if(methodDefinition != null)
			return methodDefinition.getMethod().getReturnType();
		return refDefinition.getRegisterClass();
	}

	@Override
	public Object resove(ConfigValue configValue, String type, int index, RegisterDefinition registerDefinition) {
		return PlugsFactory.getPluginsInstance((String) configValue.unwrapped());
	}
}
