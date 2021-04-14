package com.yanan.framework.transaction;

import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yanan.framework.plugin.PlugsFactory;
import com.yanan.framework.transaction.exception.TransactionExistsException;
import com.yanan.framework.transaction.exception.TransactionIsNotExistsException;

/**
 * 事物管理
 * @author yanan
 *
 */
public class TransactionManager{
	private static Logger logger = LoggerFactory.getLogger(TransactionManager.class);
	/**
	 * 存储线程事物栈
	 */
	private static ThreadLocal<Stack<AbstractTransaction>> transactionStackLocal = new ThreadLocal<>();
	/**
	 * 事物挂载信息存储
	 * 
	 */
	private static ThreadLocal<AbstractTransaction> currentTransactionLocal = new ThreadLocal<>();
	private TransactionManager() {};
	
	public static AbstractTransaction createTransaction(TransactionDefined transactionDefined) {
		//获取事物栈
		Stack<AbstractTransaction> transactionManagerStack = transactionStackLocal.get();
		//如果栈为空  创建一个新的栈
		if(transactionManagerStack == null) {
			synchronized (TransactionManager.class) {
				if(transactionManagerStack == null) {
					transactionManagerStack = new Stack<>();
					transactionStackLocal.set(transactionManagerStack);
				}
			}
		}
		//获取当前事物
		AbstractTransaction currentTransaction = getCurrentTransaction();
		AbstractTransaction newTransaction ;
//		//* 保证在同一个事务中 
		switch (transactionDefined.getTransactionPropagion()) {
//				PROPAGATION_REQUIRED,// 支持当前事务，如果不存在 就新建一个(默认) 
				case PROPAGATION_REQUIRED:
					if(currentTransaction == null)
						newTransaction = createTransaction(transactionDefined,transactionManagerStack);
					else 
						currentTransaction.reference();
					break;
//					PROPAGATION_SUPPORTS,// 支持当前事务，如果不存在，就不使用事务 
				case PROPAGATION_SUPPORTS:
					if(currentTransaction != null)
						currentTransaction.reference();
					break;
//					PROPAGATION_MANDATORY,// 支持当前事务，如果不存在，抛出异常 
				case PROPAGATION_MANDATORY:
					if(currentTransaction == null)
						throw new TransactionIsNotExistsException();
					break;
		//			//* 保证没有在同一个事务中 
//		   		PROPAGATION_REQUIRES,//_NEW 如果有事务存在，挂起当前事务，创建一个新的事务 
			case PROPAGATION_REQUIRES:
				//创建新的事物
				newTransaction = createTransaction(transactionDefined,transactionManagerStack);
				//当前事物存在
				if(currentTransaction != null) 
					currentTransaction.addEmbedTransaction(newTransaction);
				break;
//				PROPAGATION_NOT_SUPPORTED,// 以非事务方式运行，如果有事务存在，挂起当前事务 
			case PROPAGATION_NOT_SUPPORTED:
				newTransaction = createTransaction(transactionDefined,transactionManagerStack);
				newTransaction.setEnable(false);
				//当前事物存在
				if(currentTransaction != null) 
					currentTransaction.addEmbedTransaction(newTransaction);
				break;
//				PROPAGATION_NEVER,// 以非事务方式运行，如果有事务存在，抛出异常 
			case PROPAGATION_NEVER:
				if(currentTransaction != null)
					throw new TransactionExistsException();
				break;
//				PROPAGATION_NESTED,// 如果当前事务存在，则嵌套事务执行
			case PROPAGATION_NESTED:
				newTransaction = createTransaction(transactionDefined,transactionManagerStack);
				//当前事物存在
				if(currentTransaction != null) 
					currentTransaction.addEmbedTransaction(newTransaction);
				break;
			default:
				break;
			}
		return getCurrentTransaction();
	}
	private static AbstractTransaction createTransaction(TransactionDefined transactionDefined, Stack<AbstractTransaction> transactionManagerStack) {
		//创建获取事物的实现类
		AbstractTransaction transaction = PlugsFactory.getPluginsInstanceAllowNull(AbstractTransaction.class,
				transactionDefined);
		//将事物添加到栈里
		transactionManagerStack.add(transaction);
		currentTransactionLocal.set(transaction);
		return transaction;
	}

	/**
	 * 获取栈顶事物管理
	 * @return 抽象事物
	 */
	public static AbstractTransaction getCurrentTransaction() {
		return currentTransactionLocal.get();
	}

	public static void hang(int hashCode, AbstractTransaction embedTransaction,
			AbstractTransaction transactionManager) {
		
	}

	public static void resetTransactionPointer() {
		AbstractTransaction currentTransaction = getCurrentTransaction();
		if(currentTransaction != null) {
			if(currentTransaction.getParentTransaction() != null) {
				currentTransactionLocal.set(currentTransaction.getParentTransaction());
			}else {
				for(AbstractTransaction transaction : transactionStackLocal.get()) {
					if(!transaction.isCompleted())
						transaction.commit();
				}
				logger.debug("all transaction completed at transaction "+currentTransaction);
				transactionStackLocal.remove();
				currentTransactionLocal.remove();
			}
		}
	}
	
}