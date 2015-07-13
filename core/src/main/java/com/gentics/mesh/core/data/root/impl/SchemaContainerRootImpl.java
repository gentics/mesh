package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_SCHEMA_CONTAINER;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.rest.schema.SchemaResponse;

public class SchemaContainerRootImpl extends AbstractRootVertex<SchemaContainer, SchemaResponse> implements SchemaContainerRoot {

	@Override
	protected Class<? extends SchemaContainer> getPersistanceClass() {
		return SchemaContainerImpl.class;
	}

	@Override
	protected String getRootLabel() {
		return HAS_SCHEMA_CONTAINER;
	}

	@Override
	public void addSchemaContainer(SchemaContainer schema) {
		addItem(schema);
	}

	@Override
	public void removeSchemaContainer(SchemaContainer schemaContainer) {
		removeItem(schemaContainer);
	}

	@Override
	public SchemaContainer create(String name) {
		SchemaContainerImpl schema = getGraph().addFramedVertex(SchemaContainerImpl.class);
		schema.setSchemaName(name);
		addSchemaContainer(schema);
		return schema;
	}

	// TODO unique index

	@Override
	public SchemaContainer findByName(String projectName, String name) {
		if (StringUtils.isEmpty(projectName) || StringUtils.isEmpty(name)) {
			throw new NullPointerException("name or project name null");
		}
		return findByNameAndProject(projectName, name);
	}

}
