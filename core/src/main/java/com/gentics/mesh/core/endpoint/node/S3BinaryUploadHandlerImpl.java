package com.gentics.mesh.core.endpoint.node;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.binary.BinaryProcessorRegistryImpl;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.S3BinaryGraphField;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.s3binary.S3Binaries;
import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.image.ImageManipulator;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.NodeVersionConflictException;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.s3binary.S3RestResponse;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.S3BinaryFieldSchema;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.MeshUploadOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.storage.s3.S3BinaryStorage;
import com.gentics.mesh.util.NodeUtil;
import com.gentics.mesh.util.UUIDUtil;
import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.MultiMap;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * @see S3BinaryUploadHandler
 */
public class S3BinaryUploadHandlerImpl extends AbstractHandler implements S3BinaryUploadHandler {

	private static final Logger log = LoggerFactory.getLogger(S3BinaryUploadHandlerImpl.class);

	private final Database db;

	private final Lazy<BootstrapInitializer> boot;

	private final S3BinaryStorage s3binaryStorage;

	private final BinaryProcessorRegistryImpl binaryProcessorRegistry;

	private final HandlerUtilities utils;

	private FileSystem fs;

	private final MeshOptions options;

	private final S3Binaries s3binaries;

	private final WriteLock writeLock;

	@Inject
	public S3BinaryUploadHandlerImpl(ImageManipulator imageManipulator,
									 Database db,
									 Lazy<BootstrapInitializer> boot,
									 BinaryFieldResponseHandler binaryFieldResponseHandler,
									 S3BinaryStorage s3binaryStorage,
									 BinaryProcessorRegistryImpl binaryProcessorRegistry,
									 HandlerUtilities utils, Vertx rxVertx,
									 MeshOptions options,
									 S3Binaries s3binaries,
									 WriteLock writeLock) {
		this.db = db;
		this.boot = boot;

		this.s3binaryStorage = s3binaryStorage;
		this.binaryProcessorRegistry = binaryProcessorRegistry;
		this.utils = utils;
		this.fs = rxVertx.fileSystem();
		this.options = options;
		this.s3binaries = s3binaries;
		this.writeLock = writeLock;
	}

	/**
	 * Handle a request to create a new field.
	 *
	 * @param ac
	 * @param nodeUuid   UUID of the node which should be updated
	 * @param fieldName  Name of the field which should be created
	 * @param attributes Additional form data attributes
	 */
	public void handleUpdateField(InternalActionContext ac, String nodeUuid, String fieldName, MultiMap attributes) {
		validateParameter(nodeUuid, "uuid");
		validateParameter(fieldName, "fieldName");

		String languageTag = attributes.get("language");
		if (isEmpty(languageTag)) {
			throw error(BAD_REQUEST, "upload_error_no_language");
		}

		String nodeVersion = attributes.get("version");
		if (isEmpty(nodeVersion)) {
			throw error(BAD_REQUEST, "upload_error_no_version");
		}

		S3RestResponse s3RestResponse = s3binaryStorage.createPresignedUrl(nodeUuid, fieldName);

		S3UploadContext s3UploadContext = new S3UploadContext();
		// Create a new binary uuid if the data was not already stored
		s3UploadContext.setS3ObjectKey(nodeUuid + "/" + fieldName);
		s3UploadContext.setBinaryUuid(UUIDUtil.randomUUID());
		s3UploadContext.setInvokeStore();

		storeUploadInGraph(ac, s3UploadContext, nodeUuid, languageTag, nodeVersion, fieldName)
				.flatMapObservable((x)-> Observable.just(s3RestResponse))
				.subscribe(model ->
				ac.send(model, FOUND), ac::fail);
	}

