package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER_ITEM;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static com.gentics.mesh.madl.type.EdgeTypeDefinition.edgeType;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.microschema.MicroschemaModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;

/**
 * @see MicroschemaContainerRoot
 */
public class MicroschemaContainerRootImpl extends AbstractRootVertex<MicroschemaContainer> implements MicroschemaContainerRoot {

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(MicroschemaContainerRootImpl.class, MeshVertexImpl.class);
		type.createType(edgeType(HAS_SCHEMA_CONTAINER_ITEM));
		index.createIndex(edgeIndex(HAS_SCHEMA_CONTAINER_ITEM).withInOut().withOut());
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
	public void addMicroschema(User user, MicroschemaContainer container, EventQueueBatch batch) {
		addItem(container);
	}

	@Override
	public void removeMicroschema(MicroschemaContainer container, EventQueueBatch batch) {
		removeItem(container);
	}

	@Override
	public MicroschemaContainer create(MicroschemaModel microschema, User user, String uuid, EventQueueBatch batch) {
		microschema.validate();

		String name = microschema.getName();
		MicroschemaContainer conflictingMicroSchema = findByName(name);
		if (conflictingMicroSchema != null) {
			throw conflict(conflictingMicroSchema.getUuid(), name, "microschema_conflicting_name", name);
		}

		SchemaContainer conflictingSchema = mesh().boot().schemaContainerRoot().findByName(name);
		if (conflictingSchema != null) {
			throw conflict(conflictingSchema.getUuid(), name, "schema_conflicting_name", name);
		}

		MicroschemaContainer container = getGraph().addFramedVertex(MicroschemaContainerImpl.class);
		if (uuid != null) {
			container.setUuid(uuid);
		}
		MicroschemaContainerVersion version = getGraph().addFramedVertex(MicroschemaContainerVersionImpl.class);

		microschema.setVersion("1.0");
		container.setLatestVersion(version);
		version.setName(microschema.getName());
		version.setSchema(microschema);
		version.setSchemaContainer(container);
		container.setCreated(user);
		container.setName(microschema.getName());
		addMicroschema(user, container, batch);

		return container;
	}

	@Override
	public void delete(BulkActionContext bac) {
		throw new NotImplementedException("The microschema container root can't be deleted");
	}

	@Override
	public MicroschemaContainer create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		UserRoot userRoot = mesh().boot().userRoot();
		MeshAuthUser requestUser = ac.getUser();
		MicroschemaModel microschema = JsonUtil.readValue(ac.getBodyAsString(), MicroschemaModelImpl.class);
		microschema.validate();
		if (!userRoot.hasPermission(requestUser, this, GraphPermission.CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", getUuid(), CREATE_PERM.getRestPerm().getName());
		}
		MicroschemaContainer container = create(microschema, requestUser, uuid, batch);
		userRoot.inheritRolePermissions(requestUser, this, container);
		batch.add(container.onCreated());
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
