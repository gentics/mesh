package com.gentics.mesh.dagger;

import com.gentics.mesh.graphdb.spi.Database;

/**
 * Helper class to ease access to database supplier.
 */
public final class DB {

	public static Database get() {
		return MeshInternal.get().database();
	}

}
