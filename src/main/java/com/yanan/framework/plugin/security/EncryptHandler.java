package com.yanan.framework.plugin.security;

import com.yanan.framework.plugin.PlugsFactory;
import com.yanan.framework.plugin.annotations.Register;
import com.yanan.framework.plugin.annotations.Support;
import com.yanan.framework.plugin.handler.InvokeHandler;
import com.yanan.framework.plugin.handler.MethodHandler;

@Support(Encrypt.class)
@Register(signlTon = true)
public class EncryptHandler implements InvokeHandler {
	/**
	 * 加密类实现类可以定义为单例
	 */
	private EncryptService encryptService = PlugsFactory.getPluginsInstance(EncryptService.class);

	@Override
	public Object around(MethodHandler methodHandler) throws Throwable {
		Object[] parameters = methodHandler.getParameters();
			for (int i = 0; i < parameters.length; i++)
				parameters[i] = encryptService.descrypt(parameters[i]);
		Object result = methodHandler.invoke();
		return encryptService.encrypt(result);
	}

}