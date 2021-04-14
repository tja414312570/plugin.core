package com.yanan.framework.transaction;

/**
 * 事物传播行为
 * @author yanan
 *
 */
public enum TransactionPropagion {
	//* 保证在同一个事务中 
	/**
	 * 支持当前事务，如果不存在 就新建一个(默认) 
	 */
	PROPAGATION_REQUIRED,
	/**
	 * 支持当前事务，如果不存在，就不使用事务 
	 */
	PROPAGATION_SUPPORTS,
	/**
	 *支持当前事务，如果不存在，抛出异常 
	 */
	PROPAGATION_MANDATORY,
	//* 保证没有在同一个事务中 
	/**
	 * 如果有事务存在，挂起当前事务，创建一个新的事务 
	 */
	PROPAGATION_REQUIRES,
	/**
	 * 以非事务方式运行，如果有事务存在，挂起当前事务 
	 */
	PROPAGATION_NOT_SUPPORTED,
	/**
	 * 以非事务方式运行，如果有事务存在，抛出异常 
	 */
	PROPAGATION_NEVER,
	/**
	 * 如果当前事务存在，则嵌套事务执行
	 */
	PROPAGATION_NESTED,
}