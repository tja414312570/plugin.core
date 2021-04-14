package com.yanan.framework.transaction;

/**
 * 事物隔离级别
 * @author yanan
 *
 */
public enum TransactionIsolocation {
	/**
	 * 使用默认事物
	 */
	TRANSACTION_DEFAULT,
	/**
	 * 未提交读 :脏读，不可重复读，虚读都有可能发生 
	 */
	TRANSACTION_READ_UNCOMMITTED,
	/**
	 * 已提交读 :避免脏读。但是不可重复读和虚读有可能发生 
	 */
	TRANSACTION_READ_COMMITTED,
	/**
	 * 可重复读 :避免脏读和不可重复读.但是虚读有可能发生. 
	 */
	TRANSACTION_REPEATABLE_READ,
	/**
	 * 序列化 :避免以上所有读问题.
	 */
	TRANSACTION_SERIALIZABLE;
}