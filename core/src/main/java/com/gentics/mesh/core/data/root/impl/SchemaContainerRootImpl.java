package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER_ITEM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_ROOT;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Single;

public class SchemaContainerRootImpl extends AbstractRootVertex<SchemaContainer> implements SchemaContainerRoot {

	private static final Logger log = LoggerFactory.getLogger(SchemaContainerRootImpl.class);

	public static void init(Database database) {
		database.addVertexType(SchemaContainerRootImpl.class, MeshVertexImpl.class);
		database.addEdgeType(HAS_SCHEMA_ROOT);
		database.addEdgeType(HAS_SCHEMA_CONTAINER_ITEM);
		database.addEdgeIndex(HAS_SCHEMA_CONTAINER_ITEM, true, false, true);
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
	public SchemaContainer create(Schema schema, User creator) {
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

	private void validate(Schema schema) throws GenericRestException {
		if (StringUtils.isEmpty(schema.getDisplayField())) {
			throw error(BAD_REQUEST, "The displayField must not be empty");
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
	public void delete(SearchQueueBatch batch) {
		// TODO maybe we should add a check here to prevent deletion of the meshroot.schemaRoot ?
		if (log.isDebugEnabled()) {
			log.debug("Deleting schema container root {" + getUuid() + "}");
		}
		getElement().remove();
	}

	@Override
	public SchemaContainer create(InternalActionContext ac, SearchQueueBatch batch) {
		MeshAuthUser requestUser = ac.getUser();
		Schema requestModel = JsonUtil.readValue(ac.getBodyAsString(), SchemaModel.class);
		requestModel.validate();
		if (!requestUser.hasPermission(this, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", getUuid());
		}

		String schemaName = requestModel.getName();
		SchemaContainer conflictingSchema = findByName(schemaName);
		if (conflictingSchema != null) {
			throw conflict(conflictingSchema.getUuid(), schemaName, "schema_conflicting_name", schemaName);
		}

		SchemaContainer container = create(requestModel, requestUser);
		requestUser.addCRUDPermissionOnRole(this, CREATE_PERM, container);
		container.addIndexBatchEntry(batch, STORE_ACTION);
		return container;

	}

	@Override
	public Single<SchemaContainerVersion> fromReference(SchemaReference reference) {
		if (reference == null) {
			return Single.error(error(INTERNAL_SERVER_ERROR, "Missing schema reference"));
		}
		String schemaName = reference.getName();
		String schemaUuid = reference.getUuid();
		Integer schemaVersion = reference.getVersion();

		SchemaContainer schemaContainer = null;
		if (!isEmpty(schemaName)) {
			schemaContainer = findByName(schemaName);
		} else {
			schemaContainer = findByUuid(schemaUuid);
		}

		if (schemaContainer == null) {
			throw error(BAD_REQUEST, "error_schema_reference_not_found", isEmpty(schemaName) ? "-" : schemaName,
					isEmpty(schemaUuid) ? "-" : schemaUuid, schemaVersion == null ? "-" : schemaVersion.toString());
		}
		if (schemaVersion == null) {
			return Single.just(schemaContainer.getLatestVersion());
		} else {
			SchemaContainerVersion foundVersion = schemaContainer.findVersionByRev(schemaVersion);
			if (foundVersion == null) {
				throw error(BAD_REQUEST, "error_schema_reference_not_found", isEmpty(schemaName) ? "-" : schemaName,
						isEmpty(schemaUuid) ? "-" : schemaUuid, schemaVersion == null ? "-" : schemaVersion.toString());
			} else {
				return Single.just(foundVersion);
			}
		}
	}
}
