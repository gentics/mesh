package com.gentics.mesh.core.data.branch.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_VERSION;

import com.gentics.mesh.core.data.branch.BranchSchemaEdge;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;
import com.gentics.madl.annotation.GraphElement;

/**
 * @see BranchSchemaEdge
 */
@GraphElement
public class BranchSchemaEdgeImpl extends AbstractVersionEdge implements BranchSchemaEdge {

	public static void init(LegacyDatabase db) {
		db.addEdgeType(BranchSchemaEdgeImpl.class.getSimpleName());
		db.addEdgeType(HAS_SCHEMA_VERSION, BranchSchemaEdgeImpl.class);
	}

	@Override
	public SchemaContainerVersion getSchemaContainerVersion() {
		return inV().nextOrDefaultExplicit(SchemaContainerVersionImpl.class, null);
	}

}
