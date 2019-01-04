package com.gentics.mesh.dagger;

import com.gentics.mesh.graphdb.spi.LegacyDatabase;

/**
 * Helper class to ease access to database supplier.
 */
public final class DB {

	public static LegacyDatabase get() {
		return MeshInternal.get().database();
	}

}
