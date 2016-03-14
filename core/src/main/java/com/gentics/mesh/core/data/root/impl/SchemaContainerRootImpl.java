package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER_ITEM;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.collect.Tuple;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import rx.Observable;

public class SchemaContainerRootImpl extends AbstractRootVertex<SchemaContainer> implements SchemaContainerRoot {

	private static final Logger log = LoggerFactory.getLogger(SchemaContainerRootImpl.class);

	public static void checkIndices(Database database) {
		database.addVertexType(SchemaContainerRootImpl.class);
	}

	@Override
	public Class<? extends SchemaContainer> getPersistanceClass() {
		return SchemaContainerImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_SCHEMA_CONTAINER_ITEM;
	}

	@Override
	public void addSchemaContainer(SchemaContainer schema) {
		addItem(schema);
	}

	@Override
	public void removeSchemaContainer(SchemaContainer schemaContainer) {
		removeItem(schemaContainer);
	}

	@Override
	public SchemaContainer create(Schema schema, User creator) throws MeshSchemaException {
		validate(schema);
		SchemaContainerImpl container = getGraph().addFramedVertex(SchemaContainerImpl.class);
		SchemaContainerVersion version = getGraph().addFramedVertex(SchemaContainerVersionImpl.class);
		container.setLatestVersion(version);

		// set the initial version
		schema.setVersion(1);
		version.setSchema(schema);
		version.setName(schema.getName());
		version.setSchemaContainer(container);
		container.setCreated(creator);
		container.setName(schema.getName());

		addSchemaContainer(container);
		return container;
	}

	private void validate(Schema schema) throws HttpStatusCodeErrorException {
		if (StringUtils.isEmpty(schema.getDisplayField())) {
			throw error(BAD_REQUEST, "The displayField must not be empty");
		}

	}

	@Override
	public boolean contains(SchemaContainer schema) {
		// TODO this is not optimal
		if (findByName(schema.getName()).toBlocking().single() == null) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public void delete() {
		// TODO maybe we should add a check here to prevent deletion of the meshroot.schemaRoot ?
		if (log.isDebugEnabled()) {
			log.debug("Deleting schema container root {" + getUuid() + "}");
		}
		getElement().remove();
	}

	@Override
	public Observable<SchemaContainer> create(InternalActionContext ac) {
		MeshAuthUser requestUser = ac.getUser();
		Database db = MeshSpringConfiguration.getInstance().database();
		ObservableFuture<SchemaContainer> obsFut = RxHelper.observableFuture();

		Schema requestModel;
		try {
			requestModel = JsonUtil.readSchema(ac.getBodyAsString(), SchemaModel.class);
			requestModel.validate();
			if (requestUser.hasPermissionSync(ac, this, CREATE_PERM)) {

				Tuple<SearchQueueBatch, SchemaContainer> tuple = db.trx(() -> {

					String schemaName = requestModel.getName();
					SchemaContainer conflictingSchema = findByName(schemaName).toBlocking().last();
					if (conflictingSchema != null) {
						throw conflict(conflictingSchema.getUuid(), schemaName, "schema_conflicting_name", schemaName);
					}

					requestUser.reload();
					SchemaContainer container = create(requestModel, requestUser);
					requestUser.addCRUDPermissionOnRole(this, CREATE_PERM, container);
					SearchQueueBatch batch = container.addIndexBatch(SearchQueueEntryAction.CREATE_ACTION);
					return Tuple.tuple(batch, container);
				});

				SearchQueueBatch batch = tuple.v1();
				SchemaContainer createdContainer = tuple.v2();

				return batch.process().map(done -> createdContainer);
			}
		} catch (Exception e1) {
			return Observable.error(e1);
		}
		return obsFut;

	}

}
