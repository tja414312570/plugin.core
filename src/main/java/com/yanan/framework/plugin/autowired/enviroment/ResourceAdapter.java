package com.yanan.framework.plugin.autowired.enviroment;

public interface ResourceAdapter<I,R> {
	R parse(I input);
}
