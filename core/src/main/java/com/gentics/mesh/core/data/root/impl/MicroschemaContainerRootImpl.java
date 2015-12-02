package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;
import static com.gentics.mesh.util.VerticleHelper.processOrFail;

import org.apache.commons.lang.NotImplementedException;
import org.elasticsearch.common.collect.Tuple;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaCreateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.json.MeshJsonException;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class MicroschemaContainerRootImpl extends AbstractRootVertex<MicroschemaContainer>implements MicroschemaContainerRoot {

	@Override
	protected Class<? extends MicroschemaContainer> getPersistanceClass() {
		return MicroschemaContainerImpl.class;
	}

	@Override
	protected String getRootLabel() {
		return HAS_SCHEMA_CONTAINER;
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
	public MicroschemaContainer create(Microschema microschema, User user) throws MeshJsonException {
		microschema.validate();
		MicroschemaContainer container = getGraph().addFramedVertex(MicroschemaContainerImpl.class);
		container.setName(microschema.getName());
		container.setMicroschema(microschema);
		container.setCreated(user);
		addMicroschema(container);

		return container;
	}

	@Override
	public void delete() {
		throw new NotImplementedException();
	}

	@Override
	public void create(InternalActionContext ac, Handler<AsyncResult<MicroschemaContainer>> handler) {
		MeshAuthUser requestUser = ac.getUser();
		Database db = MeshSpringConfiguration.getInstance().database();

		try {
			MicroschemaCreateRequest microschema = JsonUtil.readSchema(ac.getBodyAsString(), MicroschemaCreateRequest.class);
			microschema.validate();

			requestUser.hasPermission(ac, this, GraphPermission.CREATE_PERM, rh -> {
				if (rh.failed()) {
					
				} else {
					db.trx(txCreate -> {
						try {
							requestUser.reload();
							MicroschemaContainer container = create(microschema, requestUser);
							requestUser.addCRUDPermissionOnRole(this, CREATE_PERM, container);
							SearchQueueBatch batch = container.addIndexBatch(SearchQueueEntryAction.CREATE_ACTION);
							txCreate.complete(Tuple.tuple(batch, container));
						} catch (Exception e) {
							txCreate.fail(e);
						}
					} , (AsyncResult<Tuple<SearchQueueBatch, MicroschemaContainer>> txCreated) -> {
						if (txCreated.failed()) {
							handler.handle(Future.failedFuture(txCreated.cause()));
						} else {
							processOrFail(ac, txCreated.result().v1(), handler, txCreated.result().v2());
						}
					});
				}
			});
		} catch (Exception e) {
			handler.handle(Future.failedFuture(e));
		}
	}

}
