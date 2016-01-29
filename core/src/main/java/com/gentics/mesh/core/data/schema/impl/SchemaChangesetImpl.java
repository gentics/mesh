package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CHANGE;

import java.util.List;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.SchemaChangeset;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.graphdb.spi.Database;

public class SchemaChangesetImpl extends MeshVertexImpl implements SchemaChangeset {

	public static void checkIndices(Database database) {
		database.addVertexType(SchemaChangesetImpl.class);
	}

	@Override
	public List<? extends SchemaChange> getChanges() {
		return out(HAS_CHANGE).has(SchemaChangeImpl.class).toListExplicit(SchemaChangeImpl.class);
	}

	@Override
	public SchemaChangeset addChange(SchemaChange change) {
		linkOut(change.getImpl(), HAS_CHANGE);
		return this;
	}

	@Override
	public SchemaContainer getFromContainer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SchemaContainer getToContainer() {
		// TODO Auto-generated method stub
		return null;
	}
}
