package com.gentics.mesh.core.data.model.root;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_SCHEMA;

import java.util.List;

import com.gentics.mesh.core.data.model.Schema;
import com.gentics.mesh.core.data.model.generic.MeshVertex;

public class MicroSchemaRoot extends MeshVertex {

	public List<? extends Schema> getSchemas() {
		return out(HAS_SCHEMA).toList(Schema.class);
	}

}
