package com.yanan.frame.plugin.builder.resolver;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import com.yanan.frame.plugin.annotations.Register;
import com.yanan.frame.plugin.definition.RegisterDefinition;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
/**
 * 日期参数解析
 * @author yanan
 *
 */
@Register(attribute="date")
public class DateParameterResolver implements ParameterResolver<ConfigValue>{
	private ZoneId zone = ZoneId.systemDefault();
	private String globalFormat = "yyyy-MM-dd HH:mm:ss";
	@Override
	public Object resove(ConfigValue configValue, String typeName, int parameterIndex,
			RegisterDefinition registerDefinition) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(globalFormat);
		if(configValue.valueType() == ConfigValueType.STRING) {
			LocalDateTime localDateTime = LocalDateTime.parse((CharSequence) configValue.unwrapped(),dtf);
		    Instant instant = localDateTime.atZone(zone).toInstant();
			return Date.from(instant);
		}
		LocalDateTime localDateTime = LocalDateTime.parse((CharSequence) configValue.unwrapped(),dtf);
	    Instant instant = localDateTime.atZone(zone).toInstant();
	    return Date.from(instant);
	}
}
