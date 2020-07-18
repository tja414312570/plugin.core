package com.yanan.frame.plugin.security;

import com.yanan.frame.plugin.annotations.Register;
import com.yanan.frame.plugin.annotations.Support;
import com.yanan.frame.plugin.handler.InvokeHandler;
import com.yanan.frame.plugin.handler.MethodHandler;
import com.yanan.frame.plugin.security.Encrypt;
import com.yanan.frame.plugin.security.EncryptService;
import com.yanan.frame.plugin.PlugsFactory;

@Support(Encrypt.class)
@Register(signlTon = true)
public class EncryptHandler implements InvokeHandler {
	/**
	 * 加密类实现类可以定义为单例
	 */
	private EncryptService encryptService = PlugsFactory.getPluginsInstance(EncryptService.class);

	@Override
	public void before(MethodHandler methodHandler) {
		Object[] parameters = methodHandler.getParameters();
		try {
			for (int i = 0; i < parameters.length; i++)
				parameters[i] = encryptService.descrypt(parameters[i]);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void after(MethodHandler methodHandler) {
		Object result = methodHandler.getOriginResult();
		try {
			methodHandler.interrupt(encryptService.encrypt(result));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void error(MethodHandler methodHandler, Throwable e) {
	}

}
