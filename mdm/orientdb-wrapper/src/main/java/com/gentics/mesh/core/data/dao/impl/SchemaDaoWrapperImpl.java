package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Provider;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionProperties;
import com.gentics.mesh.core.data.impl.SchemaWrapper;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.search.index.node.NodeIndexHandler;

import dagger.Lazy;
import io.vertx.core.Vertx;

public class SchemaDaoWrapperImpl extends AbstractDaoWrapper implements SchemaDaoWrapper {

	private Lazy<Vertx> vertx;
	private Lazy<NodeIndexHandler> nodeIndexHandler;
	private Provider<EventQueueBatch> batchProvider;

	@Inject
	public SchemaDaoWrapperImpl(Lazy<BootstrapInitializer> boot, Lazy<PermissionProperties> permissions, Lazy<Vertx> vertx,
		Lazy<NodeIndexHandler> nodeIndexHandler, Provider<EventQueueBatch> batchProvider) {
		super(boot, permissions);
		this.vertx = vertx;
		this.nodeIndexHandler = nodeIndexHandler;
		this.batchProvider = batchProvider;
	}

	@Override
	public SchemaContainer findByName(String name) {
		SchemaContainerRoot schemaRoot = boot.get().schemaContainerRoot();
		return SchemaWrapper.wrap(schemaRoot.findByName(name));
	}

	@Override
	public SchemaContainer findByUuid(String uuid) {
		SchemaContainerRoot schemaRoot = boot.get().schemaContainerRoot();
		return SchemaWrapper.wrap(schemaRoot.findByUuid(uuid));
	}

	@Override
	public TraversalResult<? extends SchemaContainer> findAll() {
		SchemaContainerRoot schemaRoot = boot.get().schemaContainerRoot();
		return schemaRoot.findAll();
	}

	@Override
	public long computeCount() {
		return boot.get().schemaContainerRoot().computeCount();
	}

