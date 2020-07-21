package com.yanan.frame.plugin.decoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.yanan.utils.resource.Resource;

/**
 * 标准扫描资源
 * @author yanan
 */
public class StandScanResource implements Resource {
	private String path;
	private String name;
	public StandScanResource(String path) {
		this.path = path;
		this.name = path;
	}
	@Override
	public String toString() {
		return "StandScanResource [path=" + path + ", name=" + name + "]";
	}
	@Override
	public String getPath() {
		return this.path;
	}

	@Override
	public boolean isDirect() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long lastModified() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long size() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<? extends Resource> listResource() {
		throw new UnsupportedOperationException();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getName() {
		return this.name;
	}

}
