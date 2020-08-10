package com.gentics.mesh.core.data.container.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.handler.VersionHandler.CURRENT_API_BASE_PATH;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.impl.AbstractGraphFieldSchemaContainer;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;

/**
 * See {@link Microschema}
 */
public class MicroschemaImpl extends
	AbstractGraphFieldSchemaContainer<MicroschemaResponse, MicroschemaVersionModel, MicroschemaReference, Microschema, MicroschemaVersion>
	implements Microschema {

	@Override
	protected Class<MicroschemaImpl> getContainerClass() {
		return MicroschemaImpl.class;
	}

	@Override
	protected Class<? extends MicroschemaVersion> getContainerVersionClass() {
		return MicroschemaVersionImpl.class;
	}

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(MicroschemaImpl.class, MeshVertexImpl.class);
	}

	@Override
	public MicroschemaReference transformToReference() {
		return new MicroschemaReferenceImpl().setName(getName()).setUuid(getUuid());
	}

	@Override
	public RootVertex<Microschema> getRoot() {
		return mesh().boot().meshRoot().getMicroschemaContainerRoot();
	}

	@Override
	public void delete(BulkActionContext bac) {
		for (MicroschemaVersion version : findAll()) {
			if (version.findMicronodes().hasNext()) {
				throw error(BAD_REQUEST, "microschema_delete_still_in_use", getUuid());
			}
			version.delete(bac);
		}
		super.delete(bac);
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return CURRENT_API_BASE_PATH + "/microschemas/" + getUuid();
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
