package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER_ITEM;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.microschema.MicroschemaModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;
import com.gentics.mesh.json.JsonUtil;

/**
 * @see MicroschemaContainerRoot
 */
public class MicroschemaContainerRootImpl extends AbstractRootVertex<MicroschemaContainer> implements MicroschemaContainerRoot {

	public static void init(LegacyDatabase database) {
		database.addVertexType(MicroschemaContainerRootImpl.class, MeshVertexImpl.class);
		database.addEdgeType(HAS_SCHEMA_CONTAINER_ITEM);
		database.addEdgeIndex(HAS_SCHEMA_CONTAINER_ITEM, true, false, true);
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
	public void addMicroschema(User user, MicroschemaContainer container) {
		addItem(container);
	}

	@Override
	public void removeMicroschema(MicroschemaContainer container) {
		removeItem(container);
	}

	@Override
	public MicroschemaContainer create(MicroschemaModel microschema, User user, String uuid) {
		microschema.validate();

		String name = microschema.getName();
		MicroschemaContainer conflictingSchema = findByName(name);
		if (conflictingSchema != null) {
			throw conflict(conflictingSchema.getUuid(), name, "microschema_conflicting_name", name);
		}

		MicroschemaContainer container = createVertex(MicroschemaContainerImpl.class);
		if (uuid != null) {
			container.setUuid(uuid);
		}
		MicroschemaContainerVersion version = createVertex(MicroschemaContainerVersionImpl.class);

		microschema.setVersion("1.0");
		container.setLatestVersion(version);
		version.setName(microschema.getName());
		version.setSchema(microschema);
		version.setSchemaContainer(container);
		container.setCreated(user);
		container.setName(microschema.getName());
		addMicroschema(user, container);

		return container;
	}

	@Override
	public void delete(BulkActionContext batch) {
		throw new NotImplementedException();
	}

	@Override
	public MicroschemaContainer create(InternalActionContext ac, SearchQueueBatch batch, String uuid) {
		MeshAuthUser requestUser = ac.getUser();
		MicroschemaModel microschema = JsonUtil.readValue(ac.getBodyAsString(), MicroschemaModelImpl.class);
		microschema.validate();
		if (!requestUser.hasPermission(this, GraphPermission.CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", getUuid(), CREATE_PERM.getRestPerm().getName());
		}
		MicroschemaContainer container = create(microschema, requestUser, uuid);
		requestUser.addCRUDPermissionOnRole(this, CREATE_PERM, container);
		batch.store(container, true);
		return container;

	}

	@Override
	public MicroschemaContainerVersion fromReference(MicroschemaReference reference) {
		return fromReference(reference, null);
	}

	@Override
	public MicroschemaContainerVersion fromReference(MicroschemaReference reference, Branch branch) {
		String microschemaName = reference.getName();
		String microschemaUuid = reference.getUuid();
		String version = branch == null ? reference.getVersion() : null;
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

		if (branch != null) {
			foundVersion = branch.findLatestMicroschemaVersion(container);
		} else if (version != null) {
			foundVersion = container.findVersionByRev(version);
		} else {
			foundVersion = container.getLatestVersion();
		}

		if (foundVersion == null) {
			throw error(BAD_REQUEST, "error_microschema_reference_not_found", isEmpty(microschemaName) ? "-" : microschemaName,
				isEmpty(microschemaUuid) ? "-" : microschemaUuid, version == null ? "-" : version.toString());
		}
		return foundVersion;
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
