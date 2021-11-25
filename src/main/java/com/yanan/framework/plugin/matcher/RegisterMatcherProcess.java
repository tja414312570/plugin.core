package com.yanan.framework.plugin.matcher;

import com.yanan.framework.plugin.PlugsFactory;
import com.yanan.framework.plugin.RegisterRefreshProcess;
import com.yanan.framework.plugin.annotations.Register;
import com.yanan.framework.plugin.definition.RegisterDefinition;
import com.yanan.utils.ArrayUtils;
import com.yanan.utils.reflect.ReflectUtils;
import com.yanan.utils.string.StringUtil;

@Register
public class RegisterMatcherProcess implements RegisterRefreshProcess {

	@Override
	public void process(PlugsFactory plugsFactory, RegisterDefinition currentRegisterDefinition) {
		if(!ReflectUtils.implementsOf(currentRegisterDefinition.getRegisterClass(), RegisterMatcher.class)){
			for(Class<?> serviceClass : currentRegisterDefinition.getServices()) {
				RegisterMatcher registerMatcher = PlugsFactory.getPluginsInstanceByAttributeStrictAllowNull(RegisterMatcher.class,serviceClass.getName());
				if(registerMatcher != null) {
					String[] attribute = currentRegisterDefinition.getAttribute();
					if(attribute.length == 1 && StringUtil.equals(attribute[0], "*")) {
						attribute = new String[0];
					}
					String[] parseAttribute = registerMatcher.attributes(currentRegisterDefinition.getRegisterClass());
					String[] newAttribute = ArrayUtils.megere(attribute, parseAttribute);
					currentRegisterDefinition.setAttribute(newAttribute);
				}
			}
		}
	}

}
