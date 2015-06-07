package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.schema.ObjectSchema;
import com.tinkerpop.frames.Adjacency;

public interface TPGenericPropertyContainer extends TPGenericNode {

	@Adjacency(label = BasicRelationships.HAS_OBJECT_SCHEMA, direction = com.tinkerpop.blueprints.Direction.OUT)
	public Iterable<TPTranslated> getI18nTranslations();

	@Adjacency(label = BasicRelationships.HAS_OBJECT_SCHEMA, direction = com.tinkerpop.blueprints.Direction.OUT)
	public void setSchema(ObjectSchema schema);

	@Adjacency(label = BasicRelationships.HAS_OBJECT_SCHEMA, direction = com.tinkerpop.blueprints.Direction.OUT)
	public TPObjectSchema getSchema();

}
