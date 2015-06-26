package com.gentics.mesh.core.data.model.node.field;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.ALLOWED_SCHEMA;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.data.model.Schema;
import com.gentics.mesh.core.data.model.generic.MeshVertex;

public class ListField<T extends Field> extends MeshVertex {

	private List<T> list = new ArrayList<>();

	public List<T> getList() {
		return list;
	}

	public List<? extends Schema> getAllowedSchemas() {
		return out(ALLOWED_SCHEMA).has(Schema.class).toListExplicit(Schema.class);
	}

	public void addAllowedSchema(Schema schema) {
		linkOut(schema, ALLOWED_SCHEMA);
	}

	public void removeAllowedSchema(Schema schema) {
		unlinkOut(schema, ALLOWED_SCHEMA);
	}
}
