package com.gentics.mesh.changelog.changes;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.MICROSCHEMA_VERSION_KEY_PROPERTY;

import com.gentics.mesh.changelog.AbstractChange;
import com.tinkerpop.blueprints.Direction;

/**
 * Changelog which replaces microschema edges with properties to reduce contention.
 */
public class ReplaceMicroschemaVersionEdges extends AbstractChange {

	@Override
	public String getUuid() {
		return "2E6DA7D47E13429EADA7D47E13729E56";
	}

	@Override
	public String getName() {
		return "ReplaceMicroschemaVersionEdges";
	}

	@Override
	public String getDescription() {
		return "Replaces edges from micronodes to microschema versions with properties.";
	}

	@Override
	public void applyInTx() {
		replaceSingleEdge("MicronodeImpl", Direction.OUT, "HAS_MICROSCHEMA_CONTAINER", MICROSCHEMA_VERSION_KEY_PROPERTY);
	}
}
