package com.gentics.mesh.core.endpoint.node;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.binary.BinaryProcessorRegistryImpl;
import com.gentics.mesh.core.data.s3binary.S3Binaries;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.image.ImageManipulator;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.MeshUploadOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.storage.s3.S3BinaryStorage;
import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.vertx.core.MultiMap;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;

import javax.inject.Inject;

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

	private void validateFileUpload(FileUpload ul, String fieldName) {
		MeshUploadOptions uploadOptions = options.getUploadOptions();
		long byteLimit = uploadOptions.getByteLimit();

		if (ul.size() > byteLimit) {
			if (log.isDebugEnabled()) {
				log.debug("Upload size of {" + ul.size() + "} exceeds limit of {" + byteLimit + "} by {" + (ul.size() - byteLimit) + "} bytes.");
			}
			String humanReadableFileSize = org.apache.commons.io.FileUtils.byteCountToDisplaySize(ul.size());
			String humanReadableUploadLimit = org.apache.commons.io.FileUtils.byteCountToDisplaySize(byteLimit);
			throw error(BAD_REQUEST, "node_error_uploadlimit_reached", humanReadableFileSize, humanReadableUploadLimit);
		}

		if (isEmpty(ul.fileName())) {
			throw error(BAD_REQUEST, "field_binary_error_emptyfilename", fieldName);
		}
		if (isEmpty(ul.contentType())) {
			throw error(BAD_REQUEST, "field_binary_error_emptymimetype", fieldName);
		}
	}

	/**
	 * Handle a request to create a new field.
	 * 
	 * @param ac
	 * @param nodeUuid
	 *            UUID of the node which should be updated
	 * @param fieldName
	 *            Name of the field which should be created
	 * @param attributes
	 *            Additional form data attributes
	 */
	public void handleUpdateField(InternalActionContext ac, String nodeUuid, String fieldName, MultiMap attributes) {
		validateParameter(nodeUuid, "uuid");
		validateParameter(fieldName, "fieldName");
		s3binaryStorage.createPresignedUrl(nodeUuid,fieldName);
		Observable.just(new NodeResponse()).subscribe(model ->
				ac.send(model, FOUND), ac::fail);
	}



/*
	private Single<String> hashUpload(FileUpload ul) {
		String uploadFilePath = ul.uploadedFileName();
		return fs.rxOpen(uploadFilePath, new OpenOptions())
			.flatMapPublisher(RxUtil::toBufferFlow)
			.to(FileUtils::hash).doOnError(e -> {
				log.error("Error while hashing upload {}", uploadFilePath, e);
			});
	}

	private Single<NodeResponse> storeUploadInGraph(InternalActionContext ac, List<Consumer<BinaryGraphField>> fieldModifier, UploadContext context,
		String nodeUuid,
		String languageTag, String nodeVersion,
		String fieldName) {
		FileUpload upload = context.getUpload();
		String hash = context.getHash();
		String binaryUuid = context.getBinaryUuid();

		return db.singleTxWriteLock(tx -> {
			ContentDaoWrapper contentDao = tx.contentDao();
			HibProject project = tx.getProject(ac);
			HibBranch branch = tx.getBranch(ac);
			NodeDaoWrapper nodeDao = tx.nodeDao();
			HibNode node = nodeDao.loadObjectByUuid(project, ac, nodeUuid, UPDATE_PERM);

			utils.eventAction(batch -> {

				// We need to check whether someone else has stored the binary in the meanwhile
				HibBinary binary = binaries.findByHash(hash).runInExistingTx(tx);
				if (binary == null) {
					binary = binaries.create(binaryUuid, hash, upload.size()).runInExistingTx(tx);
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
				if (!(fieldSchema instanceof BinaryFieldSchema)) {
					// TODO Add support for other field types
					throw error(BAD_REQUEST, "error_found_field_is_not_binary", fieldName);
				}

				// Create a new node version field container to store the upload
				NodeGraphFieldContainer newDraftVersion = contentDao.createGraphFieldContainer(node, languageTag, branch, ac.getUser(),
					latestDraftVersion,
					true);

				// Get the potential existing field
				BinaryGraphField oldField = newDraftVersion.getBinary(fieldName);

				// Create the new field
				BinaryGraphField field = newDraftVersion.createBinary(fieldName, binary);

				// Reuse the existing properties
				if (oldField != null) {
					oldField.copyTo(field);

					// If the old field was an image and the current upload is not an image we need to reset the custom image specific attributes.
					if (oldField.hasProcessableImage() && !NodeUtil.isProcessableImage(upload.contentType())) {
						field.setImageDominantColor(null);
					}
				}

				// Now set the field infos. This will override any copied values as well.
				field.setFileName(upload.fileName());
				field.setMimeType(upload.contentType());
				field.getBinary().setSize(upload.size());

				for (Consumer<BinaryGraphField> modifier : fieldModifier) {
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
			});
			return nodeDao.transformToRestSync(node, ac, 0);
		});
	}

	/**
	 * Processes the upload and set the binary information (e.g.: image dimensions) within the provided field. The binary data will be stored in the
	 * {@link S3BinaryStorage} if desired.
	 * 
	 * @param ctx
	 * @return Consumers which modify the graph field
	 */
	/*private Observable<Consumer<BinaryGraphField>> postProcessUpload(BinaryDataProcessorContext ctx) {
		FileUpload upload = ctx.getUpload();
		String contentType = upload.contentType();
		List<BinaryDataProcessor> processors = binaryProcessorRegistry.getProcessors(contentType);

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
*/
}
