package com.gentics.mesh.core.verticle.release;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.verticle.handler.HandlerUtilities.operateNoTx;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang.NotImplementedException;
import org.elasticsearch.common.collect.Tuple;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.MicroschemaReferenceList;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaReferenceList;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.index.node.NodeIndexHandler;
import com.gentics.mesh.util.ResultInfo;

import io.vertx.core.eventbus.DeliveryOptions;
import rx.Observable;
import rx.Single;

/**
 * CRUD Handler for Releases
 */
public class ReleaseCrudHandler extends AbstractCrudHandler<Release, ReleaseResponse> {

	@Inject
	public ReleaseCrudHandler(Database db) {
		super(db);
	}

	@Override
	public RootVertex<Release> getRootVertex(InternalActionContext ac) {
		return ac.getProject().getReleaseRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac, String uuid) {
		throw new NotImplementedException("Release can't be deleted");
	}

	@Override
	public void handleCreate(InternalActionContext ac) {
		operateNoTx(ac, () -> {
			Database db = MeshInternal.get().database();

			ResultInfo info = db.tx(() -> {
				SearchQueue queue = MeshInternal.get().boot().meshRoot().getSearchQueue();
				RootVertex<Release> root = getRootVertex(ac);
				SearchQueueBatch batch = queue.createBatch();

				Release created = root.create(ac, batch);
				Project project = created.getProject();
				ReleaseResponse model = created.transformToRestSync(ac, 0);
				ResultInfo resultInfo = new ResultInfo(model, batch);
				resultInfo.setProperty("path", created.getAPIPath(ac));
				resultInfo.setProperty("projectUuid", project.getUuid());
				resultInfo.setProperty("releaseUuid", created.getUuid());
				return resultInfo;
			});

			// The release has been created now lets start the node migration
			DeliveryOptions options = new DeliveryOptions();
			options.addHeader(NodeMigrationVerticle.PROJECT_UUID_HEADER, info.getProperty("projectUuid"));
			options.addHeader(NodeMigrationVerticle.UUID_HEADER, info.getProperty("releaseUuid"));
			Mesh.vertx().eventBus().send(NodeMigrationVerticle.RELEASE_MIGRATION_ADDRESS, null, options);

			SearchQueueBatch batch = info.getBatch();
			ac.setLocation(info.getProperty("path"));
			// Finally process the batch
			batch.processSync();
			return info.getModel();
		}, model -> ac.send(model, CREATED));

	}

