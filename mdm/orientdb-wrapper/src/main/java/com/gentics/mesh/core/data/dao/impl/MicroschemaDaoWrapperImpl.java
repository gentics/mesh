package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.function.Predicate;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionProperties;
import com.gentics.mesh.core.data.impl.MicroschemaWrapper;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.MicroschemaRoot;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
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
	public Microschema create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		UserDaoWrapper userRoot = Tx.get().data().userDao();
		MicroschemaRoot microschemaRoot = boot.get().microschemaContainerRoot();

		MeshAuthUser requestUser = ac.getUser();
		MicroschemaVersionModel microschema = JsonUtil.readValue(ac.getBodyAsString(), MicroschemaModelImpl.class);
		microschema.validate();
		if (!userRoot.hasPermission(requestUser, microschemaRoot, InternalPermission.CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", microschemaRoot.getUuid(), CREATE_PERM.getRestPerm().getName());
		}
		Microschema container = create(microschema, requestUser, uuid, batch);
		userRoot.inheritRolePermissions(requestUser, microschemaRoot, container);
		batch.add(container.onCreated());
		return container;
	}

	@Override
	public Microschema create(MicroschemaVersionModel microschema, HibUser user, String uuid, EventQueueBatch batch) {
		microschema.validate();

		SchemaDaoWrapper schemaDao = Tx.get().data().schemaDao();
		MicroschemaRoot microschemaRoot = boot.get().microschemaContainerRoot();

		String name = microschema.getName();
		Microschema conflictingMicroSchema = findByName(name);
		if (conflictingMicroSchema != null) {
			throw conflict(conflictingMicroSchema.getUuid(), name, "microschema_conflicting_name", name);
		}

		Schema conflictingSchema = schemaDao.findByName(name);
		if (conflictingSchema != null) {
			throw conflict(conflictingSchema.getUuid(), name, "schema_conflicting_name", name);
		}

		Microschema container = microschemaRoot.create();
		if (uuid != null) {
			container.setUuid(uuid);
		}
		MicroschemaVersion version = microschemaRoot.createVersion();

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
	public MicroschemaVersion fromReference(HibProject project, MicroschemaReference reference, HibBranch branch) {
		String microschemaName = reference.getName();
		String microschemaUuid = reference.getUuid();
		String version = branch == null ? reference.getVersion() : null;
		Microschema container = null;
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

		MicroschemaVersion foundVersion = null;

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
	public Microschema findByName(String name) {
		MicroschemaRoot microschemaRoot = boot.get().microschemaContainerRoot();
		return MicroschemaWrapper.wrap(microschemaRoot.findByName(name));
	}

	@Override
	public Microschema findByUuid(String uuid) {
		MicroschemaRoot microschemaRoot = boot.get().microschemaContainerRoot();
		return MicroschemaWrapper.wrap(microschemaRoot.findByUuid(uuid));
	}

	@Override
	public TraversalResult<? extends Microschema> findAll() {
		MicroschemaRoot microschemaRoot = boot.get().microschemaContainerRoot();
		return microschemaRoot.findAll();
	}

	@Override
	public TransformablePage<? extends Microschema> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		MicroschemaRoot microschemaRoot = boot.get().microschemaContainerRoot();
		return microschemaRoot.findAll(ac, pagingInfo);
	}

	@Override
	public Page<? extends Microschema> findAll(InternalActionContext ac, PagingParameters pagingInfo,
		Predicate<Microschema> extraFilter) {
		MicroschemaRoot microschemaRoot = boot.get().microschemaContainerRoot();
		return microschemaRoot.findAll(ac, pagingInfo, extraFilter);
	}

	@Override
	public Microschema loadObjectByUuid(InternalActionContext ac, String schemaUuid, InternalPermission perm) {
		// TODO check for project in context?
		MicroschemaRoot microschemaRoot = boot.get().microschemaContainerRoot();
		return MicroschemaWrapper.wrap(microschemaRoot.loadObjectByUuid(ac, schemaUuid, perm));
	}

	@Override
	public Microschema loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		// TODO check for project in context?
		MicroschemaRoot microschemaRoot = boot.get().microschemaContainerRoot();
		return MicroschemaWrapper.wrap(microschemaRoot.loadObjectByUuid(ac, uuid, perm, errorIfNotFound));
	}
	
	@Override
	public void delete(Microschema microschema, BulkActionContext bac) {
		for (MicroschemaVersion version : microschema.findAll()) {
			if (version.findMicronodes().hasNext()) {
				throw error(BAD_REQUEST, "microschema_delete_still_in_use", microschema.getUuid());
			}
			version.delete(bac);
		}
		microschema.delete(bac);
	}

}
