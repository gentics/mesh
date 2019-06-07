package com.gentics.mesh.core.data.branch.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_VERSION;
import static com.syncleus.ferma.type.EdgeTypeDefinition.edgeType;

import com.gentics.mesh.core.data.branch.BranchSchemaEdge;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.annotations.GraphElement;

/**
 * @see BranchSchemaEdge
 */
@GraphElement
public class BranchSchemaEdgeImpl extends AbstractVersionEdge implements BranchSchemaEdge {

	public static void init(Database db) {
		db.createType(edgeType(BranchSchemaEdgeImpl.class.getSimpleName()));
		db.createType(edgeType(HAS_SCHEMA_VERSION).withSuperClazz(BranchSchemaEdgeImpl.class));
	}

	@Override
	public SchemaContainerVersion getSchemaContainerVersion() {
		return inV().nextOrDefaultExplicit(SchemaContainerVersionImpl.class, null);
	}

}
