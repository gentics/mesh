package com.gentics.mesh.core.endpoint.branch;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
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

import javax.inject.Inject;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.BranchDAOActions;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.BranchDaoWrapper;
import com.gentics.mesh.core.data.dao.JobDaoWrapper;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.user.HibUser;
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
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.StreamUtil;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * CRUD Handler for Branches
 */
public class BranchCrudHandler extends AbstractCrudHandler<HibBranch, BranchResponse> {

	private static final Logger log = LoggerFactory.getLogger(BranchCrudHandler.class);

	private BootstrapInitializer boot;

	@Inject
	public BranchCrudHandler(Database db, HandlerUtilities utils, BootstrapInitializer boot, WriteLock writeLock, BranchDAOActions branchActions) {
		super(db, utils, writeLock, branchActions);
		this.boot = boot;
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
		utils.syncTx(ac, tx -> {
			HibProject project = tx.getProject(ac);
			BranchDaoWrapper branchDao = tx.data().branchDao();
			HibBranch branch = branchDao.loadObjectByUuid(project, ac, uuid, READ_PERM);
			return getSchemaVersionsInfo(branch);
		}, model -> ac.send(model, OK));
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
		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, tx -> {
				HibProject project = tx.getProject(ac);
				BranchDaoWrapper branchDao = tx.data().branchDao();
				SchemaDaoWrapper schemaDao = tx.data().schemaDao();

				HibBranch branch = branchDao.loadObjectByUuid(project, ac, uuid, UPDATE_PERM);
				BranchInfoSchemaList schemaReferenceList = ac.fromJson(BranchInfoSchemaList.class);

				BranchInfoSchemaList branchList = utils.eventAction(event -> {

					// Resolve the list of references to graph schema container versions
					for (SchemaReference reference : schemaReferenceList.getSchemas()) {
						HibSchemaVersion version = schemaDao.fromReference(project, reference);
						HibSchemaVersion assignedVersion = branch.findLatestSchemaVersion(version.getSchemaContainer());
						if (assignedVersion != null && Double.valueOf(assignedVersion.getVersion()) > Double.valueOf(version.getVersion())) {
							throw error(BAD_REQUEST, "branch_error_downgrade_schema_version", version.getName(), assignedVersion.getVersion(),
								version.getVersion());
						}
						branch.assignSchemaVersion(ac.getUser(), version, event);
					}

					return getSchemaVersionsInfo(branch);
				});

				// 2. Invoke migrations which will populate the created index
				MeshEvent.triggerJobWorker(boot.mesh());

				return branchList;

			}, model -> ac.send(model, OK));
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
		utils.syncTx(ac, tx -> {
			HibProject project = tx.getProject(ac);
			BranchDaoWrapper branchDao = tx.data().branchDao();
			HibBranch branch = branchDao.loadObjectByUuid(project, ac, uuid, InternalPermission.READ_PERM);
			return getMicroschemaVersions(branch);
		}, model -> ac.send(model, OK));
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

		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, tx -> {
				HibProject project = tx.getProject(ac);
				BranchDaoWrapper branchDao = tx.data().branchDao();
				HibBranch branch = branchDao.loadObjectByUuid(project, ac, uuid, UPDATE_PERM);
				BranchInfoMicroschemaList microschemaReferenceList = ac.fromJson(BranchInfoMicroschemaList.class);
				MicroschemaDaoWrapper microschemaDao = tx.data().microschemaDao();

				HibUser user = ac.getUser();
				utils.eventAction(batch -> {
					// Transform the list of references into microschema container version vertices
					for (MicroschemaReference reference : microschemaReferenceList.getMicroschemas()) {
						HibMicroschemaVersion version = microschemaDao.fromReference(tx.getProject(ac), reference);

						HibMicroschemaVersion assignedVersion = branch.findLatestMicroschemaVersion(version.getSchemaContainer());
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
	public BranchInfoSchemaList getSchemaVersionsInfo(HibBranch branch) {
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
	public BranchInfoMicroschemaList getMicroschemaVersions(HibBranch branch) {
		List<BranchMicroschemaInfo> list = StreamUtil.toStream(branch.findAllLatestMicroschemaVersionEdges()).map(assignment -> {
			MicroschemaReference reference = assignment.getMicroschemaContainerVersion().transformToReference();
			BranchMicroschemaInfo info = new BranchMicroschemaInfo(reference);
			info.setMigrationStatus(assignment.getMigrationStatus());
			info.setJobUuid(assignment.getJobUuid());
			return info;
		}).collect(Collectors.toList());

		return new BranchInfoMicroschemaList(list);
	}

	public void handleMigrateRemainingMicronodes(InternalActionContext ac, String branchUuid) {

		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, tx -> {
				JobDaoWrapper jobDao = tx.data().jobDao();
				BranchDaoWrapper branchDao = tx.data().branchDao();
				MicroschemaDaoWrapper microschemaDao = tx.data().microschemaDao();

				HibProject project = tx.getProject(ac);
				HibBranch branch = branchDao.findByUuid(project, branchUuid);

				// Get all active versions and group by name
				Collection<List<HibMicroschemaVersion>> versions = microschemaDao.findActiveMicroschemaVersions(branch).stream()
					.collect(Collectors.groupingBy(HibMicroschemaVersion::getName)).values();

				// Get latest versions of all active microschemas
				Map<String, HibMicroschemaVersion> latestVersions = versions.stream()
					.map(list -> {
						return list.stream().max(Comparator.comparing(Function.identity())).get();
					})
					.collect(Collectors.toMap(HibMicroschemaVersion::getName, Function.identity()));

				latestVersions.values().stream()
					.flatMap(v -> {
						return v.getPreviousVersions();
					})
					.forEach(schemaVersion -> {
						jobDao.enqueueMicroschemaMigration(ac.getUser(), branch, schemaVersion, latestVersions.get(schemaVersion.getName()));
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
	 * Helper handler which will handle requests for processing remaining not yet migrated nodes.
	 *
	 * @param ac
	 * @param branchUuid
	 */
	public void handleMigrateRemainingNodes(InternalActionContext ac, String branchUuid) {
		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, tx -> {
				JobDaoWrapper jobDao = tx.data().jobDao();
				BranchDaoWrapper branchDao = tx.data().branchDao();
				SchemaDaoWrapper schemaDao = tx.data().schemaDao();

				HibProject project = tx.getProject(ac);
				HibBranch branch = branchDao.findByUuid(project, branchUuid);

				// Get all active versions and group by name
				Collection<List<HibSchemaVersion>> versions = schemaDao.findActiveSchemaVersions(branch).stream()
					.collect(Collectors.groupingBy(HibSchemaVersion::getName)).values();

				// Get latest versions of all active schemas
				Map<String, HibSchemaVersion> latestVersions = versions.stream()
					.map(list -> {
						return list.stream().max(Comparator.comparing(Function.identity())).get();
					})
					.collect(Collectors.toMap(HibSchemaVersion::getName, Function.identity()));

				latestVersions.values().stream()
					.flatMap(v -> {
						return v.getPreviousVersions();
					})
					.forEach(schemaVersion -> {
						jobDao.enqueueSchemaMigration(ac.getUser(), branch, schemaVersion, latestVersions.get(schemaVersion.getName()));
					});

				return message(ac, "schema_migration_invoked");
			}, model -> {
				// Trigger job worker after jobs have been queued
				MeshEvent.triggerJobWorker(boot.mesh());
				ac.send(model, OK);
			});

		}
	}

	// /**
	// * A generic version to migrate remaining nodes/micronodes.
	// *
	// * @param ac
	// * The action context
	// * @param branchUuid
	// * The branch uuid
	// * @param activeSchemas
	// * A function that returns an iterable of all active schema versions / microschema versions
	// * @param enqueueMigration
	// * A function that enqueues a new migration job
	// * @param <T>
	// * The type of the schema version (either schema version or microschema version)
	// */
	// private <T extends HibFieldSchemaVersionElement<?, ?, ?, T>> void handleMigrateRemaining(InternalActionContext ac, String branchUuid,
	// Function<HibBranch, Iterable<T>> activeSchemas, PentaFunction<JobRoot, HibUser, HibBranch, T, T, HibJob> enqueueMigration) {
	// try (WriteLock lock = writeLock.lock(ac)) {
	// utils.syncTx(ac, tx -> {
	// HibProject project = ac.getProject();
	// BranchDaoWrapper branchDao = tx.data().branchDao();
	//
	// HibBranch branch = branchDao.findByUuid(project, branchUuid);
	// // Get all active versions and group by Microschema
	// Collection<? extends List<T>> versions = StreamSupport.stream(activeSchemas.apply(branch).spliterator(), false)
	// .collect(Collectors.groupingBy(HibFieldSchemaVersionElement::getName)).values();
	//
	// // Get latest versions of all active Microschemas
	// Map<String, T> latestVersions = versions.stream()
	// .map(list -> (T) list.stream().max(Comparator.comparing(Function.identity())).get())
	// .collect(Collectors.toMap(HibFieldSchemaVersionElement::getName, Function.identity()));
	//
	// latestVersions.values().stream()
	// .flatMap(v -> {
	// if (v instanceof HibMicroschemaVersion) {
	// return ((HibMicroschemaVersion) v).getPreviousVersions();
	// } else if (v instanceof HibSchemaVersion) {
	// return ((HibSchemaVersion) v).getPreviousVersions();
	// } else {
	// throw new RuntimeException("Unknown type {" + v + "}");
	// }
	// })
	// .forEach(schemaVersion -> {
	// enqueueMigration.apply(boot.jobRoot(), ac.getUser(), branch, schemaVersion, latestVersions.get(schemaVersion.getName()));
	// });
	//
	// return message(ac, "schema_migration_invoked");
	// }, model -> {
	// // Trigger job worker after jobs have been queued
	// MeshEvent.triggerJobWorker(boot.mesh());
	// ac.send(model, OK);
	// });
	// }
	// }

	/**
	 * Handle requests to make a branch the latest
	 * 
	 * @param ac
	 * @param branchUuid
	 */
	public void handleSetLatest(InternalActionContext ac, String branchUuid) {
		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, tx -> {
				HibProject project = tx.getProject(ac);
				BranchDaoWrapper branchDao = tx.data().branchDao();
				HibBranch branch = branchDao.loadObjectByUuid(project, ac, branchUuid, UPDATE_PERM);
				utils.eventAction(event -> {
					branch.setLatest();
					event.add(branch.onSetLatest());
				});
				return branchDao.transformToRestSync(branch, ac, 0);
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

		utils.syncTx(ac, tx -> {
			HibProject project = tx.getProject(ac);
			BranchDaoWrapper branchDao = tx.data().branchDao();
			HibBranch branch = branchDao.loadObjectByUuid(project, ac, uuid, READ_PERM);
			TransformablePage<? extends HibTag> tagPage = branch.getTags(ac.getUser(), ac.getPagingParameters());
			return tagPage.transformToRestSync(ac, 0);
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

		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, tx -> {
				HibProject project = tx.getProject(ac);
				BranchDaoWrapper branchDao = tx.data().branchDao();
				TagDaoWrapper tagDao = tx.data().tagDao();

				HibBranch branch = branchDao.loadObjectByUuid(project, ac, uuid, UPDATE_PERM);
				HibTag tag = tagDao.loadObjectByUuid(project, ac, tagUuid, READ_PERM);

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

				return branchDao.transformToRestSync(branch, ac, 0);
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

		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, tx -> {
				HibProject project = tx.getProject(ac);
				BranchDaoWrapper branchDao = tx.data().branchDao();
				TagDaoWrapper tagDao = tx.data().tagDao();

				HibBranch branch = branchDao.loadObjectByUuid(project, ac, uuid, UPDATE_PERM);
				HibTag tag = tagDao.loadObjectByUuid(project, ac, tagUuid, READ_PERM);

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

		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, tx -> {
				BranchDaoWrapper branchDao = tx.data().branchDao();
				HibProject project = tx.getProject(ac);

				HibBranch branch = branchDao.loadObjectByUuid(project, ac, branchUuid, UPDATE_PERM);
				TransformablePage<? extends HibTag> page = utils.eventAction(batch -> {
					return branch.updateTags(ac, batch);
				});

				return page.transformToRestSync(ac, 0);
			}, model -> ac.send(model, OK));
		}
	}
}
