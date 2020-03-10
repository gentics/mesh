package com.gentics.mesh.core.endpoint.branch;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.event.Assignment.ASSIGNED;
import static com.gentics.mesh.event.Assignment.UNASSIGNED;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.job.JobRoot;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.endpoint.handler.AbstractCrudHandler;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.branch.info.BranchInfoMicroschemaList;
import com.gentics.mesh.core.rest.branch.info.BranchInfoSchemaList;
import com.gentics.mesh.core.rest.branch.info.BranchMicroschemaInfo;
import com.gentics.mesh.core.rest.branch.info.BranchSchemaInfo;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.core.verticle.handler.WriteLockImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.PentaFunction;
import com.gentics.mesh.util.StreamUtil;

import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * CRUD Handler for Branches
 */
public class BranchCrudHandler extends AbstractCrudHandler<Branch, BranchResponse> {

	private static final Logger log = LoggerFactory.getLogger(BranchCrudHandler.class);

	private BootstrapInitializer boot;

	@Inject
	public BranchCrudHandler(Database db, HandlerUtilities utils, BootstrapInitializer boot, WriteLockImpl writeLock) {
		super(db, utils, writeLock);
		this.boot = boot;
	}

	@Override
	public RootVertex<Branch> getRootVertex(InternalActionContext ac) {
		return ac.getProject().getBranchRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac, String uuid) {
		throw new NotImplementedException("Branch can't be deleted");
	}

