package com.gentics.mesh.core.data.schema.impl;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.search.BucketableElementHelper;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;

/**
 * @see Schema
 */
public class SchemaContainerImpl extends
	AbstractGraphFieldSchemaContainer<SchemaResponse, SchemaVersionModel, SchemaReference, HibSchema, HibSchemaVersion> implements
	Schema {

	@Override
	protected Class<? extends Schema> getContainerClass() {
		return SchemaContainerImpl.class;
	}

	@Override
	protected Class<? extends SchemaVersion> getContainerVersionClass() {
		return SchemaContainerVersionImpl.class;
	}

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(SchemaContainerImpl.class, MeshVertexImpl.class);
	}

	@Override
	public RootVertex<? extends Schema> getRoot() {
		return mesh().boot().meshRoot().getSchemaContainerRoot();
	}

	@Override
	public void delete(BulkActionContext bac) {
		mesh().boot().schemaDao().delete(this, bac);
	}

	@Override
	public HibUser getCreator() {
		return mesh().userProperties().getCreator(this);
	}

	@Override
	public HibUser getEditor() {
		return mesh().userProperties().getEditor(this);
	}

	@Override
	public Integer getBucketId() {
		return BucketableElementHelper.getBucketId(this);
	}

	@Override
	public void setBucketId(Integer bucketId) {
		BucketableElementHelper.setBucketId(this, bucketId);
	}

	@Override
	public void generateBucketId() {
		BucketableElementHelper.generateBucketId(this);
	}
}
