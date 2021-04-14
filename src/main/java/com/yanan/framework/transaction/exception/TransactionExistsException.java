package com.yanan.framework.transaction.exception;

public class TransactionExistsException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9135839010786489408L;
	public TransactionExistsException() {
		super("the current transaction propagation required PROPAGATION_NEVER but exists another transaction");
	}

}