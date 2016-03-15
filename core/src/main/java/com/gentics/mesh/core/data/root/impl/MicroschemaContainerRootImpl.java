package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER_ITEM;

import java.io.IOException;

import org.apache.commons.lang.NotImplementedException;
import org.elasticsearch.common.collect.Tuple;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;

import rx.Observable;

public class MicroschemaContainerRootImpl extends AbstractRootVertex<MicroschemaContainer> implements MicroschemaContainerRoot {

	public static void checkIndices(Database database) {
		database.addVertexType(MicroschemaContainerRootImpl.class);
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
	public void delete() {
		throw new NotImplementedException();
	}

	@Override
	public Observable<MicroschemaContainer> create(InternalActionContext ac) {
		MeshAuthUser requestUser = ac.getUser();
		Database db = MeshSpringConfiguration.getInstance().database();

		try {
			Microschema microschema = JsonUtil.readSchema(ac.getBodyAsString(), MicroschemaModel.class);
			microschema.validate();

			return requestUser.hasPermissionAsync(ac, this, GraphPermission.CREATE_PERM).flatMap(hasPerm -> {
				Tuple<SearchQueueBatch, MicroschemaContainer> tuple = db.trx(() -> {
					requestUser.reload();
					MicroschemaContainer container = create(microschema, requestUser);
					requestUser.addCRUDPermissionOnRole(this, CREATE_PERM, container);
					SearchQueueBatch batch = container.createIndexBatch(SearchQueueEntryAction.CREATE_ACTION);
					return Tuple.tuple(batch, container);
				});

				SearchQueueBatch batch = tuple.v1();
				MicroschemaContainer microschemaContainer = tuple.v2();
				return batch.process().map(done -> microschemaContainer);
			});
		} catch (IOException e) {
			return Observable.error(e);
		}
	}

}
