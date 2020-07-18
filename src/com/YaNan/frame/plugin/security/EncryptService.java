package com.yanan.frame.plugin.security;

import com.yanan.frame.plugin.annotations.Service;

@Service
public interface EncryptService {
	public Object encrypt(Object parameter,String...  arguments) throws Exception;
	public Object descrypt(Object parameter,String... arguments) throws Exception;
}
