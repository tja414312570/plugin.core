package com.yanan.framework.transaction;

/**
 * 事物定义
 * @author yanan
 *
 */
public class TransactionDefined {
	/**
	 * 事物隔离级别
	 */
	private TransactionIsolocation transactionLevel;
	/**
	 * 事物传播级别
	 */
	private TransactionPropagion transactionPropagion;

	public TransactionIsolocation getTransactionLevel() {
		return transactionLevel;
	}

	public void setTransactionLevel(TransactionIsolocation transactionLevel) {
		this.transactionLevel = transactionLevel;
	}

	public TransactionPropagion getTransactionPropagion() {
		return transactionPropagion;
	}

	public void setTransactionPropagion(TransactionPropagion transactionPropagion) {
		this.transactionPropagion = transactionPropagion;
	}
	@Override
	public String toString() {
		return "TransactionDefined [transactionLevel=" + transactionLevel + ", transactionPropagion="
				+ transactionPropagion + "]";
	}
	public static TransactionDefined defaultTransactionDefined() {
		TransactionDefined transactionDefined = new TransactionDefined();
		transactionDefined.setTransactionLevel(TransactionIsolocation.TRANSACTION_DEFAULT);
		transactionDefined.setTransactionPropagion(TransactionPropagion.PROPAGATION_REQUIRED);
		return transactionDefined;
	}
}