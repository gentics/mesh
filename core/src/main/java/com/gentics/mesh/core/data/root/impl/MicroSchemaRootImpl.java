package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_SCHEMA_CONTAINER;

import java.util.List;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.SchemaContainerImpl;

public class MicroSchemaRootImpl extends MeshVertexImpl {

	public List<? extends SchemaContainerImpl> getSchemas() {
		return out(HAS_SCHEMA_CONTAINER).toList(SchemaContainerImpl.class);
	}

}
