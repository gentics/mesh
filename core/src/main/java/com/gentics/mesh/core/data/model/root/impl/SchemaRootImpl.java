package com.gentics.mesh.core.data.model.root.impl;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_SCHEMA;

import java.util.List;

import com.gentics.mesh.core.data.model.Schema;
import com.gentics.mesh.core.data.model.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.model.impl.SchemaImpl;
import com.gentics.mesh.core.data.model.root.SchemaRoot;

public class SchemaRootImpl extends MeshVertexImpl implements SchemaRoot {

	public List<? extends Schema> getSchemas() {
		return out(HAS_SCHEMA).has(SchemaImpl.class).toList(SchemaImpl.class);
	}

	public void addSchema(Schema schema) {
		linkOut((SchemaImpl) schema, HAS_SCHEMA);
	}

	public SchemaImpl create(String name) {
		SchemaImpl schema = getGraph().addFramedVertex(SchemaImpl.class);
		schema.setName(name);
		addSchema(schema);
		return schema;
	}

	// TODO unique index

	@Override
	public SchemaRootImpl getImpl() {
		return this;
	}

}
