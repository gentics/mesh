package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.UPDATE_ACTION;
import static com.gentics.mesh.core.data.service.ServerSchemaStorage.getSchemaStorage;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.failedFuture;
import static com.gentics.mesh.util.VerticleHelper.processOrFail2;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.IOException;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.generic.AbstractReferenceableCoreElement;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaImpl;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaUpdateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.RestModelHelper;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class MicroschemaContainerImpl extends AbstractReferenceableCoreElement<MicroschemaResponse, MicroschemaReference> implements MicroschemaContainer {

	@Override
	protected MicroschemaReference createEmptyReferenceModel() {
		return new MicroschemaReference();
	}

	@Override
	public String getType() {
		return MicroschemaContainer.TYPE;
	}

	@Override
	public String getName() {
		return getProperty("name");
	}

	@Override
	public void setName(String name) {
		setProperty("name", name);
	}

	@Override
	public Microschema getMicroschema() {
		Microschema microschema = getSchemaStorage().getMicroschema(getName());
		if (microschema == null) {
			try {
				microschema = JsonUtil.readSchema(getJson(), MicroschemaImpl.class);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			getSchemaStorage().addMicroschema(microschema);
		}
		return microschema;
	}

	@Override
	public void setMicroschema(Microschema microschema) {
		getSchemaStorage().removeMicroschema(microschema.getName());
		getSchemaStorage().addMicroschema(microschema);
		String json = JsonUtil.toJson(microschema);
		setJson(json);
	}

	@Override
	public void transformToRest(InternalActionContext ac, Handler<AsyncResult<MicroschemaResponse>> handler) {
		try {
			MicroschemaResponse microschema = JsonUtil.readSchema(getJson(), MicroschemaResponse.class);
			microschema.setUuid(getUuid());

			// Role permissions
			RestModelHelper.setRolePermissions(ac, this, microschema);

			microschema.setPermissions(ac.getUser().getPermissionNames(ac, this));

			handler.handle(Future.succeededFuture(microschema));
		} catch (IOException e) {
			handler.handle(Future.failedFuture(e));
		}
	}

	@Override
	public void delete() {
		addIndexBatch(DELETE_ACTION);
		getElement().remove();
	}

	@Override
	public void update(InternalActionContext ac, Handler<AsyncResult<Void>> handler) {
		Database db = MeshSpringConfiguration.getInstance().database();
		MicroschemaContainerRoot root = BootstrapInitializer.getBoot().meshRoot().getMicroschemaContainerRoot();

		try {
			MicroschemaUpdateRequest requestModel = JsonUtil.readSchema(ac.getBodyAsString(), MicroschemaUpdateRequest.class);
			requestModel.validate();

			MicroschemaContainer foundMicroschema = root.findByName(requestModel.getName());
			if (foundMicroschema != null && !foundMicroschema.getUuid().equals(getUuid())) {
				handler.handle(failedFuture(BAD_REQUEST, "microschema_conflicting_name", requestModel.getName()));
				return;
			}

			db.trx(txUpdate -> {
				if (!getName().equals(requestModel.getName())) {
					setName(requestModel.getName());
				}
				setMicroschema(requestModel);
				SearchQueueBatch batch = addIndexBatch(UPDATE_ACTION);
				txUpdate.complete(batch);
			} , (AsyncResult<SearchQueueBatch> txUpdated) -> {
				if (txUpdated.failed()) {
					handler.handle(Future.failedFuture(txUpdated.cause()));
				} else {
					processOrFail2(ac, txUpdated.result(), handler);
				}
			});
		} catch (Exception e) {
			handler.handle(Future.failedFuture(e));
		}
	}

	@Override
	public void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action) {
		// TODO Auto-generated method stub
	}

	private String getJson() {
		return getProperty("json");
	}

	private void setJson(String json) {
		setProperty("json", json);
	}
}