	/**
	 * Handle getting the schema versions of a branch.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of branch to be queried
	 */
	public void handleGetSchemaVersions(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		db.asyncTx(() -> {
			Branch branch = getRootVertex(ac).loadObjectByUuid(ac, uuid, READ_PERM);
			return Single.just(getSchemaVersionsInfo(branch));
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	/**
	 * Handle assignment of schema version to a branch.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of branch
	 */
	public void handleAssignSchemaVersion(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		try (WriteLock lock = writeLock.lock()) {
			db.asyncTx(() -> {
				RootVertex<Branch> root = getRootVertex(ac);
				Branch branch = root.loadObjectByUuid(ac, uuid, UPDATE_PERM);
				BranchInfoSchemaList schemaReferenceList = ac.fromJson(BranchInfoSchemaList.class);
				Project project = ac.getProject();
				SchemaContainerRoot schemaContainerRoot = project.getSchemaContainerRoot();

				Single<BranchInfoSchemaList> branchList = utils.eventAction(event -> {

					// Resolve the list of references to graph schema container versions
					for (SchemaReference reference : schemaReferenceList.getSchemas()) {
						SchemaContainerVersion version = schemaContainerRoot.fromReference(reference);
						SchemaContainerVersion assignedVersion = branch.findLatestSchemaVersion(version.getSchemaContainer());
						if (assignedVersion != null && Double.valueOf(assignedVersion.getVersion()) > Double.valueOf(version.getVersion())) {
							throw error(BAD_REQUEST, "branch_error_downgrade_schema_version", version.getName(), assignedVersion.getVersion(),
								version.getVersion());
						}
						branch.assignSchemaVersion(ac.getUser(), version, event);
					}

					return Single.just(getSchemaVersionsInfo(branch));
				});

				// 2. Invoke migrations which will populate the created index
				MeshEvent.triggerJobWorker(boot.mesh());

				return branchList;

			}).subscribe(model -> ac.send(model, OK), ac::fail);
		}

	}

	/**
	 * Handle getting the microschema versions of a branch.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of branch to be queried
	 */
	public void handleGetMicroschemaVersions(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		db.asyncTx(() -> {
			Branch branch = getRootVertex(ac).loadObjectByUuid(ac, uuid, GraphPermission.READ_PERM);
			return Single.just(getMicroschemaVersions(branch));
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	/**
	 * Handle assignment of microschema version to a branch.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of branch
	 */
	public void handleAssignMicroschemaVersion(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		try (WriteLock lock = writeLock.lock()) {
			utils.syncTx(ac, tx -> {
				RootVertex<Branch> root = getRootVertex(ac);
				Branch branch = root.loadObjectByUuid(ac, uuid, UPDATE_PERM);
				BranchInfoMicroschemaList microschemaReferenceList = ac.fromJson(BranchInfoMicroschemaList.class);
				MicroschemaContainerRoot microschemaContainerRoot = ac.getProject().getMicroschemaContainerRoot();

				User user = ac.getUser();
				utils.eventAction(batch -> {
					// Transform the list of references into microschema container version vertices
					for (MicroschemaReference reference : microschemaReferenceList.getMicroschemas()) {
						MicroschemaContainerVersion version = microschemaContainerRoot.fromReference(reference);

						MicroschemaContainerVersion assignedVersion = branch.findLatestMicroschemaVersion(version.getSchemaContainer());
						if (assignedVersion != null && Double.valueOf(assignedVersion.getVersion()) > Double.valueOf(version.getVersion())) {
							throw error(BAD_REQUEST, "branch_error_downgrade_microschema_version", version.getName(), assignedVersion.getVersion(),
								version.getVersion());
						}
						branch.assignMicroschemaVersion(user, version, batch);
					}
				});

				MeshEvent.triggerJobWorker(boot.mesh());
				return getMicroschemaVersions(branch);
			}, model -> ac.send(model, OK));
		}
	}

	/**
	 * Get the REST model of the schema versions of the branch.
	 * 
	 * @param branch
	 *            branch
	 * @return single emitting the rest model
	 */
	public BranchInfoSchemaList getSchemaVersionsInfo(Branch branch) {
		List<BranchSchemaInfo> list = StreamUtil.toStream(branch.findAllLatestSchemaVersionEdges())
			.map(edge -> {
				SchemaReference reference = edge.getSchemaContainerVersion().transformToReference();
				BranchSchemaInfo info = new BranchSchemaInfo(reference);
				info.setMigrationStatus(edge.getMigrationStatus());
				info.setJobUuid(edge.getJobUuid());
				return info;
			}).collect(Collectors.toList());

		return new BranchInfoSchemaList(list);
	}

	/**
	 * Get the REST model of the microschema versions of the branch.
	 * 
	 * @param branch
	 *            branch
	 * @return single emitting the rest model
	 */
	public BranchInfoMicroschemaList getMicroschemaVersions(Branch branch) {
		List<BranchMicroschemaInfo> list = StreamUtil.toStream(branch.findAllLatestMicroschemaVersionEdges()).map(edge -> {
			MicroschemaReference reference = edge.getMicroschemaContainerVersion().transformToReference();
			BranchMicroschemaInfo info = new BranchMicroschemaInfo(reference);
			info.setMigrationStatus(edge.getMigrationStatus());
			info.setJobUuid(edge.getJobUuid());
			return info;
		}).collect(Collectors.toList());

		return new BranchInfoMicroschemaList(list);
	}

	public void handleMigrateRemainingMicronodes(InternalActionContext ac, String branchUuid) {
		handleMigrateRemaining(ac, branchUuid,
			Branch::findActiveMicroschemaVersions,
			JobRoot::enqueueMicroschemaMigration);
	}

	/**
	 * Helper handler which will handle requests for processing remaining not yet migrated nodes.
	 *
	 * @param ac
	 * @param branchUuid
	 */
	public void handleMigrateRemainingNodes(InternalActionContext ac, String branchUuid) {
		handleMigrateRemaining(ac, branchUuid,
			Branch::findActiveSchemaVersions,
			JobRoot::enqueueSchemaMigration);
	}

	/**
	 * A generic version to migrate remaining nodes/micronodes.
	 * 
	 * @param ac
	 *            The action context
	 * @param branchUuid
	 *            The branch uuid
	 * @param activeSchemas
	 *            A function that returns an iterable of all active schema versions / microschema versions
	 * @param enqueueMigration
	 *            A function that enqueues a new migration job
	 * @param <T>
	 *            The type of the schema version (either schema version or microschema version)
	 */
	private <T extends GraphFieldSchemaContainerVersion> void handleMigrateRemaining(InternalActionContext ac, String branchUuid,
		Function<Branch, Iterable<T>> activeSchemas, PentaFunction<JobRoot, User, Branch, T, T, Job> enqueueMigration) {
		try (WriteLock lock = writeLock.lock()) {
			utils.syncTx(ac, tx -> {
				Branch branch = ac.getProject().getBranchRoot().findByUuid(branchUuid);

				// Get all active versions and group by Microschema
				Collection<? extends List<T>> versions = StreamSupport.stream(activeSchemas.apply(branch).spliterator(), false)
					.collect(Collectors.groupingBy(GraphFieldSchemaContainerVersion::getName)).values();

				// Get latest versions of all active Microschemas
				Map<String, T> latestVersions = versions.stream()
					.map(list -> (T) list.stream().max(Comparator.comparing(Function.identity())).get())
					.collect(Collectors.toMap(GraphFieldSchemaContainerVersion::getName, Function.identity()));

				versions.stream()
					.flatMap(Collection::stream)
					// We don't need to migrate the latest active version
					.filter(version -> version != latestVersions.get(version.getName()))
					.forEach(schemaVersion -> {
						enqueueMigration.apply(boot.jobRoot(), ac.getUser(), branch, schemaVersion, latestVersions.get(schemaVersion.getName()));
					});

				return message(ac, "schema_migration_invoked");
			}, model -> {
				// Trigger job worker after jobs have been queued
				MeshEvent.triggerJobWorker(boot.mesh());
				ac.send(model, OK);
			});
		}
	}

	/**
	 * Handle requests to make a branch the latest
	 * 
	 * @param ac
	 * @param branchUuid
	 */
	public void handleSetLatest(InternalActionContext ac, String branchUuid) {
		try (WriteLock lock = writeLock.lock()) {
			utils.syncTx(ac, (tx) -> {
				Branch branch = ac.getProject().getBranchRoot().loadObjectByUuid(ac, branchUuid, UPDATE_PERM);
				utils.eventAction(event -> {
					branch.setLatest();
					event.add(branch.onSetLatest());
				});
				return branch.transformToRestSync(ac, 0);
			}, model -> ac.send(model, OK));
		}
	}

	/**
	 * Handle the read branch tags request.
	 * 
	 * @param ac
	 *            Action context
	 * @param uuid
	 *            UUID of the branch for which the tags should be loaded
	 */
	public void readTags(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		utils.rxSyncTx(ac, (tx) -> {
			Branch branch = ac.getProject().getBranchRoot().loadObjectByUuid(ac, uuid, READ_PERM);
			TransformablePage<? extends Tag> tagPage = branch.getTags(ac.getUser(), ac.getPagingParameters());
			return tagPage.transformToRest(ac, 0);
		}, model -> ac.send(model, OK));
	}

	/**
	 * Handle the add tag request.
	 * 
	 * @param ac
	 *            Action context which also contains the branch information.
	 * @param uuid
	 *            Uuid of the branch to which tags should be added.
	 * @param tagUuid
	 *            Uuid of the tag which should be added to the branch.
	 */
	public void handleAddTag(InternalActionContext ac, String uuid, String tagUuid) {
		validateParameter(uuid, "uuid");
		validateParameter(tagUuid, "tagUuid");

		try (WriteLock lock = writeLock.lock()) {
			utils.syncTx(ac, (tx) -> {
				Branch branch = ac.getProject().getBranchRoot().loadObjectByUuid(ac, uuid, UPDATE_PERM);
				Tag tag = boot.meshRoot().getTagRoot().loadObjectByUuid(ac, tagUuid, READ_PERM);

				// TODO check if the branch is already tagged
				if (branch.hasTag(tag)) {
					if (log.isDebugEnabled()) {
						log.debug("Branch {{}} is already tagged with tag {{}}", branch.getUuid(), tag.getUuid());
					}
				} else {
					utils.eventAction(batch -> {
						branch.addTag(tag);
						batch.add(branch.onTagged(tag, ASSIGNED));
					});
				}

				return branch.transformToRestSync(ac, 0);
			}, model -> ac.send(model, OK));
		}
	}

	/**
	 * Remove the specified tag from the branch.
	 * 
	 * @param ac
	 *            Action context
	 * @param uuid
	 *            Uuid of the branch from which the tag should be removed
	 * @param tagUuid
	 *            Uuid of the tag which should be removed from the branch
	 */
	public void handleRemoveTag(InternalActionContext ac, String uuid, String tagUuid) {
		validateParameter(uuid, "uuid");
		validateParameter(tagUuid, "tagUuid");

		try (WriteLock lock = writeLock.lock()) {
			utils.syncTx(ac, () -> {
				Branch branch = ac.getProject().getBranchRoot().loadObjectByUuid(ac, uuid, UPDATE_PERM);
				Tag tag = boot.meshRoot().getTagRoot().loadObjectByUuid(ac, tagUuid, READ_PERM);

				// TODO check if the tag has already been removed

				if (branch.hasTag(tag)) {
					utils.eventAction(batch -> {
						batch.add(branch.onTagged(tag, UNASSIGNED));
						branch.removeTag(tag);
					});
				} else {
					if (log.isDebugEnabled()) {
						log.debug("Branch {{}} was not tagged with tag {{}}", branch.getUuid(), tag.getUuid());
					}

				}

			}, () -> ac.send(NO_CONTENT));
		}
	}

	/**
	 * Handle a bulk tag update request.
	 * 
	 * @param ac
	 *            Action context
	 * @param branchUuid
	 *            Uuid of the branch which should be updated
	 */
	public void handleBulkTagUpdate(InternalActionContext ac, String branchUuid) {
		validateParameter(branchUuid, "branchUuid");

		try (WriteLock lock = writeLock.lock()) {
			utils.rxSyncTx(ac, tx -> {
				Branch branch = ac.getProject().getBranchRoot().loadObjectByUuid(ac, branchUuid, UPDATE_PERM);

				TransformablePage<? extends Tag> page = utils.eventAction(batch -> {
					return branch.updateTags(ac, batch);
				});

				return page.transformToRest(ac, 0);
			}, model -> ac.send(model, OK));
		}
	}
}
