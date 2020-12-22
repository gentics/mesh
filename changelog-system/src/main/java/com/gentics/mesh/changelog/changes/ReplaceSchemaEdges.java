package com.gentics.mesh.changelog.changes;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.SCHEMA_CONTAINER_KEY_PROPERTY;

import com.gentics.mesh.changelog.AbstractChange;
import com.tinkerpop.blueprints.Direction;

/**
 * Changelog entry which removes schema edges to reduce contention.	
 */
public class ReplaceSchemaEdges extends AbstractChange {

	@Override
	public String getUuid() {
		return "74441F725E3F4537841F725E3FE53748";
	}

	@Override
	public String getName() {
		return "ReplaceSchemaEdges";
	}

	@Override
	public String getDescription() {
		return "Replaces node->schema edges with properties";
	}

	@Override
	public void applyInTx() {
		replaceSingleEdge("NodeImpl", Direction.OUT, "HAS_SCHEMA_CONTAINER", SCHEMA_CONTAINER_KEY_PROPERTY);
	}
}
