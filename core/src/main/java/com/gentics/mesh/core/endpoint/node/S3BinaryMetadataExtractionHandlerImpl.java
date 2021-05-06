package com.gentics.mesh.core.endpoint.node;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.S3BinaryGraphField;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.s3binary.S3Binaries;
import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.NodeVersionConflictException;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.s3binary.S3BinaryMetadataRequest;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.S3BinaryFieldSchema;
import com.gentics.mesh.core.s3binary.S3BinaryDataProcessor;
import com.gentics.mesh.core.s3binary.S3BinaryDataProcessorContext;
import com.gentics.mesh.core.s3binary.S3BinaryProcessorRegistryImpl;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.storage.S3BinaryStorage;
import com.gentics.mesh.util.NodeUtil;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.MultiMap;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;
import io.vertx.reactivex.core.Vertx;

import javax.inject.Inject;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Handler for the metadata extraction of the S3 Binaries.
 */
public class S3BinaryMetadataExtractionHandlerImpl extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(S3BinaryMetadataExtractionHandlerImpl.class);
	private final Database db;
	private final S3BinaryStorage s3BinaryStorage;
	private final HandlerUtilities utils;
	private final MeshOptions options;
	private final S3Binaries s3binaries;
	private final S3BinaryProcessorRegistryImpl s3binaryProcessorRegistry;
	private final Vertx vertx;

	@Inject
	public S3BinaryMetadataExtractionHandlerImpl(Database db, S3BinaryStorage s3BinaryStorage, HandlerUtilities utils,
												 Vertx rxVertx, MeshOptions options, S3Binaries s3binaries, S3BinaryProcessorRegistryImpl s3binaryProcessorRegistry) {
		this.db = db;
		this.vertx = rxVertx;
		this.s3BinaryStorage = s3BinaryStorage;
		this.utils = utils;
		this.options = options;
		this.s3binaries = s3binaries;
		this.s3binaryProcessorRegistry = s3binaryProcessorRegistry;
	}

	/**
	 * Handle a request to create a new field.
	 *
	 * @param ac
	 * @param nodeUuid   UUID of the node which should be updated
	 * @param fieldName  Name of the field which should be created
	 * @param attributes Additional form data attributes
	 */
	public void handleMetadataExtraction(InternalActionContext ac, String nodeUuid, String fieldName, MultiMap attributes) {
		validateParameter(nodeUuid, "uuid");
		validateParameter(fieldName, "fieldName");

		S3BinaryMetadataRequest request = JsonUtil.readValue(ac.getBodyAsString(), S3BinaryMetadataRequest.class);
		String languageTag = request.getLanguage();
		if (isEmpty(languageTag)) {
			throw error(BAD_REQUEST, "upload_error_no_language");
		}

		String nodeVersion = request.getVersion();
		if (isEmpty(nodeVersion)) {
			throw error(BAD_REQUEST, "upload_error_no_version");
		}

		S3UploadContext ctx = new S3UploadContext();


		String bucket = options.getS3Options().getBucket();
		String objectKey = nodeUuid + "/" + fieldName;
		s3BinaryStorage
				.exists(bucket, objectKey)
				//read from aws and return buffer with data
				.flatMap(res -> {
					if (res) {
						return s3BinaryStorage.read(bucket, objectKey).singleOrError();
					} else return null;
				}).flatMap(fileBuffer -> {
			if (nonNull(fileBuffer) || fileBuffer.getBytes().length > 0) {
				return db.singleTx(tx -> s3binaries
						.findByS3ObjectKey(nodeUuid + "/" + fieldName)
						.runInExistingTx(tx)
						.getFileName())
						.flatMap(fileName -> {
							String mimeTypeForFilename = MimeMapping.getMimeTypeForFilename(fileName);
							File tmpFile = new File(System.getProperty("java.io.tmpdir"), fileName);
							vertx.fileSystem().writeFileBlocking(tmpFile.getAbsolutePath(), fileBuffer);
							byte[] fileData = fileBuffer.getBytes();
							FileUpload fileUpload = new FileUpload() {

								@Override
								public String uploadedFileName() {
									return tmpFile.getAbsolutePath();
								}

								@Override
								public long size() {
									return fileData.length;
								}

								@Override
								public String name() {
									return fileName;
								}

								@Override
								public String fileName() {
									return fileName;
								}

								@Override
								public String contentType() {
									return mimeTypeForFilename;
								}

								@Override
								public String contentTransferEncoding() {
									// TODO Auto-generated method stub
									return null;
								}

								@Override
								public String charSet() {
									// TODO Auto-generated method stub
									return "UTF-8";
								}
							};
							ctx.setFileUpload(fileUpload);
							ctx.setS3ObjectKey(nodeUuid + "/" + fieldName);
							ctx.setFileName(fileName);
							ctx.setFileSize(fileData.length);
							return Single.just(fileUpload);
						}).flatMap(fileUpload -> postProcessUpload(new S3BinaryDataProcessorContext(ac, nodeUuid, fieldName, fileUpload))
									.toList()).flatMap(postProcess ->  storeUploadInGraph(ac, postProcess, ctx, nodeUuid, languageTag, nodeVersion, fieldName));
			} else {
				log.error("Could not read input image");
				return Single.error(error(INTERNAL_SERVER_ERROR, "image_error_reading_failed"));
			}
		}).subscribe(model ->
				ac.send(model, OK), ac::fail);
	}

	private Single<NodeResponse> storeUploadInGraph(InternalActionContext ac, List<Consumer<S3BinaryGraphField>> fieldModifier, S3UploadContext context,
													String nodeUuid,
													String languageTag, String nodeVersion,
													String fieldName) {
		FileUpload upload = context.getFileUpload();
		String s3ObjectKey = context.getS3ObjectKey();
		String fileName = context.getFileName();

		return db.singleTxWriteLock(tx -> {
			ContentDaoWrapper contentDao = tx.contentDao();
			HibProject project = tx.getProject(ac);
			HibBranch branch = tx.getBranch(ac);
			NodeDaoWrapper nodeDao = tx.nodeDao();
			HibNode node = nodeDao.loadObjectByUuid(project, ac, nodeUuid, UPDATE_PERM);

			utils.eventAction(batch -> {

				// We need to check whether someone else has stored the binary in the meanwhile
				S3HibBinary s3binary = s3binaries.findByS3ObjectKey(s3ObjectKey).runInExistingTx(tx);
				if (s3binary == null) {
					s3binary = s3binaries.create(nodeUuid, s3ObjectKey, fileName).runInExistingTx(tx);
				}
				HibLanguage language = tx.languageDao().findByLanguageTag(languageTag);
				if (language == null) {
					throw error(NOT_FOUND, "error_language_not_found", languageTag);
				}

				// Load the current latest draft
				NodeGraphFieldContainer latestDraftVersion = contentDao.getGraphFieldContainer(node, languageTag, branch, ContainerType.DRAFT);

				if (latestDraftVersion == null) {
					// latestDraftVersion = node.createGraphFieldContainer(language, branch, ac.getUser());
					// TODO Maybe it would be better to just create a new field container for the language?
					// In that case we would also need to:
					// * check for segment field conflicts
					// * update display name
					// * fail if mandatory fields are missing
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

				// Get the potential existing field
				S3BinaryGraphField oldField = newDraftVersion.getS3Binary(fieldName);

				// Create the new field
				S3BinaryGraphField field = newDraftVersion.createS3Binary(fieldName, s3binary);

				// Reuse the existing properties
				if (oldField != null) {
					oldField.copyTo(field);

					// If the old field was an image and the current upload is not an image we need to reset the custom image specific attributes.
					if (oldField.hasProcessableImage() && !NodeUtil.isProcessableImage(upload.contentType())) {
						field.setImageDominantColor(null);
					}
				}

				// Now set the field infos. This will override any copied values as well.
				field.setMimeType(upload.contentType());
				field.setFileSize(upload.size());

				for (Consumer<S3BinaryGraphField> modifier : fieldModifier) {
					modifier.accept(field);
				}

				// Now get rid of the old field
				if (oldField != null) {
					oldField.removeField(newDraftVersion);
				}
				// If the binary field is the segment field, we need to update the webroot info in the node
				if (field.getFieldKey().equals(newDraftVersion.getSchemaContainerVersion().getSchema().getSegmentField())) {
					contentDao.updateWebrootPathInfo(newDraftVersion, branch.getUuid(), "node_conflicting_segmentfield_upload");
				}

				if (ac.isPurgeAllowed() && newDraftVersion.isAutoPurgeEnabled() && latestDraftVersion.isPurgeable()) {
					contentDao.purge(latestDraftVersion);
				}

				batch.add(newDraftVersion.onUpdated(branch.getUuid(), DRAFT));
				batch.add(toGraph(s3binary).onMetadataExtracted(nodeUuid,s3ObjectKey));
			});
			return nodeDao.transformToRestSync(node, ac, 0);
		});
	}

	private Observable<Consumer<S3BinaryGraphField>> postProcessUpload(S3BinaryDataProcessorContext ctx) {
		FileUpload upload = ctx.getUpload();
		String contentType = upload.contentType();
		List<S3BinaryDataProcessor> processors = s3binaryProcessorRegistry.getProcessors(contentType);

		return Observable.fromIterable(processors).flatMapMaybe(p -> p.process(ctx)
				.doOnSuccess(s -> {
					log.info(
							"Processing of upload {" + upload.fileName() + "/" + upload.uploadedFileName() + "} in handler {" + p.getClass()
									+ "} completed.");
				})
				.doOnComplete(() -> {
					log.warn(
							"Processing of upload {" + upload.fileName() + "/" + upload.uploadedFileName() + "} in handler {" + p.getClass()
									+ "} completed.");
				}));
	}

}
