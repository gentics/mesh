package com.gentics.mesh.core.data.asset.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ASSET_BINARY;

import com.gentics.mesh.core.data.asset.AssetBinaryRoot;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.graphdb.spi.Database;

public class AssetBinaryRootImpl extends MeshVertexImpl implements AssetBinaryRoot {

	public static void init(Database database) {
		database.addVertexType(AssetBinaryRootImpl.class, MeshVertexImpl.class);
		database.addEdgeIndex(HAS_ASSET_BINARY, true, false, true);
	}

}
