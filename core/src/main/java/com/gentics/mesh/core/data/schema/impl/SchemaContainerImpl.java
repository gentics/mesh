package com.gentics.mesh.core.data.schema.impl;

import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * @see SchemaContainer
 */
public class SchemaContainerImpl extends AbstractGraphFieldSchemaContainer<Schema, SchemaReference, SchemaContainer, SchemaContainerVersion>
		implements SchemaContainer {

	@Override
	protected Class<? extends SchemaContainer> getContainerClass() {
		return SchemaContainerImpl.class;
	}

	@Override
	protected Class<? extends SchemaContainerVersion> getContainerVersionClass() {
		return SchemaContainerVersionImpl.class;
	}

	public static void checkIndices(Database database) {
		database.addVertexType(SchemaContainerImpl.class);
	}

	@Override
	public SchemaReference createEmptyReferenceModel() {
		return new SchemaReference();
	}

	@Override
	public RootVertex<SchemaContainer> getRoot() {
		return MeshRoot.getInstance().getSchemaContainerRoot();
	}

	@Override
	public String getType() {
		return SchemaContainer.TYPE;
	}

}
