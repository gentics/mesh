package com.gentics.mesh.core.endpoint.node;

import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.binary.BinaryDataProcessor;
import com.gentics.mesh.core.binary.BinaryDataProcessorContext;
import com.gentics.mesh.core.binary.BinaryProcessorRegistryImpl;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.image.spi.ImageManipulator;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.NodeVersionConflictException;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.MeshUploadOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.storage.BinaryStorage;
import com.gentics.mesh.util.FileUtils;
import com.gentics.mesh.util.NodeUtil;
import com.gentics.mesh.util.RxUtil;
import com.gentics.mesh.util.Tuple;
import com.gentics.mesh.util.UUIDUtil;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.MultiMap;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;

/**
 * Handler which contains field API specific request handlers.
 */
public class BinaryUploadHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(BinaryUploadHandler.class);

	private final Database db;

	private final Lazy<BootstrapInitializer> boot;

	private final BinaryStorage binaryStorage;

	private final BinaryProcessorRegistryImpl binaryProcessorRegistry;

	private final HandlerUtilities utils;

	private FileSystem fs;

	private final MeshOptions options;

	private final Binaries binaries;

	private final WriteLock writeLock;

	@Inject
	public BinaryUploadHandler(ImageManipulator imageManipulator,
		Database db,
		Lazy<BootstrapInitializer> boot,
		BinaryFieldResponseHandler binaryFieldResponseHandler,
		BinaryStorage binaryStorage,
		BinaryProcessorRegistryImpl binaryProcessorRegistry,
		HandlerUtilities utils, Vertx rxVertx,
		MeshOptions options,
		Binaries binaries,
		WriteLock writeLock) {
		this.db = db;
		this.boot = boot;

		this.binaryStorage = binaryStorage;
		this.binaryProcessorRegistry = binaryProcessorRegistry;
		this.utils = utils;
		this.fs = rxVertx.fileSystem();
		this.options = options;
		this.binaries = binaries;
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

		String languageTag = attributes.get("language");
		if (isEmpty(languageTag)) {
			throw error(BAD_REQUEST, "upload_error_no_language");
		}

		String nodeVersion = attributes.get("version");
		if (isEmpty(nodeVersion)) {
			throw error(BAD_REQUEST, "upload_error_no_version");
		}

		Set<FileUpload> fileUploads = ac.getFileUploads();
		if (fileUploads.isEmpty()) {
			throw error(BAD_REQUEST, "node_error_no_binarydata_found");
		}

		// Check the file upload limit
		if (fileUploads.size() > 1) {
			throw error(BAD_REQUEST, "node_error_more_than_one_binarydata_included");
		}
		FileUpload ul = fileUploads.iterator().next();
		// TODO fail on multiple multipart formdata files
		validateFileUpload(ul, fieldName);

		UploadContext ctx = new UploadContext();
		ctx.setUpload(ul);

		// First process the upload data
		hashUpload(ul).flatMap(hash -> {
			return postProcessUpload(new BinaryDataProcessorContext(ac, nodeUuid, fieldName, ul, hash))
				.toList()
				.map(list -> Tuple.tuple(hash, list));
		}).flatMap(modifierListAndHash -> {
			String hash = modifierListAndHash.v1();
			List<Consumer<BinaryGraphField>> modifierList = modifierListAndHash.v2();
			ctx.setHash(hash);

			// Check whether the binary with the given hashsum was already stored
			Binary binary = binaries.findByHash(hash).runInNewTx();

			// Create a new binary uuid if the data was not already stored
			if (binary == null) {
				ctx.setBinaryUuid(UUIDUtil.randomUUID());
				ctx.setInvokeStore();
			}

			return storeUploadInTemp(ctx, ul, hash)
				.andThen(Single.defer(() -> storeUploadInGraph(ac, modifierList, ctx, nodeUuid, languageTag, nodeVersion, fieldName)));
		}).onErrorResumeNext(e -> {
			if (ctx.isInvokeStore()) {
				String tmpId = ctx.getTemporaryId();
				if (log.isDebugEnabled()) {
					log.debug("Error detected. Purging previously stored upload for tempId {}", tmpId, e);
				}
				return binaryStorage.purgeTemporaryUpload(tmpId).doOnError(e1 -> {
					log.error("Error while purging temporary upload for tempId {}", tmpId, e1);
				}).onErrorComplete().andThen(Single.error(e));
			} else {
				return Single.error(e);
			}
		}).flatMap(n -> {
			if (ctx.isInvokeStore()) {
				String binaryUuid = ctx.getBinaryUuid();
				String tmpId = ctx.getTemporaryId();
				if (log.isDebugEnabled()) {
					log.debug("Moving upload with binaryUuid {} and tempId {} into place", binaryUuid, tmpId);
				}
				return binaryStorage.moveInPlace(binaryUuid, tmpId).andThen(Single.just(n));
			} else {
				return Single.just(n);
			}
		}).subscribe(model -> ac.send(model, CREATED), ac::fail);

	}

	private Completable storeUploadInTemp(UploadContext ctx, FileUpload ul, String hash) {
		String uploadFilePath = ul.uploadedFileName();
		if (ctx.isInvokeStore()) {
			return binaryStorage.storeInTemp(uploadFilePath, ctx.getTemporaryId());
		} else {
			// File has already been stored. Lets remove the upload from the vert.x tmpdir. We no longer need it.
			return fs.rxDelete(uploadFilePath)
				.doOnComplete(() -> {
					if (log.isTraceEnabled()) {
						log.trace("Removed temporary file {}", uploadFilePath);
					}
				})
				.doOnError(e -> {
					log.warn("Failed to remove upload from tmpDir {}", uploadFilePath, e);
				}).onErrorComplete();
		}
	}

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
			ContentDaoWrapper contentDao = tx.data().contentDao();
			HibProject project = ac.getProject();
			HibBranch branch = ac.getBranch();
			Node node = project.getNodeRoot().loadObjectByUuid(ac, nodeUuid, UPDATE_PERM);

			utils.eventAction(batch -> {

				// We need to check whether someone else has stored the binary in the meanwhile
				Binary binary = binaries.findByHash(hash).runInExistingTx(tx);
				if (binary == null) {
					binary = binaries.create(binaryUuid, hash, upload.size()).runInExistingTx(tx);
				}
				HibLanguage language = tx.data().languageDao().findByLanguageTag(languageTag);
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
			return node.transformToRestSync(ac, 0);

		});
	}

	/**
	 * Processes the upload and set the binary information (e.g.: image dimensions) within the provided field. The binary data will be stored in the
	 * {@link BinaryStorage} if desired.
	 * 
	 * @param ctx
	 * @return Consumers which modify the graph field
	 */
	private Observable<Consumer<BinaryGraphField>> postProcessUpload(BinaryDataProcessorContext ctx) {
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

}
