package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface TPMicroSchemaRoot extends TPAbstractPersistable {

	@Adjacency(label = BasicRelationships.HAS_OBJECT_SCHEMA, direction = Direction.OUT)
	public Iterable<TPObjectSchema> getSchemas();

}
