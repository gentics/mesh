package com.gentics.mesh.core.data.model.root;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.Schema;

public class SchemaRoot extends MeshVertex {

	public List<Schema> getSchemas() {
		return out(BasicRelationships.HAS_OBJECT_SCHEMA).toList(Schema.class);
	}

	public void addSchema(Schema schema) {
		linkOut(schema, BasicRelationships.HAS_OBJECT_SCHEMA);
	}

	// TODO unique index

}
