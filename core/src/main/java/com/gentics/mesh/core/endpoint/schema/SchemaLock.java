package com.gentics.mesh.core.endpoint.schema;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Container for the schema update mutex
 */
@Singleton
public class SchemaLock {

	private Object mutex = new Object();

	@Inject
	public SchemaLock() {

	}

	/**
	 * Return the mutex to be used for locks.
	 * 
	 * @return
	 */
	public Object mutex() {
		return mutex;
	}

}
