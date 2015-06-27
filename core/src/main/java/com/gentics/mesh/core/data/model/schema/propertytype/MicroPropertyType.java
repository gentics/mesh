package com.gentics.mesh.core.data.model.schema.propertytype;

import java.util.List;

import com.gentics.mesh.core.data.model.impl.AbstractPropertyTypeSchemaImpl;
import com.gentics.mesh.core.data.model.relationship.MeshRelationships;

public class MicroPropertyType extends AbstractPropertyTypeSchemaImpl {

//	@Adjacency(label = BasicRelationships.HAS_SCHEMA_TYPE, direction = Direction.OUT)
	public List<? extends BasicPropertyTypeImpl> getProperties() {
		return out(MeshRelationships.HAS_SCHEMA_TYPE).toList(BasicPropertyTypeImpl.class);
	}

//	@Adjacency(label = BasicRelationships.HAS_SCHEMA_TYPE, direction = Direction.OUT)
	public void addProperty(AbstractPropertyTypeSchemaImpl type) {
		
	}

}
