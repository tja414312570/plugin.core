package com.yanan.framework.transaction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.SQLException;

import com.yanan.framework.transaction.exception.TransactionsException;

/**
 * 事物注解
 * @author yanan
 *
 */
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Transactions {
	/**
	 * 回滚异常
	 * @return 异常上限类
	 */
	Class<?>[] value() default {SQLException.class,TransactionsException.class};
	
	/**
	 * 事物传播行为
	 * @return propagion 传播行为
	 */
	TransactionPropagion propagion() default TransactionPropagion.PROPAGATION_REQUIRED;
	
	/**
	 * 事物隔离级别
	 * @return isolocation 隔离级别
	 */
	TransactionIsolocation isolocation() default TransactionIsolocation.TRANSACTION_DEFAULT;
}