package com.gentics.mesh.core.data.asset.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BINARY;

import com.gentics.mesh.core.data.asset.Asset;
import com.gentics.mesh.core.data.asset.AssetBinary;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * @see AssetBinary
 */
public class AssetBinaryImpl extends MeshVertexImpl implements AssetBinary {

	public static void init(Database database) {
		database.addVertexType(AssetBinaryImpl.class, MeshVertexImpl.class);
	}

	@Override
	public Iterable<? extends Asset> findAssets() {
		return in(HAS_BINARY).frameExplicit(AssetImpl.class);
	}

}
