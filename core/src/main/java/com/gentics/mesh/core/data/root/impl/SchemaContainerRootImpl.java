package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaCreateRequest;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

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
		schemaContainer.setCreator(creator);
		schemaContainer.setCreationTimestamp(System.currentTimeMillis());
		schemaContainer.setEditor(creator);
		schemaContainer.setLastEditedTimestamp(System.currentTimeMillis());
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
	public void create(RoutingContext rc, Handler<AsyncResult<SchemaContainer>> handler) {
		MeshAuthUser requestUser = getUser(rc);
		BootstrapInitializer boot = BootstrapInitializer.getBoot();
		I18NService i18n = I18NService.getI18n();
		Database db = MeshSpringConfiguration.getMeshSpringConfiguration().database();

		SchemaCreateRequest schema;
		try {
			schema = JsonUtil.readSchema(rc.getBodyAsString(), SchemaCreateRequest.class);
			if (StringUtils.isEmpty(schema.getName())) {
				handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "schema_missing_name"))));
				return;
			}
			SchemaContainerRoot root = boot.schemaContainerRoot();
			if (requestUser.hasPermission(root, CREATE_PERM)) {
				SchemaContainer container;
				try (Trx txCreate = new Trx(db)) {
					container = root.create(schema, requestUser);
					requestUser.addCRUDPermissionOnRole(root, CREATE_PERM, container);
					txCreate.success();
				}
				handler.handle(Future.succeededFuture(container));
			}
		} catch (Exception e1) {
			handler.handle(Future.failedFuture(e1));
		}

	}

}
