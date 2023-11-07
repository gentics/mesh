package com.gentics.mesh.core.endpoint.node;

import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.binary.BinaryDataProcessor;
import com.gentics.mesh.core.binary.BinaryDataProcessorContext;
import com.gentics.mesh.core.binary.BinaryProcessorRegistryImpl;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.PersistingContentDao;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.storage.BinaryStorage;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.image.ImageManipulator;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.NodeVersionConflictException;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.MeshUploadOptions;
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
 * @see BinaryUploadHandler
 */
public class BinaryUploadHandlerImpl extends AbstractBinaryUploadHandler implements BinaryUploadHandler {

	private static final Logger log = LoggerFactory.getLogger(BinaryUploadHandlerImpl.class);

	private final Lazy<BootstrapInitializer> boot;

	private final BinaryProcessorRegistryImpl binaryProcessorRegistry;

	private final HandlerUtilities utils;

	private FileSystem fs;

	private final WriteLock writeLock;

	@Inject
	public BinaryUploadHandlerImpl(
			ImageManipulator imageManipulator,
			Database db,
			Lazy<BootstrapInitializer> boot,
			BinaryFieldResponseHandler binaryFieldResponseHandler,
			BinaryStorage binaryStorage,
			BinaryProcessorRegistryImpl binaryProcessorRegistry,
			HandlerUtilities utils,
			Vertx rxVertx,
			MeshOptions options,
			Binaries binaries,
			WriteLock writeLock) {
		super(db, binaries, binaryStorage, options);

		this.boot = boot;
		this.binaryProcessorRegistry = binaryProcessorRegistry;
		this.utils = utils;
		this.fs = rxVertx.fileSystem();
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
		if (ul.size() < 1) {
			throw error(BAD_REQUEST, "field_binary_error_emptyfile", fieldName, ul.fileName());
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
	@Override
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

		List<FileUpload> fileUploads = ac.getFileUploads();
		if (fileUploads.isEmpty()) {
			throw error(BAD_REQUEST, "node_error_no_binarydata_found");
		}

		// Check the file upload limit
		if (fileUploads.size() > 1) {
			throw error(BAD_REQUEST, "node_error_more_than_one_binarydata_included");
		}

		boolean publish = Boolean.parseBoolean(attributes.get("publish"));

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
			List<Consumer<HibBinaryField>> modifierList = modifierListAndHash.v2();
			ctx.setHash(hash);

			// Check whether the binary with the given hashsum was already stored
			String binaryUuid = binaries.findByHash(hash)
					.mapInTx(b -> b != null ? b.getUuid() : null)
					.runInNewTx();

			// Possibly we try to upload the same binary onto the same node+field.
			Optional<NodeResponse> maybeExisting = Optional.empty();

			// Create a new binary uuid if the data was not already stored
			if (binaryUuid == null) {
				ctx.setBinaryUuid(UUIDUtil.randomUUID());
				ctx.setInvokeStore();
			} else if (fileNotExists(binaryUuid)) {
				ctx.setBinaryUuid(binaryUuid);
				ctx.setInvokeStore();
			} else {
				maybeExisting = db.tx((tx) -> {
					NodeDao nodeDao = tx.nodeDao();
					ContentDao contentDao = tx.contentDao();
					HibProject project = tx.getProject(ac);
					HibBranch branch = tx.getBranch(ac, project);
					HibNode node = nodeDao.loadObjectByUuid(project, ac, nodeUuid, InternalPermission.READ_PERM);
					HibNodeFieldContainer latestDraftVersion = contentDao.getFieldContainer(node, languageTag, branch, ContainerType.DRAFT);
					FieldSchema fieldSchema = latestDraftVersion.getSchemaContainerVersion().getSchema().getField(fieldName);
					if (fieldSchema != null) {
						HibBinaryField binaryField = latestDraftVersion.getBinary(fieldName);
						if (binaryField != null) {
							HibBinary binary = binaryField.getBinary();
							if (binary != null && binaryUuid.equals(binary.getUuid()) && hash.equals(binary.getSHA512Sum())) {
								return Optional.ofNullable(nodeDao.transformToRestSync(nodeDao.findByUuid(project, nodeUuid), ac, 0, languageTag));
							}
						}
					}
					return Optional.empty();
				});
			}
			return maybeExisting.map(existing -> Single.just(existing))
					.orElseGet(() -> storeUploadInTemp(ctx, ul, hash).andThen(Single.defer(() -> storeUploadInGraph(ac, modifierList, ctx, nodeUuid, languageTag, nodeVersion, fieldName, publish))));
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


	private boolean fileNotExists(String binaryUuid) {
		return !new File(binaryStorage.getFilePath(binaryUuid)).exists();
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

	private Single<NodeResponse> storeUploadInGraph(InternalActionContext ac, List<Consumer<HibBinaryField>> fieldModifier, UploadContext context,
		String nodeUuid,
		String languageTag, String nodeVersion,
		String fieldName, boolean publish) {
		FileUpload upload = context.getUpload();
		String hash = context.getHash();
		String binaryUuid = context.getBinaryUuid();

		return db.singleTxWriteLock(
			(batch, tx) -> {
				PersistingContentDao contentDao = tx.<CommonTx>unwrap().contentDao();
				HibProject project = tx.getProject(ac);
				HibBranch branch = tx.getBranch(ac);
				NodeDao nodeDao = tx.nodeDao();
				HibNode node = nodeDao.loadObjectByUuid(project, ac, nodeUuid, UPDATE_PERM);
				// Load the current latest draft
				HibNodeFieldContainer latestDraftVersion = contentDao.getFieldContainer(node, languageTag, branch, ContainerType.DRAFT);
				FieldSchema fieldSchema = latestDraftVersion.getSchemaContainerVersion().getSchema().getField(fieldName);

				if (fieldSchema == null) {
					throw error(BAD_REQUEST, "error_schema_definition_not_found", fieldName);
				}

				if (!(fieldSchema instanceof BinaryFieldSchema)) {
					// TODO Add support for other field types
					throw error(BAD_REQUEST, "error_found_field_is_not_binary", fieldName);
				}

				if (latestDraftVersion == null) {
					// latestDraftVersion = node.createGraphFieldContainer(language, branch, ac.getUser());
					// TODO Maybe it would be better to just create a new field container for the language?
					// In that case we would also need to:
					// * check for segment field conflicts
					// * update display name
					// * fail if mandatory fields are missing
					throw error(NOT_FOUND, "error_language_not_found", languageTag);
				}

				// We need to check whether someone else has stored the binary in the meanwhile
				HibBinary binary = binaries.findByHash(hash).runInExistingTx(tx);

				if (binary == null) {
					BinaryCheckStatus checkStatus = StringUtils.isNotBlank(((BinaryFieldSchema) fieldSchema).getCheckServiceUrl())
						? BinaryCheckStatus.POSTPONED
						: BinaryCheckStatus.ACCEPTED;

					binary = binaries.create(binaryUuid, hash, upload.size(), checkStatus).runInExistingTx(tx);
				}

				HibLanguage language = tx.languageDao().findByLanguageTag(languageTag);
				if (language == null) {
					throw error(NOT_FOUND, "error_language_not_found", languageTag);
				}

				// Load the base version field container in order to create the diff
				HibNodeFieldContainer baseVersionContainer = contentDao.findVersion(node, languageTag, branch.getUuid(), nodeVersion);
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

				// Create a new node version field container to store the upload
				HibNodeFieldContainer newDraftVersion = contentDao.createFieldContainer(node, languageTag, branch, ac.getUser(),
					latestDraftVersion,
					true);

				// Get the potential existing field
				HibBinaryField oldField = (HibBinaryField) contentDao.detachField(newDraftVersion.getBinary(fieldName));

				// Create the new field
				HibBinaryField field = newDraftVersion.createBinary(fieldName, binary);

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

				for (Consumer<HibBinaryField> modifier : fieldModifier) {
					modifier.accept(field);
				}

				// Now get rid of the old field
				newDraftVersion.removeField(oldField);

				// If the binary field is the segment field, we need to update the webroot info in the node
				// TODO FIXME This is already called in `PersistingContentDao.connectFieldContainer()`. Normally one should not update a container without reconnecting versions,
				// but currently MeshLocalClient does this, which may be illegal. The check and call below should be removed, once MeshLocalClientImpl is improved.
				if (field.getFieldKey().equals(contentDao.getSchemaContainerVersion(newDraftVersion).getSchema().getSegmentField())) {
					contentDao.updateWebrootPathInfo(newDraftVersion, branch.getUuid(), "node_conflicting_segmentfield_upload");
				}

				if (ac.isPurgeAllowed() && contentDao.isAutoPurgeEnabled(newDraftVersion) && contentDao.isPurgeable(latestDraftVersion)) {
					contentDao.purge(latestDraftVersion);
				}

				batch.add(contentDao.onUpdated(newDraftVersion, branch.getUuid(), DRAFT));
	
				if (publish) {
					HibNodeFieldContainer publishedContainer = contentDao.publish(node, ac, language.getLanguageTag(), branch, ac.getUser());
					// Invoke a store of the document since it must now also be added to the published index
					batch.add(contentDao.onPublish(publishedContainer, branch.getUuid()));
				}
				return nodeDao.transformToRestSync(node, ac, 0);
			});
	}


	/**
	 * Processes the upload and set the binary information (e.g.: image dimensions) within the provided field. The binary data will be stored in the
	 * {@link BinaryStorage} if desired.
	 *
	 * @param ctx
	 * @return Consumers which modify the graph field
	 */
	private Observable<Consumer<HibBinaryField>> postProcessUpload(BinaryDataProcessorContext ctx) {
		FileUpload upload = ctx.getUpload();
		String contentType = upload.contentType();
		List<BinaryDataProcessor> processors = binaryProcessorRegistry.getProcessors(contentType);

		return Observable.fromIterable(processors).flatMapMaybe(p -> p.process(ctx)
			.doOnSuccess(s -> {
				log.info(
					"Processing of upload {" + upload.fileName() + "/" + upload.uploadedFileName() + "} in handler {" + p.getClass()
						+ "} succeeded.");
			})
			.doOnComplete(() -> {
				log.warn(
					"Processing of upload {" + upload.fileName() + "/" + upload.uploadedFileName() + "} in handler {" + p.getClass()
						+ "} completed.");
			}));
	}

}
