package com.gentics.mesh.core.data.model.root.impl;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_SCHEMA;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.model.impl.SchemaImpl;

public class MicroSchemaRootImpl extends MeshVertexImpl {

	public List<? extends SchemaImpl> getSchemas() {
		return out(HAS_SCHEMA).toList(SchemaImpl.class);
	}

}
