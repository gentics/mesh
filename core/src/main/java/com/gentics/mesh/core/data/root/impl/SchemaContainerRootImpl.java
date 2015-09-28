package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaCreateRequest;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class SchemaContainerRootImpl extends AbstractRootVertex<SchemaContainer>implements SchemaContainerRoot {

	private static final Logger log = LoggerFactory.getLogger(SchemaContainerRootImpl.class);

	@Override
	protected Class<? extends SchemaContainer> getPersistanceClass() {
		return SchemaContainerImpl.class;
	}

	@Override
	protected String getRootLabel() {
		return HAS_SCHEMA_CONTAINER;
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
		SchemaContainerImpl schemaContainer = getGraph().addFramedVertex(SchemaContainerImpl.class);
		schemaContainer.setSchema(schema);
		schemaContainer.setName(schema.getName());
		addSchemaContainer(schemaContainer);
		schemaContainer.setCreationTimestamp(System.currentTimeMillis());
		schemaContainer.setLastEditedTimestamp(System.currentTimeMillis());
		schemaContainer.setCreator(creator);
		schemaContainer.setEditor(creator);
		return schemaContainer;
	}

	private void validate(Schema schema) throws MeshSchemaException {
		if (StringUtils.isEmpty(schema.getDisplayField())) {
			throw new MeshSchemaException("The displayField must not be empty");
		}

	}

	@Override
	public boolean contains(SchemaContainer schema) {
		// TODO this is not optimal
		if (findByName(schema.getName()) == null) {
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
	public void create(InternalActionContext ac, Handler<AsyncResult<SchemaContainer>> handler) {
		MeshAuthUser requestUser = ac.getUser();
		Database db = MeshSpringConfiguration.getInstance().database();

		SchemaCreateRequest schema;
		try {
			schema = JsonUtil.readSchema(ac.getBodyAsString(), SchemaCreateRequest.class);
			if (StringUtils.isEmpty(schema.getName())) {
				handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, ac.i18n("schema_missing_name"))));
				return;
			}
			if (requestUser.hasPermission(ac, this, CREATE_PERM)) {
				SchemaContainer container;
				try (Trx txCreate = db.trx()) {
					requestUser.reload();
					container = create(schema, requestUser);
					requestUser.addCRUDPermissionOnRole(this, CREATE_PERM, container);
					txCreate.success();
				}
				handler.handle(Future.succeededFuture(container));
			}
		} catch (Exception e1) {
			handler.handle(Future.failedFuture(e1));
		}

	}

}
