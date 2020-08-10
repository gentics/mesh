package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.handler.VersionHandler.CURRENT_API_BASE_PATH;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;

/**
 * @see Schema
 */
public class SchemaContainerImpl extends
		AbstractGraphFieldSchemaContainer<SchemaResponse, SchemaVersionModel, SchemaReference, Schema, SchemaVersion> implements
	Schema {

	@Override
	protected Class<? extends Schema> getContainerClass() {
		return SchemaContainerImpl.class;
	}

	@Override
	protected Class<? extends SchemaVersion> getContainerVersionClass() {
		return SchemaContainerVersionImpl.class;
	}

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(SchemaContainerImpl.class, MeshVertexImpl.class);
	}

	@Override
	public SchemaReference transformToReference() {
		return new SchemaReferenceImpl().setName(getName()).setUuid(getUuid());
	}

	@Override
	public RootVertex<Schema> getRoot() {
		return mesh().boot().meshRoot().getSchemaContainerRoot();
	}

	@Override
	public void delete(BulkActionContext bac) {
		mesh().boot().schemaDao().delete(this, bac);
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return CURRENT_API_BASE_PATH + "/schemas/" + getUuid();
	}

	@Override
	public User getCreator() {
		return mesh().userProperties().getCreator(this);
	}

	@Override
	public User getEditor() {
		return mesh().userProperties().getEditor(this);
	}

}
