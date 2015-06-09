package com.gentics.mesh.core.data.model.schema.propertytypes;

import java.util.List;

import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.AbstractPropertyTypeSchema;

public class MicroPropertyTypeSchema extends AbstractPropertyTypeSchema {

//	@Adjacency(label = BasicRelationships.HAS_SCHEMA_TYPE, direction = Direction.OUT)
	public List<BasicPropertyTypeSchema> getProperties() {
		return out(BasicRelationships.HAS_SCHEMA_TYPE).toList(BasicPropertyTypeSchema.class);
	}

//	@Adjacency(label = BasicRelationships.HAS_SCHEMA_TYPE, direction = Direction.OUT)
	public void addProperty(AbstractPropertyTypeSchema type) {
		
	}

}
