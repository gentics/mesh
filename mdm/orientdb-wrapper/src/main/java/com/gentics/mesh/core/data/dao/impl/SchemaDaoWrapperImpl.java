package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Bucket;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.AbstractContainerDaoWrapper;
import com.gentics.mesh.core.data.dao.ProjectDao;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.ContainerRootVertex;
import com.gentics.mesh.core.data.root.SchemaRoot;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.event.project.ProjectSchemaEventModel;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.parameter.PagingParameters;

import dagger.Lazy;

/**
 * @see SchemaDaoWrapper
 */
public class SchemaDaoWrapperImpl 
			extends AbstractContainerDaoWrapper<
				SchemaResponse, SchemaVersionModel, SchemaReference, 
				HibSchema, HibSchemaVersion, SchemaModel, 
				Schema, SchemaVersion
			> 
			implements SchemaDaoWrapper {

	@Inject
	public SchemaDaoWrapperImpl(Lazy<OrientDBBootstrapInitializer> boot) {
		super(boot);
	}

	@Override
	public HibSchema findByName(String name) {
		return getRoot().findByName(name);
	}

	@Override
	public HibSchema findByUuid(String uuid) {
		return getRoot().findByUuid(uuid);
	}

	@Override
	public Page<? extends HibSchema> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return getRoot().findAll(ac, pagingInfo);
	}

	@Override
	public Page<? extends HibSchema> findAll(HibProject project, InternalActionContext ac, PagingParameters pagingInfo,
		Predicate<HibSchema> extraFilter) {
		Project graphProject = toGraph(project);
		return graphProject.getSchemaContainerRoot().findAll(ac, pagingInfo, schema -> {
			return extraFilter.test(schema);
		});
	}

	@Override
	public HibSchema findByUuid(HibProject project, String schemaUuid) {
		return toGraph(project).getSchemaContainerRoot().findByUuid(schemaUuid);

	}

	@Override
	public HibSchema findByName(HibProject project, String schemaName) {
		return toGraph(project).getSchemaContainerRoot().findByName(schemaName);
	}

	@Override
	public Result<? extends SchemaRoot> getRoots(HibSchema schema) {
		return boot.get().meshRoot().getSchemaContainerRoot().getRoots(toGraph(schema));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterable<HibSchemaVersion> findAllVersions(HibSchema schema) {
		Schema graphSchema = toGraph(schema);
		return (Iterable<HibSchemaVersion>) (Iterable<?>) boot.get().meshRoot().getSchemaContainerRoot().findAllVersions(graphSchema);
	}

	@Override
	public Result<? extends Node> getNodes(HibSchema schema) {
		Schema graphSchema = toGraph(schema);
		return boot.get().meshRoot().getSchemaContainerRoot().getNodes(graphSchema);
	}

	@Override
	public Stream<ProjectSchemaEventModel> assignEvents(HibSchema schema, Assignment assigned) {
		ProjectDao projectDao = Tx.get().projectDao();
		return getRoots(schema).stream()
			.map(SchemaRoot::getProject)
			.filter(Objects::nonNull)
			.map(project -> projectDao.onSchemaAssignEvent(project, schema, assigned));
	}

	@Override
	public HibSchemaVersion findVersionByRev(HibSchema schema, String version) {
		return toGraph(schema).findVersionByRev(version);
	}

	@Override
	public SchemaResponse transformToRestSync(HibSchema schema, InternalActionContext ac, int level, String... languageTags) {
		Schema graphSchema = toGraph(schema);
		return graphSchema.transformToRestSync(ac, level, languageTags);
	}

	@Override
	public HibSchemaVersion findVersionByUuid(HibSchema schema, String versionUuid) {
		Schema graphSchema = toGraph(schema);
		return graphSchema.findVersionByUuid(versionUuid);
	}

	@Override
	public Result<? extends HibNodeFieldContainer> findDraftFieldContainers(HibSchemaVersion version, String branchUuid) {
		return toGraph(version).getDraftFieldContainers(branchUuid);
	}

	@Override
	public Result<HibProject> findLinkedProjects(HibSchema schema) {
		return new TraversalResult<>(getRoots(schema).stream().map(root -> root.getProject()));
	}

	@Override
	public Result<? extends HibNode> findNodes(HibSchemaVersion version, String branchUuid, HibUser user, ContainerType type) {
		return toGraph(version).getNodes(branchUuid, user, type);
	}

	@Override
	public Result<? extends HibSchema> findAll(HibProject project) {
		return toGraph(project).getSchemaContainerRoot().findAll();
	}

	@Override
	public Result<HibSchemaVersion> findActiveSchemaVersions(HibBranch branch) {
		Branch graphBranch = toGraph(branch);
		return graphBranch.findActiveSchemaVersions();
	}

	@Override
	public Stream<? extends HibNodeFieldContainer> getFieldContainers(HibSchemaVersion version, String branchUuid) {
		SchemaVersion graphVersion = toGraph(version);
		return graphVersion.getFieldContainers(branchUuid);
	}

	@Override
	public Stream<? extends HibNodeFieldContainer> getFieldContainers(HibSchemaVersion version, String branchUuid, Bucket bucket) {
		SchemaVersion graphVersion = toGraph(version);
		return graphVersion.getFieldContainers(branchUuid, bucket);
	}

	@Override
	public Result<? extends HibSchema> findAll() {
		return boot.get().meshRoot().getSchemaContainerRoot().findAll();
	}

	@Override
	public Page<? extends HibSchema> findAll(InternalActionContext ac, PagingParameters pagingInfo,
			Predicate<HibSchema> extraFilter) {
		return boot.get().meshRoot().getSchemaContainerRoot().findAll(ac, pagingInfo);
	}

	@Override
	public Stream<? extends HibSchema> findAllStream(HibProject root, InternalActionContext ac,
			InternalPermission permission) {
		return toGraph(root).getSchemaContainerRoot().findAllStream(ac, permission);
	}

	@Override
	public Page<? extends HibSchema> findAll(HibProject root, InternalActionContext ac, PagingParameters pagingInfo) {
		return toGraph(root).getSchemaContainerRoot().findAll(ac, pagingInfo);
	}

	@Override
	public Page<? extends HibSchema> findAllNoPerm(HibProject root, InternalActionContext ac,
			PagingParameters pagingInfo) {
		return toGraph(root).getSchemaContainerRoot().findAllNoPerm(ac, pagingInfo);
	}

	@Override
	public void addItem(HibProject root, HibSchema item) {
		toGraph(root).getSchemaContainerRoot().addItem(toGraph(item));
	}

	@Override
	public void removeItem(HibProject root, HibSchema item) {
		toGraph(root).getSchemaContainerRoot().removeItem(toGraph(item));
	}

	@Override
	public String getRootLabel(HibProject root) {
		return toGraph(root).getSchemaContainerRoot().getRootLabel();
	}

	@Override
	public Class<? extends HibSchema> getPersistenceClass(HibProject root) {
		return toGraph(root).getSchemaContainerRoot().getPersistanceClass();
	}

	@Override
	public long globalCount(HibProject root) {
		return toGraph(root).getSchemaContainerRoot().globalCount();
	}

	@Override
	protected ContainerRootVertex<SchemaResponse, SchemaVersionModel, Schema, SchemaVersion> getRoot() {
		return boot.get().meshRoot().getSchemaContainerRoot();
	}
}
