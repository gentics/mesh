package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Bucket;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.AbstractCoreDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionPropertiesImpl;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.root.SchemaRoot;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.schema.handler.SchemaComparator;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.event.project.ProjectSchemaEventModel;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;

import dagger.Lazy;

/**
 * @see SchemaDaoWrapper
 */
public class SchemaDaoWrapperImpl extends AbstractCoreDaoWrapper<SchemaResponse, HibSchema, Schema> implements SchemaDaoWrapper {

	private final SchemaComparator comparator;

	@Inject
	public SchemaDaoWrapperImpl(Lazy<OrientDBBootstrapInitializer> boot, Lazy<PermissionPropertiesImpl> permissions, SchemaComparator comparator) {
		super(boot, permissions);
		this.comparator = comparator;
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
	public Page<? extends HibSchema> findAll(InternalActionContext ac, HibProject project, PagingParameters pagingInfo) {
		return toGraph(project).getSchemaContainerRoot().findAll(ac, pagingInfo);
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
	public HibSchema loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm) {
		// TODO check for project in context?
		return getRoot().loadObjectByUuid(ac, uuid, perm);
	}

	@Override
	public HibSchema loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		// TODO check for project in context?
		return getRoot().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	@Override
	public HibSchema loadObjectByUuid(HibProject project, InternalActionContext ac, String uuid, InternalPermission perm) {
		return toGraph(project).getSchemaContainerRoot().loadObjectByUuid(ac, uuid, perm);
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
		return getRoots(schema).stream()
			.map(SchemaRoot::getProject)
			.filter(Objects::nonNull)
			.map(project -> project.onSchemaAssignEvent(schema, assigned));
	}

	@Override
	public void addSchema(HibSchema schemaContainer, HibProject project, HibUser user, EventQueueBatch batch) {
		Schema graphSchemaContainer = toGraph(schemaContainer);
		SchemaRoot schemaRoot;
		if (project != null) {
			Project graphProject = toGraph(project);
			schemaRoot = graphProject.getSchemaContainerRoot();			
		} else {
			schemaRoot = boot.get().meshRoot().getSchemaContainerRoot();
		}
		schemaRoot.addSchemaContainer(user, graphSchemaContainer, batch);
	}

	@Override
	public HibSchemaVersion applyChanges(HibSchemaVersion version, InternalActionContext ac, SchemaChangesListModel model, EventQueueBatch batch) {
		SchemaVersion graphSchemaVersion = toGraph(version);
		return graphSchemaVersion.applyChanges(ac, model, batch);
	}

	@Override
	public HibSchemaVersion applyChanges(HibSchemaVersion version, InternalActionContext ac, EventQueueBatch batch) {
		return toGraph(version).applyChanges(ac, batch);
	}

	@Override
	public HibSchemaVersion findVersionByRev(HibSchema schema, String version) {
		return toGraph(schema).findVersionByRev(version);
	}

	@Override
	public boolean isLinkedToProject(HibSchema schema, HibProject project) {
		Project graphProject = toGraph(project);
		Schema graphSchema = toGraph(schema);
		SchemaRoot root = graphProject.getSchemaContainerRoot();
		return root.contains(graphSchema);
	}

	@Override
	public SchemaResponse transformToRestSync(HibSchema schema, InternalActionContext ac, int level, String... languageTags) {
		Schema graphSchema = toGraph(schema);
		return graphSchema.transformToRestSync(ac, level, languageTags);
	}

	@Override
	public void removeSchema(HibSchema schema, HibProject project, EventQueueBatch batch) {
		toGraph(project).getSchemaContainerRoot().removeSchemaContainer(toGraph(schema), batch);
	}

	@Override
	public SchemaChangesListModel diff(HibSchemaVersion version, InternalActionContext ac, SchemaModel requestModel) {
		SchemaVersion graphVersion = toGraph(version);
		return graphVersion.diff(ac, comparator, requestModel);
	}

	@Override
	public HibSchemaVersion findVersionByUuid(HibSchema schema, String versionUuid) {
		Schema graphSchema = toGraph(schema);
		return graphSchema.findVersionByUuid(versionUuid);
	}

	@Override
	public Map<HibBranch, HibSchemaVersion> findReferencedBranches(HibSchema schema) {
		return toGraph(schema).findReferencedBranches();
	}

	@Override
	public Iterator<? extends HibNodeFieldContainer> findDraftFieldContainers(HibSchemaVersion version, String branchUuid) {
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
	public void addSchema(HibSchema schema) {
		getRoot().addItem(toGraph(schema));
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
	public boolean contains(HibProject project, HibSchema schema) {
		return toGraph(project).getSchemaContainerRoot().contains(toGraph(schema));
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
	public Result<? extends HibSchema> findAllDynamic(HibProject root) {
		return toGraph(root).getSchemaContainerRoot().findAllDynamic();
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
	public HibSchema findByName(HibProject root, InternalActionContext ac, String name, InternalPermission perm) {
		return toGraph(root).getSchemaContainerRoot().findByName(ac, name, perm);
	}

	@Override
	public HibSchema checkPerms(HibProject root, HibSchema element, String uuid, InternalActionContext ac,
			InternalPermission perm, boolean errorIfNotFound) {
		return toGraph(root).getSchemaContainerRoot().checkPerms(toGraph(element), uuid, ac, perm, errorIfNotFound);
	}

	@Override
	public HibSchema create(HibProject root, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return toGraph(root).getSchemaContainerRoot().create(ac, batch, uuid);
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
	public PermissionInfo getRolePermissions(HibProject root, HibBaseElement element, InternalActionContext ac,
			String roleUuid) {
		return toGraph(root).getSchemaContainerRoot().getRolePermissions(element, ac, roleUuid);
	}

	@Override
	public Result<? extends HibRole> getRolesWithPerm(HibProject root, HibBaseElement element,
			InternalPermission perm) {
		return toGraph(root).getSchemaContainerRoot().getRolesWithPerm(element, perm);
	}

	@Override
	public void delete(HibProject root, HibSchema element, BulkActionContext bac) {
		toGraph(root).getSchemaContainerRoot().delete(toGraph(element), bac);
	}

	@Override
	public boolean update(HibProject root, HibSchema element, InternalActionContext ac, EventQueueBatch batch) {
		return toGraph(root).getSchemaContainerRoot().update(toGraph(element), ac, batch);
	}

	@Override
	protected RootVertex<Schema> getRoot() {
		return boot.get().meshRoot().getSchemaContainerRoot();
	}

	@Override
	public Schema persist(String uuid) {
		Schema vertex = boot.get().meshRoot().getSchemaContainerRoot().create();
		if (uuid != null) {
			vertex.setUuid(uuid);
		}
		vertex.setLatestVersion(boot.get().meshRoot().getSchemaContainerRoot().createVersion());
		return vertex;
	}

	@Override
	public void unpersist(Schema element) {
		element.remove();
	}

	@Override
	public void deleteVersion(HibSchemaVersion v, BulkActionContext bac) {
		toGraph(v).delete(bac);		
	}
}
