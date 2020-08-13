package com.gentics.mesh.core.data.branch.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_VERSION;
import static com.gentics.mesh.madl.type.EdgeTypeDefinition.edgeType;

import com.gentics.madl.annotations.GraphElement;
import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.branch.BranchSchemaEdge;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;

/**
 * @see BranchSchemaEdge
 */
@GraphElement
public class BranchSchemaEdgeImpl extends AbstractVersionEdge implements BranchSchemaEdge {

	public static void init(TypeHandler type, IndexHandler index) {
		type.createType(edgeType(BranchSchemaEdgeImpl.class.getSimpleName()));
		type.createType(edgeType(HAS_SCHEMA_VERSION).withSuperClazz(BranchSchemaEdgeImpl.class));
	}

	@Override
	public SchemaVersion getSchemaContainerVersion() {
		return inV().nextOrDefaultExplicit(SchemaContainerVersionImpl.class, null);
	}

}
