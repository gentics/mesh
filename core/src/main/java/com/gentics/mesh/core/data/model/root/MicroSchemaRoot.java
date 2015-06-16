package com.gentics.mesh.core.data.model.root;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.relationship.MeshRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.Schema;

public class MicroSchemaRoot extends MeshVertex {

	public List<? extends Schema> getSchemas() {
		return out(MeshRelationships.HAS_OBJECT_SCHEMA).toList(Schema.class);
	}

}
