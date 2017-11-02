package com.gentics.mesh.core.data.asset.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BINARY;

import com.gentics.mesh.core.data.asset.Binary;
import com.gentics.mesh.core.data.asset.BinaryRoot;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * @see BinaryRoot
 */
public class BinaryRootImpl extends MeshVertexImpl implements BinaryRoot {

	public static void init(Database database) {
		database.addVertexType(BinaryRootImpl.class, MeshVertexImpl.class);
		database.addEdgeIndex(HAS_BINARY, true, false, true);
	}

	@Override
	public Database database() {
		return MeshInternal.get().database();
	}

	@Override
	public Class<? extends Binary> getPersistanceClass() {
		return BinaryImpl.class;
	}

}
