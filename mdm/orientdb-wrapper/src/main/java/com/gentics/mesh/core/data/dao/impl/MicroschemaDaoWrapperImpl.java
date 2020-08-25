package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.util.HibClassConverter.toMicroschema;
import static com.gentics.mesh.core.data.util.HibClassConverter.toMicroschemaVersion;
import static com.gentics.mesh.core.data.util.HibClassConverter.toProject;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Map;
import java.util.function.Predicate;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionProperties;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.MicroschemaRoot;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;

import dagger.Lazy;

public class MicroschemaDaoWrapperImpl extends AbstractDaoWrapper implements MicroschemaDaoWrapper {

	private final MicroschemaComparator comparator;

	@Inject
	public MicroschemaDaoWrapperImpl(Lazy<BootstrapInitializer> boot, Lazy<PermissionProperties> permissions, MicroschemaComparator comparator) {
		super(boot, permissions);
		this.comparator = comparator;
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

		HibSchema conflictingSchema = schemaDao.findByName(name);
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
	public HibMicroschemaVersion fromReference(HibProject project, MicroschemaReference reference, HibBranch branch) {
		String microschemaName = reference.getName();
		String microschemaUuid = reference.getUuid();
		String version = branch == null ? reference.getVersion() : null;
		HibMicroschema container = null;
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

		HibMicroschemaVersion foundVersion = null;

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
		return microschemaRoot.findByName(name);
	}

	@Override
	public HibMicroschema findByUuid(String uuid) {
		MicroschemaRoot microschemaRoot = boot.get().microschemaContainerRoot();
		return microschemaRoot.findByUuid(uuid);
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
	public HibMicroschema loadObjectByUuid(InternalActionContext ac, String schemaUuid, InternalPermission perm) {
		// TODO check for project in context?
		MicroschemaRoot microschemaRoot = boot.get().microschemaContainerRoot();
		return microschemaRoot.loadObjectByUuid(ac, schemaUuid, perm);
	}

	@Override
	public HibMicroschema loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		// TODO check for project in context?
		MicroschemaRoot microschemaRoot = boot.get().microschemaContainerRoot();
		return microschemaRoot.loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	@Override
	public void delete(HibMicroschema microschema, BulkActionContext bac) {
		Microschema graphMicroschema = toMicroschema(microschema);
		for (MicroschemaVersion version : graphMicroschema.findAll()) {
			if (version.findMicronodes().hasNext()) {
				throw error(BAD_REQUEST, "microschema_delete_still_in_use", microschema.getUuid());
			}
			version.delete(bac);
		}
		graphMicroschema.delete(bac);
	}

	@Override
	public boolean isLinkedToProject(HibMicroschema microschema, HibProject project) {
		Project graphProject = toProject(project);
		Microschema graphMicroschema = toMicroschema(microschema);
		MicroschemaRoot root = graphProject.getMicroschemaContainerRoot();
		return root.contains(graphMicroschema);
	}

	@Override
	public HibMicroschemaVersion applyChanges(HibMicroschemaVersion version, InternalActionContext ac, EventQueueBatch batch) {
		MicroschemaVersion graphMicroschemaVersion = toMicroschemaVersion(version);
		return graphMicroschemaVersion.applyChanges(ac, batch);
	}

	@Override
	public HibMicroschemaVersion applyChanges(HibMicroschemaVersion version, InternalActionContext ac, SchemaChangesListModel model,
		EventQueueBatch batch) {
		MicroschemaVersion graphMicroschemaVersion = toMicroschemaVersion(version);
		return graphMicroschemaVersion.applyChanges(ac, model, batch);
	}

	@Override
	public SchemaChangesListModel diff(HibMicroschemaVersion version, InternalActionContext ac, MicroschemaModel requestModel) {
		MicroschemaVersion graphVersion = toMicroschemaVersion(version);
		return graphVersion.diff(ac, comparator, requestModel);
	}

	@Override
	public Iterable<? extends HibMicroschemaVersion> findAllVersions(HibMicroschema microschema) {
		return toMicroschema(microschema).findAll();
	}

	@Override
	public Map<HibBranch, HibMicroschemaVersion> findReferencedBranches(HibMicroschema microschema) {
		Map<?, ?> map = toMicroschema(microschema).findReferencedBranches();
		return (Map<HibBranch, HibMicroschemaVersion>) map;
	}

	@Override
	public TraversalResult<? extends NodeGraphFieldContainer> findDraftFieldContainers(HibMicroschemaVersion version, String branchUuid) {
		return toMicroschemaVersion(version).getDraftFieldContainers(branchUuid);
	}

	@Override
	public MicroschemaResponse transformToRestSync(HibMicroschema microschema, InternalActionContext ac, int level, String... languageTags) {
		return toMicroschema(microschema).transformToRestSync(ac, level, languageTags);
	}

	@Override
	public void unlink(HibMicroschema microschema, HibProject project, EventQueueBatch batch) {
		toProject(project).getMicroschemaContainerRoot().removeMicroschema(toMicroschema(microschema), batch);
	}

	@Override
	public String getETag(HibMicroschema schema, InternalActionContext ac) {
		return toMicroschema(schema).getETag(ac);
	}

}
