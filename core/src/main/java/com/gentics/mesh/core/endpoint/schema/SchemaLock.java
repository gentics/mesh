package com.gentics.mesh.core.endpoint.schema;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SchemaLock {

	private Object mutex = new Object();

	@Inject
	public SchemaLock() {

	}

	public Object mutex() {
		return mutex;
	}

}
