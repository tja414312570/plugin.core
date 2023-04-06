package com.yanan.framework.transaction;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yanan.framework.plugin.annotations.Register;
import com.yanan.framework.plugin.annotations.Support;
import com.yanan.framework.plugin.handler.InvokeHandler;
import com.yanan.framework.plugin.handler.MethodHandler;
import com.yanan.utils.reflect.ReflectUtils;

/**
 * 注解事物支持方法切面控制器
 * @author yanan
 *
 */
@Support(Transactions.class)
@Register
public class TransactionMethodHandler implements InvokeHandler {
	private Logger logger = LoggerFactory.getLogger(TransactionMethodHandler.class);

	@Override
	public Object around(MethodHandler methodHandler) throws Throwable {
		//获取事物注解
		Transactions transactions = methodHandler.getMethod().getAnnotation(Transactions.class);
		//创建事物定义
		TransactionDefined transactionDefined = new TransactionDefined();
		transactionDefined.setTransactionLevel(transactions.isolocation());
		transactionDefined.setTransactionPropagion(transactions.propagion());
		//创建事物
		TransactionManager.createTransaction(transactionDefined);
		logger.debug("create transaction at execute method ["+methodHandler.getMethod()+"] transaction ["+TransactionManager.getCurrentTransaction()+"]");
		Object result = null ;
		try {
			result = methodHandler.invoke();
		}catch (Throwable t) {
			AbstractTransaction transactionManager = TransactionManager.getCurrentTransaction();
			logger.debug("transaction execute exception when execute ["+methodHandler.getMethod().getName()+"] transaction ["+transactionManager+"]",t);
			boolean rollback = false;
			//判断是否需要回滚
			for(Class<?> clzz : transactions.value()) {
				if(ReflectUtils.extendsOf(t.getClass(), clzz)) {
					if(transactionManager.checkReference()) {
						logger.debug("transaction rollback when execute ["+methodHandler.getMethod().getName()+"] transaction ["+transactionManager+"]");
						rollback = true;
						if(transactionManager.getTransactionDefined().getTransactionPropagion() != TransactionPropagion.PROPAGATION_NESTED) {
							transactionManager.rollback();
							transactionManager.completedRollback();
						}
						TransactionManager.resetTransactionPointer();
					}
					break;
				}
			}
			//如果没有回滚，走提交
			if(!rollback) {
				if(transactionManager.checkReference()) {
					if(transactionManager.getTransactionDefined().getTransactionPropagion() != TransactionPropagion.PROPAGATION_NESTED) {
						transactionManager.commit();
						transactionManager.completedCommit();
					}
					TransactionManager.resetTransactionPointer();
					logger.debug("Transaction not rollback because exception type is not @Transaction declare type "+Arrays.toString(transactions.value())+" or child's . target " + t.getClass());
				}
			}
			//如果是独立的事物，中断异常
			if(transactionManager.getTransactionDefined().getTransactionPropagion() == TransactionPropagion.PROPAGATION_REQUIRES) {
				methodHandler.interrupt(null);
			}
			throw t;
		}
		logger.debug("transaction execute completed at method ["+methodHandler.getMethod().getName()+"] transaction ["+TransactionManager.getCurrentTransaction()+"]");
		AbstractTransaction transactionManager = TransactionManager.getCurrentTransaction();
		if(transactionManager.checkReference()) {
			//嵌套事物不需要自动提交
			if(transactionManager.getTransactionDefined().getTransactionPropagion() != TransactionPropagion.PROPAGATION_NESTED) {
				transactionManager.commit();
				transactionManager.completedCommit();
			}
			TransactionManager.resetTransactionPointer();
		}
		
		return result;
	}
}