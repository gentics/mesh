package com.gentics.mesh.core.data.model.schema;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.TPObjectSchema;

@NodeEntity
public class MicroSchemaRoot extends AbstractPersistable {

	private static final long serialVersionUID = 3219647773178647366L;

	@RelatedTo(type = BasicRelationships.HAS_OBJECT_SCHEMA, direction = Direction.OUTGOING, elementClass = ObjectSchema.class)
	private Set<TPObjectSchema> schemas = new HashSet<>();

	@Indexed(unique = true)
	private String unique = MicroSchemaRoot.class.getSimpleName();

	public MicroSchemaRoot() {
	}

	public Iterable<TPObjectSchema> getSchemas() {
		return schemas;
	}

}
