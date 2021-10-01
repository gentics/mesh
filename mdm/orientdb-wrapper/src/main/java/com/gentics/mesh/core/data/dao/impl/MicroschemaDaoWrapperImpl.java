package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.generic.PermissionPropertiesImpl;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.root.MicroschemaRoot;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.PagingParameters;

import dagger.Lazy;

/**
 * @see MicroschemaDaoWrapper
 */
public class MicroschemaDaoWrapperImpl extends AbstractDaoWrapper<HibMicroschema> implements MicroschemaDaoWrapper {

	private final MicroschemaComparator comparator;

	@Inject
	public MicroschemaDaoWrapperImpl(Lazy<OrientDBBootstrapInitializer> boot, Lazy<PermissionPropertiesImpl> permissions,
		MicroschemaComparator comparator) {
		super(boot, permissions);
		this.comparator = comparator;
	}

	@Override
	public HibMicroschema create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		UserDao userRoot = Tx.get().userDao();
		MicroschemaRoot microschemaRoot = boot.get().meshRoot().getMicroschemaContainerRoot();

		HibUser requestUser = ac.getUser();
		MicroschemaVersionModel microschema = JsonUtil.readValue(ac.getBodyAsString(), MicroschemaModelImpl.class);
		microschema.validate();
		if (!userRoot.hasPermission(requestUser, microschemaRoot, InternalPermission.CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", microschemaRoot.getUuid(),
				CREATE_PERM.getRestPerm().getName());
		}
		HibMicroschema container = create(microschema, requestUser, uuid, batch);
		userRoot.inheritRolePermissions(requestUser, microschemaRoot, container);
		batch.add(container.onCreated());
		return container;
	}

	@Override
	public HibMicroschema create(MicroschemaVersionModel microschema, HibUser user, String uuid,
		EventQueueBatch batch) {
		microschema.validate();

		SchemaDao schemaDao = Tx.get().schemaDao();
		MicroschemaRoot microschemaRoot = boot.get().meshRoot().getMicroschemaContainerRoot();

		String name = microschema.getName();
		Microschema conflictingMicroSchema = findByName(name);
		if (conflictingMicroSchema != null) {
			throw conflict(conflictingMicroSchema.getUuid(), name, "microschema_conflicting_name", name);
		}

		HibSchema conflictingSchema = schemaDao.findByName(name);
		if (conflictingSchema != null) {
			throw conflict(conflictingSchema.getUuid(), name, "schema_conflicting_name", name);
		}

		HibMicroschema container = microschemaRoot.create();
		if (uuid != null) {
			toGraph(container).setUuid(uuid);
		}
		HibMicroschemaVersion version = microschemaRoot.createVersion();

		microschema.setVersion("1.0");
		container.setLatestVersion(version);
		version.setName(microschema.getName());
		version.setSchema(microschema);
		version.setSchemaContainer(container);
		container.setCreated(user);
		container.setName(microschema.getName());
		container.generateBucketId();
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
				container = findByName(project, microschemaName);
			} else {
				container = findByName(microschemaName);
			}
		} else {
			if (project != null) {
				container = findByUuid(project, microschemaUuid);
			} else {
				container = findByUuid(microschemaUuid);
			}
		}
		// Return the specified version or fallback to latest version.
		if (container == null) {
			throw error(BAD_REQUEST, "error_microschema_reference_not_found",
				isEmpty(microschemaName) ? "-" : microschemaName, isEmpty(microschemaUuid) ? "-" : microschemaUuid,
				version == null ? "-" : version.toString());
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
			throw error(BAD_REQUEST, "error_microschema_reference_not_found",
				isEmpty(microschemaName) ? "-" : microschemaName, isEmpty(microschemaUuid) ? "-" : microschemaUuid,
				version == null ? "-" : version.toString());
		}
		return foundVersion;
	}

	@Override
	public HibMicroschema findByName(HibProject project, String microschemaName) {
		return toGraph(project).getMicroschemaContainerRoot().findByName(microschemaName);
	}

	@Override
	public Microschema findByName(String name) {
		MicroschemaRoot microschemaRoot = boot.get().meshRoot().getMicroschemaContainerRoot();
		return microschemaRoot.findByName(name);
	}

	@Override
	public HibMicroschema findByUuid(HibProject project, String uuid) {
		return toGraph(project).getMicroschemaContainerRoot().findByUuid(uuid);
	}

	@Override
	public Page<? extends HibMicroschema> findAll(HibProject project, InternalActionContext ac,
		PagingParameters pagingInfo) {
		Project graphProject = toGraph(project);
		return graphProject.getMicroschemaContainerRoot().findAll(ac, pagingInfo);
	}

	@Override
	public Page<? extends Microschema> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		MicroschemaRoot microschemaRoot = boot.get().meshRoot().getMicroschemaContainerRoot();
		return microschemaRoot.findAll(ac, pagingInfo);
	}

	@Override
	public Page<? extends HibMicroschema> findAll(InternalActionContext ac, PagingParameters pagingInfo,
		Predicate<HibMicroschema> extraFilter) {
		MicroschemaRoot microschemaRoot = boot.get().meshRoot().getMicroschemaContainerRoot();
		return microschemaRoot.findAll(ac, pagingInfo, microschema -> extraFilter.test(microschema));
	}

	@Override
	public HibMicroschema loadObjectByUuid(InternalActionContext ac, String schemaUuid, InternalPermission perm) {
		// TODO check for project in context?
		MicroschemaRoot microschemaRoot = boot.get().meshRoot().getMicroschemaContainerRoot();
		return microschemaRoot.loadObjectByUuid(ac, schemaUuid, perm);
	}

	@Override
	public HibMicroschema loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm,
		boolean errorIfNotFound) {
		// TODO check for project in context?
		MicroschemaRoot microschemaRoot = boot.get().meshRoot().getMicroschemaContainerRoot();
		return microschemaRoot.loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	@Override
	public void delete(HibMicroschema microschema, BulkActionContext bac) {
		Microschema graphMicroschema = toGraph(microschema);
		for (HibMicroschemaVersion version : graphMicroschema.findAll()) {
			MicroschemaVersion graphVersion = toGraph(version);
			if (graphVersion.findMicronodes().hasNext()) {
				throw error(BAD_REQUEST, "microschema_delete_still_in_use", microschema.getUuid());
			}
			graphVersion.delete(bac);
		}
		graphMicroschema.delete(bac);
	}

	@Override
	public boolean isLinkedToProject(HibMicroschema microschema, HibProject project) {
		Project graphProject = toGraph(project);
		Microschema graphMicroschema = toGraph(microschema);
		MicroschemaRoot root = graphProject.getMicroschemaContainerRoot();
		return root.contains(graphMicroschema);
	}

	@Override
	public HibMicroschemaVersion applyChanges(HibMicroschemaVersion version, InternalActionContext ac,
		EventQueueBatch batch) {
		MicroschemaVersion graphMicroschemaVersion = toGraph(version);
		return graphMicroschemaVersion.applyChanges(ac, batch);
	}

	@Override
	public HibMicroschemaVersion applyChanges(HibMicroschemaVersion version, InternalActionContext ac,
		SchemaChangesListModel model, EventQueueBatch batch) {
		MicroschemaVersion graphMicroschemaVersion = toGraph(version);
		return graphMicroschemaVersion.applyChanges(ac, model, batch);
	}

	@Override
	public SchemaChangesListModel diff(HibMicroschemaVersion version, InternalActionContext ac,
		MicroschemaModel requestModel) {
		MicroschemaVersion graphVersion = toGraph(version);
		return graphVersion.diff(ac, comparator, requestModel);
	}

	@Override
	public Iterable<? extends HibMicroschemaVersion> findAllVersions(HibMicroschema microschema) {
		return toGraph(microschema).findAll();
	}

	@Override
	public Map<HibBranch, HibMicroschemaVersion> findReferencedBranches(HibMicroschema microschema) {
		Map<?, ?> map = toGraph(microschema).findReferencedBranches();
		return (Map<HibBranch, HibMicroschemaVersion>) map;
	}

	@Override
	public Result<? extends NodeGraphFieldContainer> findDraftFieldContainers(HibMicroschemaVersion version,
		String branchUuid) {
		return toGraph(version).getDraftFieldContainers(branchUuid);
	}

	@Override
	public MicroschemaResponse transformToRestSync(HibMicroschema microschema, InternalActionContext ac, int level,
		String... languageTags) {
		return toGraph(microschema).transformToRestSync(ac, level, languageTags);
	}

	@Override
	public void unlink(HibMicroschema microschema, HibProject project, EventQueueBatch batch) {
		toGraph(project).getMicroschemaContainerRoot().removeMicroschema(toGraph(microschema), batch);
	}

	@Override
	public String getETag(HibMicroschema schema, InternalActionContext ac) {
		return toGraph(schema).getETag(ac);
	}

	@Override
	public HibMicroschema findByUuid(String uuid) {
		MicroschemaRoot microschemaRoot = boot.get().meshRoot().getMicroschemaContainerRoot();
		return microschemaRoot.findByUuid(uuid);
	}

	@Override
	public long count() {
		return boot.get().meshRoot().getMicroschemaContainerRoot().globalCount();
	}

	@Override
	public void addMicroschema(HibMicroschema schema, HibUser user, EventQueueBatch batch) {
		boot.get().meshRoot().getMicroschemaContainerRoot().addMicroschema(user, schema, batch);
	}

	@Override
	public Result<? extends HibMicroschema> findAll(HibProject project) {
		return toGraph(project).getMicroschemaContainerRoot().findAll();
	}

	@Override
	public Result<HibMicroschemaVersion> findActiveMicroschemaVersions(HibBranch branch) {
		return toGraph(branch).findActiveMicroschemaVersions();
	}

	@Override
	public boolean contains(HibProject project, HibMicroschema microschema) {
		return toGraph(project).getMicroschemaContainerRoot().contains(microschema);
	}

	@Override
	public void addMicroschema(HibProject project, HibUser user, HibMicroschema microschema,
		EventQueueBatch batch) {
		Project graphProject = toGraph(project);
		graphProject.getMicroschemaContainerRoot().addMicroschema(user, microschema, batch);
	}

	@Override
	public void removeMicroschema(HibProject project, HibMicroschema microschema, EventQueueBatch batch) {
		toGraph(project).getMicroschemaContainerRoot().removeMicroschema(microschema, batch);
	}

	@Override
	public Result<? extends HibMicroschema> findAll() {
		return boot.get().meshRoot().getMicroschemaContainerRoot().findAll();
	}

	@Override
	public boolean update(HibMicroschema element, InternalActionContext ac, EventQueueBatch batch) {
		return boot.get().meshRoot().getMicroschemaContainerRoot().update(toGraph(element), ac, batch);
	}

	@Override
	public Stream<? extends HibMicroschema> findAllStream(HibProject root, InternalActionContext ac,
			InternalPermission permission) {
		return toGraph(root).getMicroschemaContainerRoot().findAllStream(ac, permission);
	}

	@Override
	public Result<? extends HibMicroschema> findAllDynamic(HibProject root) {
		return toGraph(root).getMicroschemaContainerRoot().findAllDynamic();
	}

	@Override
	public Page<? extends HibMicroschema> findAll(HibProject root, InternalActionContext ac,
			PagingParameters pagingInfo, Predicate<HibMicroschema> extraFilter) {
		return toGraph(root).getMicroschemaContainerRoot().findAll(ac, pagingInfo, e -> extraFilter.test(e));
	}

	@Override
	public Page<? extends HibMicroschema> findAllNoPerm(HibProject root, InternalActionContext ac,
			PagingParameters pagingInfo) {
		return toGraph(root).getMicroschemaContainerRoot().findAllNoPerm(ac, pagingInfo);
	}

	@Override
	public HibMicroschema findByName(HibProject root, InternalActionContext ac, String name, InternalPermission perm) {
		return toGraph(root).getMicroschemaContainerRoot().findByName(ac, name, perm);
	}

	@Override
	public HibMicroschema checkPerms(HibProject root, HibMicroschema element, String uuid, InternalActionContext ac,
			InternalPermission perm, boolean errorIfNotFound) {
		return toGraph(root).getMicroschemaContainerRoot().checkPerms(toGraph(element), uuid, ac, perm, errorIfNotFound);
	}

	@Override
	public HibMicroschema create(HibProject root, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return toGraph(root).getMicroschemaContainerRoot().create(ac, batch, uuid);
	}

	@Override
	public void addItem(HibProject root, HibMicroschema item) {
		toGraph(root).getMicroschemaContainerRoot().addItem(toGraph(item));
	}

	@Override
	public void removeItem(HibProject root, HibMicroschema item) {
		toGraph(root).getMicroschemaContainerRoot().removeItem(toGraph(item));
	}

	@Override
	public String getRootLabel(HibProject root) {
		return toGraph(root).getMicroschemaContainerRoot().getRootLabel();
	}

	@Override
	public Class<? extends HibMicroschema> getPersistenceClass(HibProject root) {
		return toGraph(root).getMicroschemaContainerRoot().getPersistanceClass();
	}

	@Override
	public long globalCount(HibProject root) {
		return toGraph(root).getMicroschemaContainerRoot().globalCount();
	}

	@Override
	public PermissionInfo getRolePermissions(HibProject root, HibBaseElement element, InternalActionContext ac,
			String roleUuid) {
		return toGraph(root).getMicroschemaContainerRoot().getRolePermissions(element, ac, roleUuid);
	}

	@Override
	public Result<? extends HibRole> getRolesWithPerm(HibProject root, HibBaseElement element,
			InternalPermission perm) {
		return toGraph(root).getMicroschemaContainerRoot().getRolesWithPerm(element, perm);
	}

	@Override
	public void delete(HibProject root, HibMicroschema element, BulkActionContext bac) {
		toGraph(root).getMicroschemaContainerRoot().delete(toGraph(element), bac);
	}

	@Override
	public boolean update(HibProject root, HibMicroschema element, InternalActionContext ac, EventQueueBatch batch) {
		return toGraph(root).getMicroschemaContainerRoot().update(toGraph(element), ac, batch);
	}
}
