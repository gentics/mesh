package com.gentics.mesh.core.data.container.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.AbstractGraphFieldSchemaContainer;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * See {@link MicroschemaContainer}
 */
public class MicroschemaContainerImpl
		extends AbstractGraphFieldSchemaContainer<Microschema, MicroschemaReference, MicroschemaContainer, MicroschemaContainerVersion>
		implements MicroschemaContainer {

	@Override
	protected Class<MicroschemaContainerImpl> getContainerClass() {
		return MicroschemaContainerImpl.class;
	}

	@Override
	public String getType() {
		return MicroschemaContainer.TYPE;
	}

	@Override
	protected Class<? extends MicroschemaContainerVersion> getContainerVersionClass() {
		return MicroschemaContainerVersionImpl.class;
	}

	public static void init(Database database) {
		database.addVertexType(MicroschemaContainerImpl.class, MeshVertexImpl.class);
	}

	@Override
	public MicroschemaReference createEmptyReferenceModel() {
		return new MicroschemaReference();
	}

	@Override
	public RootVertex<MicroschemaContainer> getRoot() {
		return MeshRoot.getInstance().getMicroschemaContainerRoot();
	}

	@Override
	public void delete(SearchQueueBatch batch) {
		for (MicroschemaContainerVersion version : findAll()) {
			if (!version.findMicronodes().isEmpty()) {
				throw error(BAD_REQUEST, "microschema_delete_still_in_use", getUuid());
			}
		}
		super.delete(batch);
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return "/api/v1/microschemas/" + getUuid();
	}

}
