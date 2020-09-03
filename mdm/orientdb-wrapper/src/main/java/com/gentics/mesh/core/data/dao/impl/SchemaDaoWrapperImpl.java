package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.event.Assignment.UNASSIGNED;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Provider;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionProperties;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.SchemaRoot;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.schema.handler.SchemaComparator;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.event.project.ProjectSchemaEventModel;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.search.index.node.NodeIndexHandler;

import dagger.Lazy;
import io.vertx.core.Vertx;

public class SchemaDaoWrapperImpl extends AbstractDaoWrapper<HibSchema> implements SchemaDaoWrapper {

	private final Lazy<Vertx> vertx;
	private final Lazy<NodeIndexHandler> nodeIndexHandler;
	private final Provider<EventQueueBatch> batchProvider;
	private final SchemaComparator comparator;

	@Inject
	public SchemaDaoWrapperImpl(Lazy<BootstrapInitializer> boot, Lazy<PermissionProperties> permissions, Lazy<Vertx> vertx,
		Lazy<NodeIndexHandler> nodeIndexHandler, Provider<EventQueueBatch> batchProvider, SchemaComparator comparator) {
		super(boot, permissions);
		this.vertx = vertx;
		this.nodeIndexHandler = nodeIndexHandler;
		this.batchProvider = batchProvider;
		this.comparator = comparator;
	}

	@Override
	public HibSchema findByName(String name) {
		SchemaRoot schemaRoot = boot.get().schemaContainerRoot();
		return schemaRoot.findByName(name);
	}

	@Override
	public HibSchema findByUuid(String uuid) {
		SchemaRoot schemaRoot = boot.get().schemaContainerRoot();
		return schemaRoot.findByUuid(uuid);
	}

	@Override
	public HibSchema findByUuidGlobal(String uuid) {
		return findByUuid(uuid);
	}

	@Override
	public Result<HibSchema> findAll() {
		SchemaRoot schemaRoot = boot.get().schemaContainerRoot();
		// TODO findAll should not return wildcard generics
		return (Result<HibSchema>) (Result<?>) schemaRoot.findAll();
	}

	@Override
	public long computeCount() {
		return boot.get().schemaContainerRoot().computeCount();
	}

	@Override
	public long computeGlobalCount() {
		return computeCount();
	}

