package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.AbstractContainerDaoWrapper;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionPropertiesImpl;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.MicroschemaRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

import dagger.Lazy;

/**
 * @see MicroschemaDaoWrapper
 */
public class MicroschemaDaoWrapperImpl 
			extends AbstractContainerDaoWrapper<MicroschemaResponse, MicroschemaVersionModel, MicroschemaReference, HibMicroschema, HibMicroschemaVersion, MicroschemaModel, Microschema> 
			implements MicroschemaDaoWrapper {

	@Inject
	public MicroschemaDaoWrapperImpl(Lazy<OrientDBBootstrapInitializer> boot, Lazy<PermissionPropertiesImpl> permissions) {
		super(boot, permissions);
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
	public boolean isLinkedToProject(HibMicroschema microschema, HibProject project) {
		Project graphProject = toGraph(project);
		Microschema graphMicroschema = toGraph(microschema);
		MicroschemaRoot root = graphProject.getMicroschemaContainerRoot();
		return root.contains(graphMicroschema);
	}

	@Override
	public Iterable<? extends HibMicroschemaVersion> findAllVersions(HibMicroschema microschema) {
		return toGraph(microschema).findAll();
	}

	@Override
	public Map<HibBranch, HibMicroschemaVersion> findReferencedBranches(HibMicroschema microschema) {
		return toGraph(microschema).findReferencedBranches();
	}

	@Override
	public Result<? extends HibNodeFieldContainer> findDraftFieldContainers(HibMicroschemaVersion version,
		String branchUuid) {
		return toGraph(version).getDraftFieldContainers(branchUuid);
	}

	@Override
	public MicroschemaResponse transformToRestSync(HibMicroschema microschema, InternalActionContext ac, int level,
		String... languageTags) {
		return toGraph(microschema).transformToRestSync(ac, level, languageTags);
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
	public Result<? extends HibMicroschema> findAll(HibProject project) {
		return toGraph(project).getMicroschemaContainerRoot().findAll();
	}

	@Override
	public Result<HibMicroschemaVersion> findActiveSchemaVersions(HibBranch branch) {
		return toGraph(branch).findActiveMicroschemaVersions();
	}

	@Override
	public boolean contains(HibProject project, HibMicroschema microschema) {
		return toGraph(project).getMicroschemaContainerRoot().contains(microschema);
	}

	@Override
	public Result<? extends HibMicroschema> findAll() {
		return boot.get().meshRoot().getMicroschemaContainerRoot().findAll();
	}

	@Override
	public Stream<? extends HibMicroschema> findAllStream(HibProject root, InternalActionContext ac,
			InternalPermission permission) {
		return toGraph(root).getMicroschemaContainerRoot().findAllStream(ac, permission);
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
	protected RootVertex<Microschema> getRoot() {
		return boot.get().meshRoot().getMicroschemaContainerRoot();
	}

	@Override
	public HibMicroschema createPersisted(String uuid) {
		HibMicroschema vertex = super.createPersisted(uuid);
		vertex.setLatestVersion(boot.get().meshRoot().getMicroschemaContainerRoot().createVersion());
		return vertex;
	}

	@Override
	public void deletePersisted(HibMicroschema element) {
		toGraph(element).remove();
	}

	@Override
	public HibMicroschemaVersion findVersionByRev(HibMicroschema hibMicroschema, String version) {
		return toGraph(hibMicroschema).findVersionByRev(version);
	}

	@Override
	public HibMicroschemaVersion findVersionByUuid(HibMicroschema schema, String versionUuid) {
		return toGraph(schema).findVersionByUuid(versionUuid);
	}

	@Override
	public boolean update(HibMicroschema element, InternalActionContext ac, EventQueueBatch batch) {
		return boot.get().meshRoot().getMicroschemaContainerRoot().update(toGraph(element), ac, batch);
	}

	@Override
	public Result<HibProject> findLinkedProjects(HibMicroschema schema) {
		return new TraversalResult<>(boot.get().meshRoot().getProjectRoot()
				.findAll().stream().filter(project -> project.getMicroschemaContainerRoot().contains(schema)));
	}
}
