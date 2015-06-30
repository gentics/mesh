package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_SCHEMA_CONTAINER;

import java.util.List;

import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;

public class SchemaRootImpl extends MeshVertexImpl implements SchemaContainerRoot {

	public List<? extends SchemaContainer> getSchemaContainers() {
		return out(HAS_SCHEMA_CONTAINER).has(SchemaContainerImpl.class).toList(SchemaContainerImpl.class);
	}

	public void addSchemaContainer(SchemaContainer schema) {
		linkOut(schema.getImpl(), HAS_SCHEMA_CONTAINER);
	}

	public SchemaContainer create(String name) {
		SchemaContainerImpl schema = getGraph().addFramedVertex(SchemaContainerImpl.class);
		schema.setSchemaName(name);
		addSchemaContainer(schema);
		return schema;
	}

	// TODO unique index

	@Override
	public SchemaRootImpl getImpl() {
		return this;
	}

}