	@Override
	public TransformablePage<? extends HibSchema> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		SchemaRoot schemaRoot = boot.get().schemaContainerRoot();
		return schemaRoot.findAll(ac, pagingInfo);
	}

	@Override
	public TransformablePage<? extends HibSchema> findAll(InternalActionContext ac, HibProject project, PagingParameters pagingInfo) {
		return project.getSchemaContainerRoot().findAll(ac, pagingInfo);
	}

	@Override
	public HibSchema create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		MeshAuthUser requestUser = ac.getUser();
		UserDaoWrapper userDao = Tx.get().data().userDao();
		SchemaRoot schemaRoot = boot.get().schemaContainerRoot();

		SchemaVersionModel requestModel = JsonUtil.readValue(ac.getBodyAsString(), SchemaModelImpl.class);
		requestModel.validate();

		if (!userDao.hasPermission(requestUser, schemaRoot, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", schemaRoot.getUuid(), CREATE_PERM.getRestPerm().getName());
		}
		HibSchema container = create(requestModel, requestUser, uuid, ac.getSchemaUpdateParameters().isStrictValidation());
		userDao.inheritRolePermissions(requestUser, schemaRoot, container);
		batch.add(container.onCreated());
		return container;

	}

	@Override
	public HibSchemaVersion fromReference(HibProject project, SchemaReference reference) {
		if (reference == null) {
			throw error(INTERNAL_SERVER_ERROR, "Missing schema reference");
		}
		String schemaName = reference.getName();
		String schemaUuid = reference.getUuid();
		String schemaVersion = reference.getVersion();

		// Prefer the name over the uuid
		HibSchema schemaContainer = null;
		if (!isEmpty(schemaName)) {
			if (project != null) {
				schemaContainer = findByName(project, schemaName);
			} else {
				schemaContainer = findByName(schemaName);
			}
		} else {
			if (project != null) {
				schemaContainer = findByUuid(project, schemaUuid);
			} else {
				schemaContainer = findByUuid(schemaUuid);
			}
		}

		// Check whether a container was actually found
		if (schemaContainer == null) {
			throw error(BAD_REQUEST, "error_schema_reference_not_found", isEmpty(schemaName) ? "-" : schemaName, isEmpty(schemaUuid) ? "-"
				: schemaUuid, schemaVersion == null ? "-" : schemaVersion.toString());
		}
		if (schemaVersion == null) {
			return schemaContainer.getLatestVersion();
		} else {
			HibSchemaVersion foundVersion = toGraph(schemaContainer).findVersionByRev(schemaVersion);
			if (foundVersion == null) {
				throw error(BAD_REQUEST, "error_schema_reference_not_found", isEmpty(schemaName) ? "-" : schemaName, isEmpty(schemaUuid) ? "-"
					: schemaUuid, schemaVersion == null ? "-" : schemaVersion.toString());
			} else {
				return foundVersion;
			}
		}

	}

	@Override
	public HibSchema findByUuid(HibProject project, String schemaUuid) {
		return project.getSchemaContainerRoot().findByUuid(schemaUuid);

	}

	@Override
	public HibSchema findByName(HibProject project, String schemaName) {
		return project.getSchemaContainerRoot().findByName(schemaName);
	}

	@Override
	public HibSchemaVersion fromReference(SchemaReference reference) {
		return fromReference(null, reference);
	}

	@Override
	public HibSchema create(SchemaVersionModel schema, HibUser creator, String uuid) {
		return create(schema, creator, uuid, false);
	}

	@Override
	public HibSchema create(SchemaVersionModel schema, HibUser creator, String uuid, boolean validate) {
		SchemaRoot schemaRoot = boot.get().schemaContainerRoot();
		MicroschemaDaoWrapper microschemaDao = Tx.get().data().microschemaDao();

		// TODO FIXME - We need to skip the validation check if the instance is creating a clustered instance because vert.x is not yet ready.
		// https://github.com/gentics/mesh/issues/210
		if (validate && vertx.get() != null) {
			validateSchema(nodeIndexHandler.get(), schema);
		}

		String name = schema.getName();
		HibSchema conflictingSchema = findByName(name);
		if (conflictingSchema != null) {
			throw conflict(conflictingSchema.getUuid(), name, "schema_conflicting_name", name);
		}

		HibMicroschema conflictingMicroschema = microschemaDao.findByName(name);
		if (conflictingMicroschema != null) {
			throw conflict(conflictingMicroschema.getUuid(), name, "microschema_conflicting_name", name);
		}

		Schema container = schemaRoot.create();
		if (uuid != null) {
			container.setUuid(uuid);
		}
		SchemaVersion version = schemaRoot.createVersion();
		container.setLatestVersion(version);

		// set the initial version
		schema.setVersion("1.0");
		version.setSchema(schema);
		version.setName(schema.getName());
		version.setSchemaContainer(container);
		container.setCreated(creator);
		container.setName(schema.getName());

		schemaRoot.addSchemaContainer(creator, container, null);
		return container;
	}

	public static void validateSchema(NodeIndexHandler indexHandler, SchemaVersionModel schema) {
		// TODO Maybe set the timeout to the configured search.timeout? But the default of 60 seconds is really long.
		Throwable error = indexHandler.validate(schema).blockingGet(10, TimeUnit.SECONDS);

		if (error != null) {
			if (error instanceof GenericRestException) {
				throw (GenericRestException) error;
			} else {
				throw new RuntimeException(error);
			}
		}
	}

	@Override
	public HibSchema loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm) {
		// TODO check for project in context?
		SchemaRoot schemaRoot = boot.get().schemaContainerRoot();
		return schemaRoot.loadObjectByUuid(ac, uuid, perm);
	}

	@Override
	public HibSchema loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		// TODO check for project in context?
		SchemaRoot schemaRoot = boot.get().schemaContainerRoot();
		return schemaRoot.loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	@Override
	public Result<? extends SchemaRoot> getRoots(HibSchema schema) {
		return boot.get().schemaContainerRoot().getRoots(toGraph(schema));
	}

	@Override
	public Iterable<SchemaVersion> findAllVersions(HibSchema schema) {
		Schema graphSchema = toGraph(schema);
		return (Iterable<SchemaVersion>) (Iterable<?>) boot.get().schemaContainerRoot().findAllVersions(graphSchema);
	}

	@Override
	public Result<? extends Node> getNodes(HibSchema schema) {
		Schema graphSchema = toGraph(schema);
		return boot.get().schemaContainerRoot().getNodes(graphSchema);
	}

	@Override
	public void delete(HibSchema schema, BulkActionContext bac) {
		Schema graphSchema = toGraph(schema);

		// Check whether the schema is currently being referenced by nodes.
		Iterator<? extends Node> it = getNodes(schema).iterator();
		if (!it.hasNext()) {

			unassignEvents(graphSchema).forEach(bac::add);
			bac.add(schema.onDeleted());

			for (SchemaVersion v : findAllVersions(schema)) {
				v.delete(bac);
			}
			graphSchema.remove();
		} else {
			throw error(BAD_REQUEST, "schema_delete_still_in_use", schema.getUuid());
		}
	}

	/**
	 * Returns events for unassignment on deletion.
	 * 
	 * @return
	 */
	private Stream<ProjectSchemaEventModel> unassignEvents(Schema schema) {
		return getRoots(schema).stream()
			.map(SchemaRoot::getProject)
			.filter(Objects::nonNull)
			.map(project -> project.onSchemaAssignEvent(schema, UNASSIGNED));
	}

	@Override
	public void addSchema(HibSchema schemaContainer, HibProject project, HibUser user, EventQueueBatch batch) {
		Project graphProject = toGraph(project);
		Schema graphSchemaContainer = toGraph(schemaContainer);
		graphProject.getSchemaContainerRoot().addSchemaContainer(user, graphSchemaContainer, batch);
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
	public SchemaResponse transformToRestSync(HibSchema schema, InternalActionContext ac, int level) {
		Schema graphSchema = toGraph(schema);
		return graphSchema.transformToRestSync(ac, level);
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
		Map<?, ?> map = toGraph(schema).findReferencedBranches();
		return (Map<HibBranch, HibSchemaVersion>) map;
	}

	@Override
	public Iterator<? extends NodeGraphFieldContainer> findDraftFieldContainers(HibSchemaVersion version, String branchUuid) {
		return toGraph(version).getDraftFieldContainers(branchUuid);
	}

	@Override
	public Result<HibProject> findLinkedProjects(HibSchema schema) {
		return new TraversalResult<>(getRoots(schema).stream().map(root -> root.getProject()));
	}

	@Override
	public String getETag(HibSchema schema, InternalActionContext ac) {
		return toGraph(schema).getETag(ac);
	}

	@Override
	public Result<? extends HibNode> findNodes(HibSchemaVersion version, String branchUuid, MeshAuthUser user, ContainerType type) {
		return toGraph(version).getNodes(branchUuid, user, type);
	}

	@Override
	public Result<? extends HibSchema> findAll(HibProject project) {
		return toGraph(project).getSchemaContainerRoot().findAll();
	}

	@Override
	public void addSchema(HibSchema schema) {
		boot.get().schemaContainerRoot().addItem(toGraph(schema));
	}

	@Override
	public Result<HibSchemaVersion> findActiveSchemaVersions(HibBranch branch) {
		Branch graphBranch = toGraph(branch);
		return graphBranch.findActiveSchemaVersions();
	}

	@Override
	public Stream<? extends NodeGraphFieldContainer> getFieldContainers(HibSchemaVersion version, String branchUuid) {
		SchemaVersion graphVersion = toGraph(version);
		return graphVersion.getFieldContainers(branchUuid);
	}

}
