package com.yanan.framework.transaction;

import java.util.Stack;

import com.yanan.framework.plugin.annotations.Service;

/**
 * 抽象事物，提供事物的基本方法，定义事物执行规则和逻辑等
 * @author yanan
 *
 */
@Service
public abstract class AbstractTransaction {
	/**
	 * 事物定义
	 */
	protected TransactionDefined transactionDefined;
	/**
	 * 内部事物栈
	 */
	private Stack<AbstractTransaction> embedTransactionStack;
	/**
	 * 父事物
	 */
	private AbstractTransaction parentTransaction;
	/**
	 * 此事物是否启用
	 */
	private boolean enable = true;
	/**
	 * 事物是否完成
	 */
	private boolean completed;
	/**
	 * 事物引用次数
	 */
	private int reference = 1;
	public AbstractTransaction(TransactionDefined transactionDefined) {
		this.transactionDefined = transactionDefined;
	}
	/**
	 * 事物提交
	 */
	public abstract void commit();
	/**
	 * 事物回滚
	 */
	public abstract void rollback();
	/**
	 *事物管理对象
	 * @param managerObject
	 */
	public abstract void manager(Object managerObject);
	public TransactionDefined getTransactionDefined() {
		return transactionDefined;
	}
	public void setTransactionDefined(TransactionDefined transactionDefined) {
		this.transactionDefined = transactionDefined;
	}
	public void addEmbedTransaction(AbstractTransaction embedTransaction) {
		if(embedTransactionStack == null)
			embedTransactionStack = new Stack<>();
		embedTransactionStack.add(embedTransaction);
		embedTransaction.setParentTransaction(this);
	}
	public void setParentTransaction(AbstractTransaction parentTransaction) {
		this.parentTransaction  = parentTransaction;
	}
	public void setEnable(boolean enable) {
		this.enable  = enable;
	}
	public Stack<AbstractTransaction> getEmbedTransactionStack() {
		return embedTransactionStack;
	}
	public void setEmbedTransactionStack(Stack<AbstractTransaction> embedTransactionStack) {
		this.embedTransactionStack = embedTransactionStack;
	}
	public AbstractTransaction getParentTransaction() {
		return parentTransaction;
	}
	public boolean isEnable() {
		return enable;
	}
	public boolean isCompleted() {
		return completed;
	}
	public void reference() {
		this.reference++;
	}
	public int getReference() {
		return reference;
	}
	protected boolean checkReference() {
		return --reference <= 0;
	}
	public void completedCommit() {
		completed = true;
		if(embedTransactionStack!= null)
		for(AbstractTransaction transaction : embedTransactionStack) {
			if(!transaction.isCompleted()) {
				transaction.commit();
				transaction.completedCommit();
			}
		}
	}
	public void completedRollback() {
		completed = true;
		if(embedTransactionStack!= null)
		for(AbstractTransaction transaction : embedTransactionStack) {
			if(!transaction.isCompleted()) {
				transaction.rollback();
				transaction.completedRollback();
			}
				
		}
	}
}