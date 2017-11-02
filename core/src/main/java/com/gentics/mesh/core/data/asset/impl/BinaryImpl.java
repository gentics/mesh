package com.gentics.mesh.core.data.asset.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BINARY;

import com.gentics.mesh.core.data.asset.Binary;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.impl.BinaryGraphFieldImpl;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.buffer.Buffer;
import rx.Single;

/**
 * @see Binary
 */
public class BinaryImpl extends MeshVertexImpl implements Binary {

	public static void init(Database database) {
		database.addVertexType(BinaryImpl.class, MeshVertexImpl.class);
	}

	@Override
	public Iterable<? extends BinaryGraphField> findAssets() {
		return inE(HAS_BINARY).frameExplicit(BinaryGraphFieldImpl.class);
	}

	@Override
	public Single<Buffer> getStream() {
		// TODO Auto-generated method stub
		return null;
	}

}
