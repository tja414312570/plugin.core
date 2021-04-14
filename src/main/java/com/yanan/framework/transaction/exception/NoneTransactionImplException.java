package com.yanan.framework.transaction.exception;

public class NoneTransactionImplException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4299259146629706397L;
	public NoneTransactionImplException(String msg) {
		super(msg);
	}
	public NoneTransactionImplException() {
		super("could not found transaction implements , create transaction failed");
	}
}