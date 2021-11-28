package com.yanan.framework.plugin.autowired.enviroment;

import java.lang.reflect.Field;
import java.util.List;
import com.yanan.framework.plugin.InstanceBeforeProcesser;
import com.yanan.framework.plugin.PlugsFactory;
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
				if (field.getAnnotation(Variable.class) == null) {
					fieldName = name + "." + field.getName();
					List<VariableSupporter> variableSupporters = PlugsFactory
							.getPluginsInstanceList(VariableSupporter.class);
					for (VariableSupporter variableSupporter : variableSupporters) {
						value = variableSupporter.getVariable(fieldName, field.getType(), variable, field);
						if (value != null)
							break;
					}
					if (value == null)
						continue;
					try {
						ReflectUtils.setFieldValue(field, instances, value);
					} catch (IllegalArgumentException | IllegalAccessException e) {
						throw new VariableAutowiredFailedException(e);
					}
				}
			}
		}
		return instances;
	}

}
