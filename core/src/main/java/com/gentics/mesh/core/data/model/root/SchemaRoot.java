package com.gentics.mesh.core.data.model.root;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_OBJECT_SCHEMA;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.tinkerpop.Schema;

public class SchemaRoot extends MeshVertex {

	public List<? extends Schema> getSchemas() {
		return out(HAS_OBJECT_SCHEMA).toList(Schema.class);
	}

	public void addSchema(Schema schema) {
		linkOut(schema, HAS_OBJECT_SCHEMA);
	}

	// TODO unique index

}