	@Override
	public TransformablePage<? extends SchemaContainer> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		SchemaContainerRoot schemaRoot = boot.get().schemaContainerRoot();
		return schemaRoot.findAll(ac, pagingInfo);
	}

	@Override
	public SchemaContainer create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		MeshAuthUser requestUser = ac.getUser();
		UserDaoWrapper userDao = Tx.get().data().userDao();
		SchemaContainerRoot schemaRoot = boot.get().schemaContainerRoot();

		SchemaModel requestModel = JsonUtil.readValue(ac.getBodyAsString(), SchemaModelImpl.class);
		requestModel.validate();

		if (!userDao.hasPermission(requestUser, schemaRoot, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", schemaRoot.getUuid(), CREATE_PERM.getRestPerm().getName());
		}
		SchemaContainer container = create(requestModel, requestUser, uuid, ac.getSchemaUpdateParameters().isStrictValidation());
		userDao.inheritRolePermissions(requestUser, schemaRoot, container);
		batch.add(container.onCreated());
		return container;

	}

	@Override
	public SchemaContainerVersion fromReference(Project project, SchemaReference reference) {
		if (reference == null) {
			throw error(INTERNAL_SERVER_ERROR, "Missing schema reference");
		}
		String schemaName = reference.getName();
		String schemaUuid = reference.getUuid();
		String schemaVersion = reference.getVersion();

		// Prefer the name over the uuid
		SchemaContainer schemaContainer = null;
		if (!isEmpty(schemaName)) {
			if (project != null) {
				schemaContainer = findByName(project, schemaName);
			} else {
				schemaContainer = findByName(schemaName);
			}
		} else {
			if (project != null) {
				schemaContainer = findByUuid(project, schemaUuid);
			} else {
				schemaContainer = findByUuid(schemaUuid);
			}
		}

		// Check whether a container was actually found
		if (schemaContainer == null) {
			throw error(BAD_REQUEST, "error_schema_reference_not_found", isEmpty(schemaName) ? "-" : schemaName, isEmpty(schemaUuid) ? "-"
				: schemaUuid, schemaVersion == null ? "-" : schemaVersion.toString());
		}
		if (schemaVersion == null) {
			return schemaContainer.getLatestVersion();
		} else {
			SchemaContainerVersion foundVersion = schemaContainer.findVersionByRev(schemaVersion);
			if (foundVersion == null) {
				throw error(BAD_REQUEST, "error_schema_reference_not_found", isEmpty(schemaName) ? "-" : schemaName, isEmpty(schemaUuid) ? "-"
					: schemaUuid, schemaVersion == null ? "-" : schemaVersion.toString());
			} else {
				return foundVersion;
			}
		}

	}

	@Override
	public SchemaContainer findByUuid(Project project, String schemaUuid) {
		SchemaContainer schema = project.getSchemaContainerRoot().findByUuid(schemaUuid);
		return SchemaWrapper.wrap(schema);
	}

	@Override
	public SchemaContainer findByName(Project project, String schemaName) {
		SchemaContainer schema = project.getSchemaContainerRoot().findByName(schemaName);
		return SchemaWrapper.wrap(schema);
	}

	@Override
	public SchemaContainerVersion fromReference(SchemaReference reference) {
		return fromReference(null, reference);
	}

	@Override
	public SchemaContainer create(SchemaModel schema, User creator, String uuid) {
		return create(schema, creator, uuid, false);
	}

	@Override
	public SchemaContainer create(SchemaModel schema, User creator, String uuid, boolean validate) {
		SchemaContainerRoot schemaRoot = boot.get().schemaContainerRoot();
		MicroschemaDaoWrapper microschemaDao = Tx.get().data().microschemaDao();

		// TODO FIXME - We need to skip the validation check if the instance is creating a clustered instance because vert.x is not yet ready.
		// https://github.com/gentics/mesh/issues/210
		if (validate && vertx.get() != null) {
			validateSchema(nodeIndexHandler.get(), schema);
		}

		String name = schema.getName();
		SchemaContainer conflictingSchema = findByName(name);
		if (conflictingSchema != null) {
			throw conflict(conflictingSchema.getUuid(), name, "schema_conflicting_name", name);
		}

		MicroschemaContainer conflictingMicroschema = microschemaDao.findByName(name);
		if (conflictingMicroschema != null) {
			throw conflict(conflictingMicroschema.getUuid(), name, "microschema_conflicting_name", name);
		}

		SchemaContainer container = schemaRoot.create();
		if (uuid != null) {
			container.setUuid(uuid);
		}
		SchemaContainerVersion version = schemaRoot.createVersion();
		container.setLatestVersion(version);

		// set the initial version
		schema.setVersion("1.0");
		version.setSchema(schema);
		version.setName(schema.getName());
		version.setSchemaContainer(container);
		container.setCreated(creator);
		container.setName(schema.getName());

		schemaRoot.addSchemaContainer(creator, container, null);
		return container;
	}

	public static void validateSchema(NodeIndexHandler indexHandler, SchemaModel schema) {
		// TODO Maybe set the timeout to the configured search.timeout? But the default of 60 seconds is really long.
		Throwable error = indexHandler.validate(schema).blockingGet(10, TimeUnit.SECONDS);

		if (error != null) {
			if (error instanceof GenericRestException) {
				throw (GenericRestException) error;
			} else {
				throw new RuntimeException(error);
			}
		}
	}

	@Override
	public SchemaContainer loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm) {
		// TODO check for project in context?
		SchemaContainerRoot schemaRoot = boot.get().schemaContainerRoot();
		return SchemaWrapper.wrap(schemaRoot.loadObjectByUuid(ac, uuid, perm));
	}

	@Override
	public SchemaContainer loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		// TODO check for project in context?
		SchemaContainerRoot schemaRoot = boot.get().schemaContainerRoot();
		return SchemaWrapper.wrap(schemaRoot.loadObjectByUuid(ac, uuid, perm, errorIfNotFound));
	}

}
