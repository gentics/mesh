package com.gentics.mesh.core.data.binary.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BINARY;

import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.binary.BinaryRoot;
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

	@Override
	public Binary create(String sha512sum) {
		Binary binary = getGraph().addFramedVertex(BinaryImpl.class);
		binary.setSHA512Sum(sha512sum);
		return binary;
	}

	@Override
	public Binary findByHash(String hash) {
		//TODO use index
		return out(HAS_BINARY).has(Binary.SHA512SUM_KEY, hash).nextOrDefaultExplicit(BinaryImpl.class, null);
	}

}