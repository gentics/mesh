package com.gentics.mesh.core.verticle.handler;

public interface WriteLock extends AutoCloseable {

	@Override
	void close();

	WriteLock lock();
}
