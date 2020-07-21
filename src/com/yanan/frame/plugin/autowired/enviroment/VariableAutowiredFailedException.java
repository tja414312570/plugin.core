package com.yanan.frame.plugin.autowired.enviroment;

public class VariableAutowiredFailedException extends RuntimeException {

	public VariableAutowiredFailedException(Exception e) {
		super(e);
	}

	public VariableAutowiredFailedException(String msg, Exception e) {
		super(msg,e);
	}

	public VariableAutowiredFailedException(String msg) {
		super(msg);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -3736651097787503692L;

}
