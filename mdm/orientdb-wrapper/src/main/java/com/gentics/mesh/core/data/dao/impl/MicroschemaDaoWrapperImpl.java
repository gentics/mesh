package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionProperties;
import com.gentics.mesh.core.data.impl.MicroschemaWrapper;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.microschema.MicroschemaModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;

import dagger.Lazy;

public class MicroschemaDaoWrapperImpl extends AbstractDaoWrapper implements MicroschemaDaoWrapper {

	@Inject
	public MicroschemaDaoWrapperImpl(Lazy<BootstrapInitializer> boot, Lazy<PermissionProperties> permissions) {
		super(boot, permissions);
	}

	@Override
	public MicroschemaContainer create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		UserDaoWrapper userRoot = Tx.get().data().userDao();
		MicroschemaContainerRoot microschemaRoot = boot.get().microschemaContainerRoot();

		MeshAuthUser requestUser = ac.getUser();
		MicroschemaModel microschema = JsonUtil.readValue(ac.getBodyAsString(), MicroschemaModelImpl.class);
		microschema.validate();
		if (!userRoot.hasPermission(requestUser, microschemaRoot, GraphPermission.CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", microschemaRoot.getUuid(), CREATE_PERM.getRestPerm().getName());
		}
		MicroschemaContainer container = create(microschema, requestUser, uuid, batch);
		userRoot.inheritRolePermissions(requestUser, microschemaRoot, container);
		batch.add(container.onCreated());
		return container;
	}

	@Override
	public MicroschemaContainer create(MicroschemaModel microschema, User user, String uuid, EventQueueBatch batch) {
		microschema.validate();

		SchemaDaoWrapper schemaDao = Tx.get().data().schemaDao();
		MicroschemaContainerRoot microschemaRoot = boot.get().microschemaContainerRoot();

		String name = microschema.getName();
		MicroschemaContainer conflictingMicroSchema = findByName(name);
		if (conflictingMicroSchema != null) {
			throw conflict(conflictingMicroSchema.getUuid(), name, "microschema_conflicting_name", name);
		}

		SchemaContainer conflictingSchema = schemaDao.findByName(name);
		if (conflictingSchema != null) {
			throw conflict(conflictingSchema.getUuid(), name, "schema_conflicting_name", name);
		}

		MicroschemaContainer container = microschemaRoot.create();
		if (uuid != null) {
			container.setUuid(uuid);
		}
		MicroschemaContainerVersion version = microschemaRoot.createVersion();

		microschema.setVersion("1.0");
		container.setLatestVersion(version);
		version.setName(microschema.getName());
		version.setSchema(microschema);
		version.setSchemaContainer(container);
		container.setCreated(user);
		container.setName(microschema.getName());
		microschemaRoot.addMicroschema(user, container, batch);

		return container;
	}

	@Override
	public MicroschemaContainerVersion fromReference(Project project, MicroschemaReference reference, Branch branch) {
		String microschemaName = reference.getName();
		String microschemaUuid = reference.getUuid();
		String version = branch == null ? reference.getVersion() : null;
		MicroschemaContainer container = null;
		if (!isEmpty(microschemaName)) {
			if (project != null) {
				container = project.getMicroschemaContainerRoot().findByName(microschemaName);
			} else {
				container = findByName(microschemaName);
			}
		} else {
			if (project != null) {
				container = project.getMicroschemaContainerRoot().findByUuid(microschemaUuid);
			} else {
				container = findByUuid(microschemaUuid);
			}
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
	public MicroschemaContainer findByName(String name) {
		MicroschemaContainerRoot microschemaRoot = boot.get().microschemaContainerRoot();
		return MicroschemaWrapper.wrap(microschemaRoot.findByName(name));
	}

	@Override
	public MicroschemaContainer findByUuid(String uuid) {
		MicroschemaContainerRoot microschemaRoot = boot.get().microschemaContainerRoot();
		return MicroschemaWrapper.wrap(microschemaRoot.findByUuid(uuid));
	}

	@Override
	public TraversalResult<? extends MicroschemaContainer> findAll() {
		MicroschemaContainerRoot microschemaRoot = boot.get().microschemaContainerRoot();
		return microschemaRoot.findAll();
	}

	@Override
	public TransformablePage<? extends MicroschemaContainer> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		MicroschemaContainerRoot microschemaRoot = boot.get().microschemaContainerRoot();
		return microschemaRoot.findAll(ac, pagingInfo);
	}

	@Override
	public MicroschemaContainer loadObjectByUuid(InternalActionContext ac, String schemaUuid, GraphPermission perm) {
		// TODO check for project in context?
		MicroschemaContainerRoot microschemaRoot = boot.get().microschemaContainerRoot();
		return MicroschemaWrapper.wrap(microschemaRoot.loadObjectByUuid(ac, schemaUuid, perm));
	}

	@Override
	public MicroschemaContainer loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		// TODO check for project in context?
		MicroschemaContainerRoot microschemaRoot = boot.get().microschemaContainerRoot();
		return MicroschemaWrapper.wrap(microschemaRoot.loadObjectByUuid(ac, uuid, perm, errorIfNotFound));
	}

}
