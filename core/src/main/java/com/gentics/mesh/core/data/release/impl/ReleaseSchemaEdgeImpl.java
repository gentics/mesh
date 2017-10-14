package com.gentics.mesh.core.data.release.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_VERSION;

import com.gentics.mesh.core.data.release.ReleaseSchemaEdge;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.annotations.GraphElement;

/**
 * @see ReleaseSchemaEdge
 */
@GraphElement
public class ReleaseSchemaEdgeImpl extends AbstractVersionEdge implements ReleaseSchemaEdge {

	public static void init(Database db) {
		db.addEdgeType(ReleaseSchemaEdgeImpl.class.getSimpleName());
		db.addEdgeType(HAS_SCHEMA_VERSION, ReleaseSchemaEdgeImpl.class);
	}

	@Override
	public SchemaContainerVersion getSchemaContainerVersion() {
		return inV().nextOrDefaultExplicit(SchemaContainerVersionImpl.class, null);
	}

}
