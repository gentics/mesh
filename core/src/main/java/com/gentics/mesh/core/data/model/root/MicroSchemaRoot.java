package com.gentics.mesh.core.data.model.root;

import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.ObjectSchema;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface MicroSchemaRoot extends AbstractPersistable {

	@Adjacency(label = BasicRelationships.HAS_OBJECT_SCHEMA, direction = Direction.OUT)
	public Iterable<ObjectSchema> getSchemas();

}
