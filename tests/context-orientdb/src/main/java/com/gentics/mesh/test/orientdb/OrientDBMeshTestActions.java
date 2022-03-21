package com.gentics.mesh.test.orientdb;

import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.test.MeshTestActions;

/**
 * OrientDB implementation of {@link MeshTestActions}.
 * 
 * @author plyhun
 *
 */
class OrientDBMeshTestActions implements MeshTestActions {

	OrientDBMeshTestActions() {
		
	}

	/**
	 * OrientDB needs no extra actions on schema version entity update, so returns back the argument.
	 */
	@Override
	public <SCV extends HibFieldSchemaVersionElement<?, ?, ?, ?, ?>> SCV updateSchemaVersion(SCV version) {
		return version;
	}
}
