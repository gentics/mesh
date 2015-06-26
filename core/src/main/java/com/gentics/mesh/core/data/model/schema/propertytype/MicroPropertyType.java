package com.gentics.mesh.core.data.model.schema.propertytype;

import java.util.List;

import com.gentics.mesh.core.data.model.AbstractPropertyTypeSchema;
import com.gentics.mesh.core.data.model.relationship.MeshRelationships;

public class MicroPropertyType extends AbstractPropertyTypeSchema {

//	@Adjacency(label = BasicRelationships.HAS_SCHEMA_TYPE, direction = Direction.OUT)
	public List<? extends BasicPropertyType> getProperties() {
		return out(MeshRelationships.HAS_SCHEMA_TYPE).toList(BasicPropertyType.class);
	}

//	@Adjacency(label = BasicRelationships.HAS_SCHEMA_TYPE, direction = Direction.OUT)
	public void addProperty(AbstractPropertyTypeSchema type) {
		
	}

}
