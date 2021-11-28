package com.yanan.framework.plugin.autowired.enviroment;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import com.yanan.framework.plugin.InstanceBeforeProcesser;
import com.yanan.framework.plugin.annotations.Register;
import com.yanan.framework.plugin.definition.RegisterDefinition;
import com.yanan.utils.reflect.ReflectUtils;
import com.yanan.utils.string.StringUtil;

@Register
public class VariableBeanProcess implements InstanceBeforeProcesser {

	@Override
	public Object before(RegisterDefinition registerDefinition, Class<?> serviceClasss, Object instances) {
		Class<?> clzz = registerDefinition.getRegisterClass();
		Variable variable = clzz.getAnnotation(Variable.class);
		if (variable != null) {
			String name = variable.value();
			if (StringUtil.isEmpty(name)) {
				name = clzz.getSimpleName().toLowerCase();
			}
			Field[] fields = ReflectUtils.getAllFields(clzz);
			String fieldName;
			Object value = null;
			for (Field field : fields) {
				value = null;
				if (field.getAnnotation(Variable.class) == null) {
					fieldName = name + "." + field.getName();
					try {
						value = VariableProcesser.getVariable(fieldName, field.getType(), variable, field,instances);
					}catch(VariableAutowiredFailedException e) {
						
					}
					if (value == null)
						continue;
					try {
						ReflectUtils.setFieldValue(field, instances, value);
					} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
						throw new VariableAutowiredFailedException(e);
					}
				}
			}
		}
		return instances;
	}

}
