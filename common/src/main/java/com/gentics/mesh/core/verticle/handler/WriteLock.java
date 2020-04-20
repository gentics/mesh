package com.gentics.mesh.core.verticle.handler;

import com.gentics.mesh.context.InternalActionContext;

/**
 * Locking mechanism to be used for write operations.
 */
public interface WriteLock extends AutoCloseable {

	public static final String GLOBAL_LOCK_KEY = "MESH_GLOBAL_LOCK";

	@Override
	void close();

	/**
	 * Return the write lock that is configured according to the provided context.
	 * 
	 * @param ac
	 * @return Fluent API
	 */
	WriteLock lock(InternalActionContext ac);

}
