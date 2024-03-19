package com.gentics.mesh.changelog.highlevel.change;

import javax.inject.Inject;

import com.gentics.mesh.changelog.highlevel.AbstractHighLevelChange;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.GraphDatabase;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class RemoveDupIndex extends AbstractHighLevelChange {

	private static final Logger log = LoggerFactory.getLogger(RestructureWebrootIndex.class);

	private final GraphDatabase db;

	@Inject
	public RemoveDupIndex(GraphDatabase db) {
		this.db = db;
	}

	@Override
	public boolean isAllowedInCluster(MeshOptions options) {
		return false;
	}

	@Override
	public String getUuid() {
		return "1994B4DD16CCEE11A734897419011310";
	}

	@Override
	public String getName() {
		return "Remove duplicated indices";
	}

	@Override
	public void applyNoTx() {
		// This index is dubbed by MeshVertexImpl parent class.
		db.index().removeVertexIndex("NodeImpl_uuid", NodeImpl.class);
	}

	@Override
	public String getDescription() {
		return "Remove duplicated index on the entity";
	}

}
