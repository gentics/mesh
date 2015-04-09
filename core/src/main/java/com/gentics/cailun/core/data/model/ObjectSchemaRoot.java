package com.gentics.cailun.core.data.model;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.data.model.generic.AbstractPersistable;
import com.gentics.cailun.core.data.model.relationship.BasicRelationships;

@NodeEntity
public class ObjectSchemaRoot extends AbstractPersistable {

	private static final long serialVersionUID = 5160771115848405859L;

	@RelatedTo(type = BasicRelationships.HAS_SCHEMA, direction = Direction.OUTGOING, elementClass = ObjectSchema.class)
	private Set<ObjectSchema> schemas = new HashSet<>();

	@Indexed(unique = true)
	private String unique = ObjectSchemaRoot.class.getSimpleName();

	public ObjectSchemaRoot() {
	}

	public Set<ObjectSchema> getSchemas() {
		return schemas;
	}
}
