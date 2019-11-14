package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER_ITEM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_ROOT;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static com.gentics.mesh.madl.type.EdgeTypeDefinition.edgeType;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.concurrent.TimeUnit;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.search.index.node.NodeIndexHandler;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see SchemaContainerRoot
 */
public class SchemaContainerRootImpl extends AbstractRootVertex<SchemaContainer> implements SchemaContainerRoot {

	private static final Logger log = LoggerFactory.getLogger(SchemaContainerRootImpl.class);

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(SchemaContainerRootImpl.class, MeshVertexImpl.class);
		type.createType(edgeType(HAS_SCHEMA_ROOT));
		type.createType(edgeType(HAS_SCHEMA_CONTAINER_ITEM));
		index.createIndex(edgeIndex(HAS_SCHEMA_CONTAINER_ITEM).withInOut().withOut());
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
	public void addSchemaContainer(User user, SchemaContainer schema, EventQueueBatch batch) {
		addItem(schema);
	}

	@Override
	public void removeSchemaContainer(SchemaContainer schemaContainer, EventQueueBatch batch) {
		removeItem(schemaContainer);
	}

	@Override
	public SchemaContainer create(SchemaModel schema, User creator, String uuid) {
		return create(schema, creator, uuid, false);
	}

	@Override
	public SchemaContainer create(SchemaModel schema, User creator, String uuid, boolean validate) {
		// TODO FIXME - We need to skip the validation check if the instance is creating a clustered instance because vert.x is not yet ready.
		// https://github.com/gentics/mesh/issues/210
		if (validate && vertx() != null) {
			validateSchema(mesh().nodeContainerIndexHandler(), schema);
		}

		String name = schema.getName();
		SchemaContainer conflictingSchema = findByName(name);
		if (conflictingSchema != null) {
			throw conflict(conflictingSchema.getUuid(), name, "schema_conflicting_name", name);
		}

		MicroschemaContainer conflictingMicroschema = mesh().boot().microschemaContainerRoot().findByName(name);
		if (conflictingMicroschema != null) {
			throw conflict(conflictingMicroschema.getUuid(), name, "microschema_conflicting_name", name);
		}

		SchemaContainerImpl container = getGraph().addFramedVertex(SchemaContainerImpl.class);
		if (uuid != null) {
			container.setUuid(uuid);
		}
		SchemaContainerVersion version = getGraph().addFramedVertex(SchemaContainerVersionImpl.class);
		container.setLatestVersion(version);

		// set the initial version
		schema.setVersion("1.0");
		version.setSchema(schema);
		version.setName(schema.getName());
		version.setSchemaContainer(container);
		container.setCreated(creator);
		container.setName(schema.getName());

		EventQueueBatch batch = createBatch();
		addSchemaContainer(creator, container, null);
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
	public boolean contains(SchemaContainer schema) {
		if (findByUuid(schema.getUuid()) == null) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public void delete(BulkActionContext bac) {
		if (mesh().boot().meshRoot().getSchemaContainerRoot() == this) {
			throw error(INTERNAL_SERVER_ERROR, "Deletion of the global schema root is not possible");
		}
		if (log.isDebugEnabled()) {
			log.debug("Deleting schema container root {" + getUuid() + "}");
		}
		getElement().remove();
		bac.inc();
	}

	@Override
	public SchemaContainer create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		MeshAuthUser requestUser = ac.getUser();
		SchemaModel requestModel = JsonUtil.readValue(ac.getBodyAsString(), SchemaModelImpl.class);
		requestModel.validate();

		if (!requestUser.hasPermission(this, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", getUuid(), CREATE_PERM.getRestPerm().getName());
		}
		SchemaContainer container = create(requestModel, requestUser, uuid, ac.getSchemaUpdateParameters().isStrictValidation());
		requestUser.addCRUDPermissionOnRole(this, CREATE_PERM, container);
		batch.add(container.onCreated());
		return container;

	}

	@Override
	public SchemaContainerVersion fromReference(SchemaReference reference) {
		if (reference == null) {
			throw error(INTERNAL_SERVER_ERROR, "Missing schema reference");
		}
		String schemaName = reference.getName();
		String schemaUuid = reference.getUuid();
		String schemaVersion = reference.getVersion();

		// Prefer the name over the uuid
		SchemaContainer schemaContainer = null;
		if (!isEmpty(schemaName)) {
			schemaContainer = findByName(schemaName);
		} else {
			schemaContainer = findByUuid(schemaUuid);
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

	/**
	 * Get the project
	 *
	 * @return project
	 */
	@Override
	public Project getProject() {
		return in(HAS_SCHEMA_ROOT).has(ProjectImpl.class).nextOrDefaultExplicit(ProjectImpl.class, null);
	}
}
