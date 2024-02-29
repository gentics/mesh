package com.gentics.mesh.changelog.changes;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.SCHEMA_CONTAINER_VERSION_KEY_PROPERTY;

import org.apache.tinkerpop.gremlin.structure.Direction;

import com.gentics.mesh.changelog.AbstractChange;

/**
 * Changelog entry which removed the schema version edges with properties
 */
public class ReplaceSchemaVersionEdges extends AbstractChange {

	@Override
	public String getUuid() {
		return "E737684330534623B768433053C623F2";
	}

	@Override
	public String getName() {
		return "ReplaceSchemaVersionEdges";
	}

	@Override
	public String getDescription() {
		return "Replaces edges from node content to schema versions with properties.";
	}

	@Override
	public void applyInTx() throws Exception {
		replaceSingleEdge("NodeGraphFieldContainerImpl", Direction.OUT, "HAS_SCHEMA_CONTAINER_VERSION", SCHEMA_CONTAINER_VERSION_KEY_PROPERTY);
	}
}
