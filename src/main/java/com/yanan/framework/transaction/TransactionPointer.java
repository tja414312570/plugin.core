package com.yanan.framework.transaction;

/**
 * 事物节点  暂未使用
 * @author yanan
 *
 */
public class TransactionPointer {
	/**
	 * 事物上级节点
	 */
	private int parentNode;
	/**
	 * 事物当前节点
	 */
	private int currentNode;
	public TransactionPointer(int parentNode, int currentNode) {
		super();
		this.parentNode = parentNode;
		this.currentNode = currentNode;
	}
	public int getParentNode() {
		return parentNode;
	}
	public void setParentNode(int parentNode) {
		this.parentNode = parentNode;
	}
	public int getCurrentNode() {
		return currentNode;
	}
	public void setCurrentNode(int currentNode) {
		this.currentNode = currentNode;
	}
	@Override
	public String toString() {
		return "TransactionPointer [parentNode=" + parentNode + ", currentNode=" + currentNode + "]";
	}
}