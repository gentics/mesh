package com.gentics.mesh.core.data.container.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CREATOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_EDITOR;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.AbstractGraphFieldSchemaContainer;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.microschema.MicroschemaModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * See {@link MicroschemaContainer}
 */
public class MicroschemaContainerImpl extends
		AbstractGraphFieldSchemaContainer<MicroschemaResponse, MicroschemaModel, MicroschemaReference, MicroschemaContainer, MicroschemaContainerVersion>
		implements MicroschemaContainer {

	@Override
	protected Class<MicroschemaContainerImpl> getContainerClass() {
		return MicroschemaContainerImpl.class;
	}

	@Override
	protected Class<? extends MicroschemaContainerVersion> getContainerVersionClass() {
		return MicroschemaContainerVersionImpl.class;
	}

	public static void init(Database database) {
		database.addVertexType(MicroschemaContainerImpl.class, MeshVertexImpl.class);
	}

	@Override
	public MicroschemaReference transformToReference() {
		return new MicroschemaReferenceImpl().setName(getName()).setUuid(getUuid());
	}

	@Override
	public RootVertex<MicroschemaContainer> getRoot() {
		return MeshInternal.get().boot().meshRoot().getMicroschemaContainerRoot();
	}

	@Override
	public void delete(SearchQueueBatch batch) {
		for (MicroschemaContainerVersion version : findAll()) {
			if (version.findMicronodes().hasNext()) {
				throw error(BAD_REQUEST, "microschema_delete_still_in_use", getUuid());
			}
			version.delete(batch);
		}
		super.delete(batch);
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return "/api/v1/microschemas/" + getUuid();
	}

	@Override
	public User getCreator() {
		return out(HAS_CREATOR).nextOrDefault(UserImpl.class, null);
	}

	@Override
	public User getEditor() {
		return out(HAS_EDITOR).nextOrDefaultExplicit(UserImpl.class, null);
	}

}
