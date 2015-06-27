package com.gentics.mesh.core.data.model.node.field.impl;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.ALLOWED_SCHEMA;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.data.model.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.model.impl.SchemaImpl;
import com.gentics.mesh.core.data.model.node.field.Field;

public class ListFieldImpl<T extends Field> extends MeshVertexImpl {

	private List<T> list = new ArrayList<>();

	public List<T> getList() {
		return list;
	}

	public List<? extends SchemaImpl> getAllowedSchemas() {
		return out(ALLOWED_SCHEMA).has(SchemaImpl.class).toListExplicit(SchemaImpl.class);
	}

	public void addAllowedSchema(SchemaImpl schema) {
		linkOut(schema, ALLOWED_SCHEMA);
	}

	public void removeAllowedSchema(SchemaImpl schema) {
		unlinkOut(schema, ALLOWED_SCHEMA);
	}
}