	private Single<NodeResponse> storeUploadInGraph(InternalActionContext ac, S3UploadContext context,
													String nodeUuid,
													String languageTag, String nodeVersion,
													String fieldName) {
		String binaryUuid = context.getBinaryUuid();
		String s3ObjectKey = context.getS3ObjectKey();

		return db.singleTxWriteLock(tx -> {
			ContentDaoWrapper contentDao = tx.contentDao();
			HibProject project = tx.getProject(ac);
			HibBranch branch = tx.getBranch(ac);
			NodeDaoWrapper nodeDao = tx.nodeDao();
			HibNode node = nodeDao.loadObjectByUuid(project, ac, nodeUuid, UPDATE_PERM);

			utils.eventAction(batch -> {

				// We need to check whether someone else has stored the binary in the meanwhile
				S3HibBinary s3HibBinary = s3binaries.create(binaryUuid, s3ObjectKey).runInExistingTx(tx);
				HibLanguage language = tx.languageDao().findByLanguageTag(languageTag);
				if (language == null) {
					throw error(NOT_FOUND, "error_language_not_found", languageTag);
				}

				// Load the current latest draft
				NodeGraphFieldContainer latestDraftVersion = contentDao.getGraphFieldContainer(node, languageTag, branch, ContainerType.DRAFT);

				if (latestDraftVersion == null) {
					throw error(NOT_FOUND, "error_language_not_found", languageTag);
				}

				// Load the base version field container in order to create the diff
				NodeGraphFieldContainer baseVersionContainer = contentDao.findVersion(node, languageTag, branch.getUuid(), nodeVersion);
				if (baseVersionContainer == null) {
					throw error(BAD_REQUEST, "node_error_draft_not_found", nodeVersion, languageTag);
				}

				List<FieldContainerChange> baseVersionDiff = contentDao.compareTo(baseVersionContainer, latestDraftVersion);
				List<FieldContainerChange> requestVersionDiff = Arrays.asList(new FieldContainerChange(fieldName, FieldChangeTypes.UPDATED));

				// Compare both sets of change sets
				List<FieldContainerChange> intersect = baseVersionDiff.stream().filter(requestVersionDiff::contains)
						.collect(Collectors.toList());

				// Check whether the update was not based on the latest draft version. In that case a conflict check needs to occur.
				if (!latestDraftVersion.getVersion().equals(nodeVersion)) {

					// Check whether a conflict has been detected
					if (intersect.size() > 0) {
						NodeVersionConflictException conflictException = new NodeVersionConflictException("node_error_conflict_detected");
						conflictException.setOldVersion(baseVersionContainer.getVersion().toString());
						conflictException.setNewVersion(latestDraftVersion.getVersion().toString());
						for (FieldContainerChange fcc : intersect) {
							conflictException.addConflict(fcc.getFieldCoordinates());
						}
						throw conflictException;
					}
				}

				FieldSchema fieldSchema = latestDraftVersion.getSchemaContainerVersion().getSchema().getField(fieldName);
				if (fieldSchema == null) {
					throw error(BAD_REQUEST, "error_schema_definition_not_found", fieldName);
				}
				if (!(fieldSchema instanceof S3BinaryFieldSchema)) {
					// TODO Add support for other field types
					throw error(BAD_REQUEST, "error_found_field_is_not_binary", fieldName);
				}

				// Create a new node version field container to store the upload
				NodeGraphFieldContainer newDraftVersion = contentDao.createGraphFieldContainer(node, languageTag, branch, ac.getUser(),
						latestDraftVersion,
						true);
				// Create the new field
				S3BinaryGraphField field = newDraftVersion.createS3Binary(fieldName, s3HibBinary);

				// Now get rid of the old field
				// If the binary field is the segment field, we need to update the webroot info in the node
				if (field.getFieldKey().equals(newDraftVersion.getSchemaContainerVersion().getSchema().getSegmentField())) {
					contentDao.updateWebrootPathInfo(newDraftVersion, branch.getUuid(), "node_conflicting_segmentfield_upload");
				}

				if (ac.isPurgeAllowed() && newDraftVersion.isAutoPurgeEnabled() && latestDraftVersion.isPurgeable()) {
					contentDao.purge(latestDraftVersion);
				}

				batch.add(newDraftVersion.onUpdated(branch.getUuid(), DRAFT));
			});
			return nodeDao.transformToRestSync(node, ac, 0);
		});
	}
}
