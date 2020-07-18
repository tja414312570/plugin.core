package com.yanan.frame.plugin.autowired.property;

public class PropertyAutowiredFailedException extends RuntimeException {

	public PropertyAutowiredFailedException(Exception e) {
		super(e);
	}

	public PropertyAutowiredFailedException(String msg, Exception e) {
		super(msg,e);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -3736651097787503692L;

}
