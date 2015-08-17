package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.error.MeshSchemaException;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class SchemaContainerRootImpl extends AbstractRootVertex<SchemaContainer>implements SchemaContainerRoot {

	private static final Logger log = LoggerFactory.getLogger(SchemaContainerRootImpl.class);

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
	public SchemaContainer create(Schema schema, User creator) throws MeshSchemaException {
		validate(schema);
		SchemaContainerImpl schemaContainer = getGraph().addFramedVertex(SchemaContainerImpl.class);
		schemaContainer.setSchema(schema);
		schemaContainer.setName(schema.getName());
		addSchemaContainer(schemaContainer);
		schemaContainer.setCreator(creator);
		schemaContainer.setCreationTimestamp(System.currentTimeMillis());
		schemaContainer.setEditor(creator);
		schemaContainer.setLastEditedTimestamp(System.currentTimeMillis());
		return schemaContainer;
	}

	private void validate(Schema schema) throws MeshSchemaException {
		if (StringUtils.isEmpty(schema.getDisplayField())) {
			throw new MeshSchemaException("The displayField must not be empty");
		}

	}

	@Override
	public boolean contains(SchemaContainer schema) {
		// TODO this is not optimal
		if (findByName(schema.getName()) == null) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public void delete() {
		// TODO maybe we should add a check here to prevent deletion of the meshroot.schemaRoot ?
		if (log.isDebugEnabled()) {
			log.debug("Deleting schema container root {" + getUuid() + "}");
		}
		getElement().remove();
	}

}
