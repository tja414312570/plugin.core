package com.yanan.framework.transaction.exception;

import java.sql.SQLException;

public class TransactionExecuteException extends RuntimeException {

	public TransactionExecuteException(SQLException e) {
		super(e);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1110864830345748326L;
	
}