	/**
	 * Handle getting the schema versions of a release.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of release to be queried
	 */
	public void handleGetSchemaVersions(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		operateNoTx(() -> {
			Release release = getRootVertex(ac).loadObjectByUuid(ac, uuid, GraphPermission.READ_PERM);
			return getSchemaVersions(release);
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	/**
	 * Handle assignment of schema version to a release.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of release
	 */
	public void handleAssignSchemaVersion(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		operateNoTx(() -> {
			RootVertex<Release> root = getRootVertex(ac);
			Release release = root.loadObjectByUuid(ac, uuid, UPDATE_PERM);
			SchemaReferenceList schemaReferenceList = ac.fromJson(SchemaReferenceList.class);
			Project project = ac.getProject();
			SchemaContainerRoot schemaContainerRoot = project.getSchemaContainerRoot();
			List<DeliveryOptions> events = new ArrayList<>();

			Tuple<SearchQueueBatch, Single<SchemaReferenceList>> tuple = db.tx(() -> {
				SearchQueueBatch batch = MeshInternal.get().boot().meshRoot().getSearchQueue().createBatch();

				// Resolve the list of references to graph schema container versions
				for (SchemaReference reference : schemaReferenceList) {
					SchemaContainerVersion version = schemaContainerRoot.fromReference(reference);
					// Invoke schema migration for each found schema version

					SchemaContainerVersion assignedVersion = release.getVersion(version.getSchemaContainer());
					if (assignedVersion != null && assignedVersion.getVersion() > version.getVersion()) {
						throw error(BAD_REQUEST, "release_error_downgrade_schema_version", version.getName(),
								Integer.toString(assignedVersion.getVersion()), Integer.toString(version.getVersion()));
					}
					release.assignSchemaVersion(version);

					// Create index queue entries for creating indices
					batch.addEntry(NodeIndexHandler.getIndexName(project.getUuid(), release.getUuid(), version.getUuid(), DRAFT), Node.TYPE,
							SearchQueueEntryAction.CREATE_INDEX);
					batch.addEntry(NodeIndexHandler.getIndexName(project.getUuid(), release.getUuid(), version.getUuid(), PUBLISHED), Node.TYPE,
							SearchQueueEntryAction.CREATE_INDEX);

					DeliveryOptions options = new DeliveryOptions();
					options.addHeader(NodeMigrationVerticle.PROJECT_UUID_HEADER, release.getRoot().getProject().getUuid());
					options.addHeader(NodeMigrationVerticle.RELEASE_UUID_HEADER, release.getUuid());
					options.addHeader(NodeMigrationVerticle.UUID_HEADER, version.getSchemaContainer().getUuid());
					options.addHeader(NodeMigrationVerticle.FROM_VERSION_UUID_HEADER, assignedVersion.getUuid());
					options.addHeader(NodeMigrationVerticle.TO_VERSION_UUID_HEADER, version.getUuid());
					events.add(options);
				}

				return Tuple.tuple(batch, getSchemaVersions(release));
			});

			// 1. Process batch and create need indices
			tuple.v1().processSync();

			// 2. Invoke migrations which will populate the created index
			for (DeliveryOptions option : events) {
				Mesh.vertx().eventBus().send(NodeMigrationVerticle.SCHEMA_MIGRATION_ADDRESS, null, option);
			}
			return tuple.v2();

		}).subscribe(model -> ac.send(model, OK), ac::fail);

	}

	/**
	 * Handle getting the microschema versions of a release.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of release to be queried
	 */
	public void handleGetMicroschemaVersions(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		operateNoTx(() -> {
			Release release = getRootVertex(ac).loadObjectByUuid(ac, uuid, GraphPermission.READ_PERM);
			return getMicroschemaVersions(release);
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	/**
	 * Handle assignment of microschema version to a release.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of release
	 */
	public void handleAssignMicroschemaVersion(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		operateNoTx(() -> {
			RootVertex<Release> root = getRootVertex(ac);
			Release release = root.loadObjectByUuid(ac, uuid, UPDATE_PERM);
			MicroschemaReferenceList microschemaReferenceList = ac.fromJson(MicroschemaReferenceList.class);
			MicroschemaContainerRoot microschemaContainerRoot = ac.getProject().getMicroschemaContainerRoot();

			return db.tx(() -> {
				// Transform the list of references into microschema container version vertices
				for (MicroschemaReference reference : microschemaReferenceList) {
					MicroschemaContainerVersion version = microschemaContainerRoot.fromReference(reference);

					MicroschemaContainerVersion assignedVersion = release.getVersion(version.getSchemaContainer());
					if (assignedVersion != null && assignedVersion.getVersion() > version.getVersion()) {
						throw error(BAD_REQUEST, "release_error_downgrade_microschema_version", version.getName(),
								Integer.toString(assignedVersion.getVersion()), Integer.toString(version.getVersion()));
					}
					release.assignMicroschemaVersion(version);

					// start microschema migration
					DeliveryOptions options = new DeliveryOptions();
					options.addHeader(NodeMigrationVerticle.PROJECT_UUID_HEADER, release.getRoot().getProject().getUuid());
					options.addHeader(NodeMigrationVerticle.RELEASE_UUID_HEADER, release.getUuid());
					options.addHeader(NodeMigrationVerticle.UUID_HEADER, version.getSchemaContainer().getUuid());
					options.addHeader(NodeMigrationVerticle.FROM_VERSION_UUID_HEADER, assignedVersion.getUuid());
					options.addHeader(NodeMigrationVerticle.TO_VERSION_UUID_HEADER, version.getUuid());
					Mesh.vertx().eventBus().send(NodeMigrationVerticle.MICROSCHEMA_MIGRATION_ADDRESS, null, options);
				}
				return getMicroschemaVersions(release);
			});
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	/**
	 * Get the rest model of the schema versions of the release.
	 * 
	 * @param release
	 *            release
	 * @return single emitting the rest model
	 */
	protected Single<SchemaReferenceList> getSchemaVersions(Release release) {
		try {
			return Observable.from(release.findAllSchemaVersions()).map(SchemaContainerVersion::transformToReference).collect(() -> {
				return new SchemaReferenceList();
			}, (x, y) -> {
				x.add(y);
			}).toSingle();
		} catch (Exception e) {
			throw error(INTERNAL_SERVER_ERROR, "Unknown error while getting schema versions", e);
		}
	}

	/**
	 * Get the rest model of the microschema versions of the release
	 * 
	 * @param release
	 *            release
	 * @return single emitting the rest model
	 */
	protected Single<MicroschemaReferenceList> getMicroschemaVersions(Release release) {
		try {
			return Observable.from(release.findAllMicroschemaVersions()).map(MicroschemaContainerVersion::transformToReference).collect(() -> {
				return new MicroschemaReferenceList();
			}, (x, y) -> {
				x.add(y);
			}).toSingle();
		} catch (Exception e) {
			throw error(INTERNAL_SERVER_ERROR, "Unknown error while getting microschema versions", e);
		}
	}
}
