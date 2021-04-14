package com.yanan.framework.transaction.exception;


public class TransactionIsNotExistsException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9135839010786489408L;
	public TransactionIsNotExistsException() {
		super("the current transaction propagation required PROPAGATION_MANDATORY but exists another transaction");
	}

}