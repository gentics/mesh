package com.gentics.mesh.core.data.model.schema.propertytypes;

import java.util.List;

import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.AbstractPropertyTypeSchema;

public class MicroPropertyType extends AbstractPropertyTypeSchema {

//	@Adjacency(label = BasicRelationships.HAS_SCHEMA_TYPE, direction = Direction.OUT)
	public List<? extends BasicPropertyType> getProperties() {
		return out(BasicRelationships.HAS_SCHEMA_TYPE).toList(BasicPropertyType.class);
	}

//	@Adjacency(label = BasicRelationships.HAS_SCHEMA_TYPE, direction = Direction.OUT)
	public void addProperty(AbstractPropertyTypeSchema type) {
		
	}

}
