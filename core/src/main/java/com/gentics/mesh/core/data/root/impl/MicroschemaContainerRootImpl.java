package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER_ITEM;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;

import org.apache.commons.lang.NotImplementedException;
import org.elasticsearch.common.collect.Tuple;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;

import rx.Single;

public class MicroschemaContainerRootImpl extends AbstractRootVertex<MicroschemaContainer> implements MicroschemaContainerRoot {

	public static void init(Database database) {
		database.addVertexType(MicroschemaContainerRootImpl.class, MeshVertexImpl.class);
	}

	@Override
	public Class<? extends MicroschemaContainer> getPersistanceClass() {
		return MicroschemaContainerImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_SCHEMA_CONTAINER_ITEM;
	}

	@Override
	public void addMicroschema(MicroschemaContainer container) {
		addItem(container);
	}

	@Override
	public void removeMicroschema(MicroschemaContainer container) {
		removeItem(container);
	}

	@Override
	public MicroschemaContainer create(Microschema microschema, User user) {
		microschema.validate();
		MicroschemaContainer container = getGraph().addFramedVertex(MicroschemaContainerImpl.class);
		MicroschemaContainerVersion version = getGraph().addFramedVertex(MicroschemaContainerVersionImpl.class);

		microschema.setVersion(1);
		container.setLatestVersion(version);
		version.setName(microschema.getName());
		version.setSchema(microschema);
		version.setSchemaContainer(container);
		container.setCreated(user);
		container.setName(microschema.getName());
		addMicroschema(container);

		return container;
	}

	@Override
	public void delete(SearchQueueBatch batch) {
		throw new NotImplementedException();
	}

	@Override
	public Single<MicroschemaContainer> create(InternalActionContext ac) {
		MeshAuthUser requestUser = ac.getUser();
		Database db = MeshInternal.get().database();
		try {
			Microschema microschema = JsonUtil.readValue(ac.getBodyAsString(), MicroschemaModel.class);
			microschema.validate();
			if (requestUser.hasPermission(this, GraphPermission.CREATE_PERM)) {
				Tuple<SearchQueueBatch, MicroschemaContainer> tuple = db.tx(() -> {
					requestUser.reload();
					MicroschemaContainer container = create(microschema, requestUser);
					requestUser.addCRUDPermissionOnRole(this, CREATE_PERM, container);
					SearchQueueBatch batch = container.createIndexBatch(STORE_ACTION);
					return Tuple.tuple(batch, container);
				});

				SearchQueueBatch batch = tuple.v1();
				MicroschemaContainer microschemaContainer = tuple.v2();
				return batch.process().andThen(Single.just(microschemaContainer));
			} else {
				throw error(FORBIDDEN, "error_missing_perm", microschema.getUuid());
			}
		} catch (IOException e) {
			return Single.error(e);
		}
	}

	@Override
	public Single<MicroschemaContainerVersion> fromReference(MicroschemaReference reference) {
		return fromReference(reference, null);
	}

	@Override
	public Single<MicroschemaContainerVersion> fromReference(MicroschemaReference reference, Release release) {
		String microschemaName = reference.getName();
		String microschemaUuid = reference.getUuid();
		Integer version = release == null ? reference.getVersion() : null;
		MicroschemaContainer container = null;
		if (!isEmpty(microschemaName)) {
			container = findByName(microschemaName);
		} else {
			container = findByUuid(microschemaUuid);
		}
		// Return the specified version or fallback to latest version.
		if (container == null) {
			throw error(BAD_REQUEST, "error_microschema_reference_not_found", isEmpty(microschemaName) ? "-" : microschemaName,
					isEmpty(microschemaUuid) ? "-" : microschemaUuid, version == null ? "-" : version.toString());
		}

		MicroschemaContainerVersion foundVersion = null;

		if (release != null) {
			foundVersion = release.getVersion(container);
		} else if (version != null) {
			foundVersion = container.findVersionByRev(version);
		} else {
			foundVersion = container.getLatestVersion();
		}

		if (foundVersion == null) {
			throw error(BAD_REQUEST, "error_microschema_reference_not_found", isEmpty(microschemaName) ? "-" : microschemaName,
					isEmpty(microschemaUuid) ? "-" : microschemaUuid, version == null ? "-" : version.toString());
		}
		return Single.just(foundVersion);
	}

	@Override
	public boolean contains(MicroschemaContainer microschema) {
		if (findByUuid(microschema.getUuid()) == null) {
			return false;
		} else {
			return true;
		}
	}
}
