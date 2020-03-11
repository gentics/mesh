package com.gentics.mesh.core.verticle.handler;

/**
 * Locking mechanism to be used for write operations.
 */
public interface WriteLock extends AutoCloseable {

	@Override
	void close();

	/**
	 * Return the locked write lock.
	 * 
	 * @return Fluent API
	 */
	WriteLock lock();
}
