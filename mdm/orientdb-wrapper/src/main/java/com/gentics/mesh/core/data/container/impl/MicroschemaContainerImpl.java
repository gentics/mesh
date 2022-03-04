package com.gentics.mesh.core.data.container.impl;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.impl.AbstractGraphFieldSchemaContainer;
import com.gentics.mesh.core.data.search.BucketableElementHelper;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;

/**
 * See {@link Microschema}
 */
public class MicroschemaContainerImpl extends
	AbstractGraphFieldSchemaContainer<MicroschemaResponse, MicroschemaVersionModel, MicroschemaReference, HibMicroschema, HibMicroschemaVersion>
	implements Microschema {

	@Override
	protected Class<MicroschemaContainerImpl> getContainerClass() {
		return MicroschemaContainerImpl.class;
	}

	@Override
	protected Class<? extends MicroschemaVersion> getContainerVersionClass() {
		return MicroschemaContainerVersionImpl.class;
	}

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(MicroschemaContainerImpl.class, MeshVertexImpl.class);
	}

	@Override
	public RootVertex<Microschema> getRoot() {
		return ((OrientDBBootstrapInitializer) mesh().boot()).meshRoot().getMicroschemaContainerRoot();
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
}
