package com.gentics.mesh.core.data.model.root;

import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.ObjectSchema;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface ObjectSchemaRoot extends AbstractPersistable {

	@Adjacency(label = BasicRelationships.HAS_OBJECT_SCHEMA, direction = Direction.OUT)
	public Iterable<ObjectSchema> getSchemas();

	@Adjacency(label = BasicRelationships.HAS_OBJECT_SCHEMA, direction = Direction.OUT)
	public void addSchema(ObjectSchema schema);

	// TODO unique index

}
