package com.gentics.mesh.core.data.model.schema.propertytypes;

import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.AbstractPropertyTypeSchema;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface MicroPropertyTypeSchema extends AbstractPropertyTypeSchema {

	@Adjacency(label = BasicRelationships.HAS_SCHEMA_TYPE, direction = Direction.OUT)
	public Iterable<BasicPropertyTypeSchema> getProperties();

	@Adjacency(label = BasicRelationships.HAS_SCHEMA_TYPE, direction = Direction.OUT)
	public void addProperty(AbstractPropertyTypeSchema type);

}
