package com.gentics.mesh.core.endpoint.node;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.binary.BinaryDataProcessor;
import com.gentics.mesh.core.binary.BinaryProcessorRegistry;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.binary.BinaryRoot;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.image.spi.ImageInfo;
import com.gentics.mesh.core.image.spi.ImageManipulator;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.error.NodeVersionConflictException;
import com.gentics.mesh.core.rest.node.field.BinaryFieldTransformRequest;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.etc.config.MeshUploadOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.storage.BinaryStorage;
import com.gentics.mesh.util.FileUtils;
import com.gentics.mesh.util.NodeUtil;
import com.gentics.mesh.util.RxUtil;

import dagger.Lazy;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;

/**
 * Handler which contains field API specific request handlers.
 */
public class BinaryFieldHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(BinaryFieldHandler.class);

	private ImageManipulator imageManipulator;

	private Database db;

	private Lazy<BootstrapInitializer> boot;

	private BinaryFieldResponseHandler binaryFieldResponseHandler;

	private BinaryStorage binaryStorage;

	private BinaryProcessorRegistry binaryProcessorRegistry;

	private HandlerUtilities utils;

	@Inject
	public BinaryFieldHandler(ImageManipulator imageManipulator,
		Database db,
		Lazy<BootstrapInitializer> boot,
		BinaryFieldResponseHandler binaryFieldResponseHandler,
		BinaryStorage binaryStorage,
		BinaryProcessorRegistry binaryProcessorRegistry,
		HandlerUtilities utils) {

		this.imageManipulator = imageManipulator;
		this.db = db;
		this.boot = boot;
		this.binaryFieldResponseHandler = binaryFieldResponseHandler;
		this.binaryStorage = binaryStorage;
		this.binaryProcessorRegistry = binaryProcessorRegistry;
		this.utils = utils;
	}

	public void handleReadBinaryField(RoutingContext rc, String uuid, String fieldName) {
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
		db.asyncTx(() -> {
			Project project = ac.getProject();
			Node node = project.getNodeRoot().loadObjectByUuid(ac, uuid, READ_PUBLISHED_PERM);
			// Language language = boot.get().languageRoot().findByLanguageTag(languageTag);
			// if (language == null) {
			// throw error(NOT_FOUND, "error_language_not_found", languageTag);
			// }

			Branch branch = ac.getBranch(node.getProject());
			NodeGraphFieldContainer fieldContainer = node.findVersion(ac.getNodeParameters().getLanguageList(), branch.getUuid(),
				ac.getVersioningParameters().getVersion());
			if (fieldContainer == null) {
				throw error(NOT_FOUND, "object_not_found_for_version", ac.getVersioningParameters().getVersion());
			}
			BinaryGraphField binaryField = fieldContainer.getBinary(fieldName);
			if (binaryField == null) {
				throw error(NOT_FOUND, "error_binaryfield_not_found_with_name", fieldName);
			}
			binaryFieldResponseHandler.handle(rc, binaryField);
		}).doOnError(ac::fail).subscribe();
	}

	private void validateFileUpload(FileUpload ul, String fieldName) {
		MeshUploadOptions uploadOptions = Mesh.mesh().getOptions().getUploadOptions();
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
		validateFileUpload(ul, fieldName);
		// This the name and path of the file to be moved to a new location.
		// This will be changed because it is possible that the file has to be moved multiple times
		// (if the transaction failed and has to be repeated).
		ac.put("sourceFile", ul.uploadedFileName());

		db.tx(() -> {
			Project project = ac.getProject();
			Branch branch = ac.getBranch();
			Node node = project.getNodeRoot().loadObjectByUuid(ac, nodeUuid, UPDATE_PERM);

			Language language = boot.get().languageRoot().findByLanguageTag(languageTag);
			if (language == null) {
				throw error(NOT_FOUND, "error_language_not_found", languageTag);
			}

			// Load the current latest draft
			NodeGraphFieldContainer latestDraftVersion = node.getGraphFieldContainer(languageTag, branch, ContainerType.DRAFT);

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
			NodeGraphFieldContainer baseVersionContainer = node.findVersion(languageTag, branch.getUuid(), nodeVersion);
			if (baseVersionContainer == null) {
				throw error(BAD_REQUEST, "node_error_draft_not_found", nodeVersion, languageTag);
			}

			List<FieldContainerChange> baseVersionDiff = baseVersionContainer.compareTo(latestDraftVersion);
			List<FieldContainerChange> requestVersionDiff = Arrays.asList(new FieldContainerChange(fieldName, FieldChangeTypes.UPDATED));

			// Compare both sets of change sets
			List<FieldContainerChange> intersect = baseVersionDiff.stream().filter(requestVersionDiff::contains).collect(Collectors.toList());

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

			utils.eventAction(batch -> {
				// Create a new node version field container to store the upload
				NodeGraphFieldContainer newDraftVersion = node.createGraphFieldContainer(languageTag, branch, ac.getUser(), latestDraftVersion, true);

				// Check whether the binary with the given hashsum was already stored
				BinaryRoot binaryRoot = boot.get().meshRoot().getBinaryRoot();
				String hash = FileUtils.hash(ul.uploadedFileName());
				Binary binary = binaryRoot.findByHash(hash);

				// Create a new binary if the data was not already stored
				boolean storeBinary = binary == null;
				if (storeBinary) {
					binary = binaryRoot.create(hash, ul.size());
				}

				// Get the potential existing field
				BinaryGraphField oldField = newDraftVersion.getBinary(fieldName);

				// Create the new field
				BinaryGraphField field = newDraftVersion.createBinary(fieldName, binary);

				// Reuse the existing properties
				if (oldField != null) {
					oldField.copyTo(field);

					// If the old field was an image and the current upload is not an image we need to reset the custom image specific attributes.
					if (oldField.hasProcessableImage() && !NodeUtil.isProcessableImage(ul.contentType())) {
						field.setImageDominantColor(null);
					}
				}

				// Process the upload which will update the binary field
				processUpload(ac, ul, field, storeBinary);

				// Now get rid of the old field
				if (oldField != null) {
					oldField.removeField(newDraftVersion);
				}
				// If the binary field is the segment field, we need to update the webroot info in the node
				if (field.getFieldKey().equals(newDraftVersion.getSchemaContainerVersion().getSchema().getSegmentField())) {
					newDraftVersion.updateWebrootPathInfo(branch.getUuid(), "node_conflicting_segmentfield_upload");
				}

				batch.add(newDraftVersion.onUpdated(branch.getUuid(), DRAFT));
			});
			return node.transformToRest(ac, 0);
		}).subscribe(model -> ac.send(model, CREATED), ac::fail);
	}

	/**
	 * Processes the upload and set the binary information (e.g.: image dimensions) within the provided field. The binary data will be stored in the
	 * {@link BinaryStorage} if desired.
	 * 
	 * @param ac
	 * @param ul
	 *            Upload to process
	 * @param field
	 *            Field which will be updated with the extracted information
	 * @param storeBinary
	 *            Whether to store the data in the binary store
	 */
	private void processUpload(ActionContext ac, FileUpload ul, BinaryGraphField field, boolean storeBinary) {

		// Process the upload and extract needed information
		String contentType = ul.contentType();
		for (BinaryDataProcessor p : binaryProcessorRegistry.getProcessors(contentType)) {
			try {
				p.process(ul, field);
			} catch (Exception e) {
				log.warn("Processing of upload {" + ul.fileName() + "/" + ul.uploadedFileName() + "} in handler {" + p.getClass() + "}", e);
			}
		}

		// Store the data
		if (storeBinary) {
			Binary binary = field.getBinary();
			String binaryUuid = binary.getUuid();
			String uploadFile = ul.uploadedFileName();
			AsyncFile asyncFile = Mesh.vertx().fileSystem().openBlocking(uploadFile, new OpenOptions());
			Flowable<Buffer> stream = RxUtil.toBufferFlow(asyncFile);
			binaryStorage.store(stream, binaryUuid).andThen(Single.just(ul.size())).blockingGet();
		}

	}

	/**
	 * Handle image transformation. This operation will utilize the binary data of the existing field and apply the transformation options. The new binary data
	 * will be stored and the field will be updated accordingly.
	 * 
	 * @param rc
	 *            routing context
	 * @param uuid
	 * @param fieldName
	 */
	public void handleTransformImage(RoutingContext rc, String uuid, String fieldName) {
		validateParameter(uuid, "uuid");
		validateParameter(fieldName, "fieldName");
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
		BinaryFieldTransformRequest transformation = JsonUtil.readValue(ac.getBodyAsString(), BinaryFieldTransformRequest.class);
		if (isEmpty(transformation.getLanguage())) {
			throw error(BAD_REQUEST, "image_error_language_not_set");
		}

		FileSystem fs = new Vertx(vertx).fileSystem();
		db.asyncTx(() -> {
			// Load needed elements
			Project project = ac.getProject();
			Node node = project.getNodeRoot().loadObjectByUuid(ac, uuid, UPDATE_PERM);

			Language language = boot.get().languageRoot().findByLanguageTag(transformation.getLanguage());
			if (language == null) {
				throw error(NOT_FOUND, "error_language_not_found", transformation.getLanguage());
			}

			NodeGraphFieldContainer latestDraftVersion = node.getLatestDraftFieldContainer(language.getLanguageTag());
			if (latestDraftVersion == null) {
				throw error(NOT_FOUND, "error_language_not_found", language.getLanguageTag());
			}

			FieldSchema fieldSchema = latestDraftVersion.getSchemaContainerVersion().getSchema().getField(fieldName);
			if (fieldSchema == null) {
				throw error(BAD_REQUEST, "error_schema_definition_not_found", fieldName);
			}
			if (!(fieldSchema instanceof BinaryFieldSchema)) {
				throw error(BAD_REQUEST, "error_found_field_is_not_binary", fieldName);
			}

			BinaryGraphField initialField = latestDraftVersion.getBinary(fieldName);
			if (initialField == null) {
				throw error(NOT_FOUND, "error_binaryfield_not_found_with_name", fieldName);
			}

			if (!initialField.hasProcessableImage()) {
				throw error(BAD_REQUEST, "error_transformation_non_image", fieldName);
			}

			try {
				// Prepare the imageManipulationParameter using the transformation request as source
				ImageManipulationParameters parameters = new ImageManipulationParametersImpl();
				parameters.setWidth(transformation.getWidth());
				parameters.setHeight(transformation.getHeight());
				parameters.setRect(transformation.getCropRect());
				if (parameters.getRect() != null) {
					parameters.setCropMode(CropMode.RECT);
				}

				parameters.validate();

				// Update the binary field with the new information
				utils.eventAction(batch -> {
					Branch branch = ac.getBranch();

					// Create a new node version field container to store the upload
					NodeGraphFieldContainer newDraftVersion = node.createGraphFieldContainer(language.getLanguageTag(), branch, ac.getUser(),
						latestDraftVersion,
						true);

					String binaryUuid = initialField.getBinary().getUuid();
					Flowable<Buffer> stream = binaryStorage.read(binaryUuid);

					// Use the focal point which is stored along with the binary field if no custom point was included in the query parameters.
					// Otherwise the query parameter focal point will be used and thus override the stored focal point.
					FocalPoint focalPoint = initialField.getImageFocalPoint();
					if (parameters.getFocalPoint() == null && focalPoint != null) {
						parameters.setFocalPoint(focalPoint);
					}

					// Resize the original image and store the result in the filesystem
					Single<TransformationResult> obsTransformation = imageManipulator.handleResize(stream, binaryUuid, parameters).flatMap(file -> {
						Flowable<Buffer> obs = RxUtil.toBufferFlow(file.getFile());

						// Hash the resized image data and store it using the computed fieldUuid + hash
						Single<String> hash = FileUtils.hash(obs);

						// The image was stored and hashed. Now we need to load the stored file again and check the image properties
						Single<ImageInfo> info = imageManipulator.readImageInfo(file.getPath());

						return Single.zip(hash, info, (hashV, infoV) -> {
							// Return a POJO which hold all information that is needed to update the field
							TransformationResult result = new TransformationResult(hashV, file.getProps().size(), infoV, file.getPath());
							return Single.just(result);
						}).flatMap(e -> e);
					});

					// Now that the binary data has been resized and inspected we can use this information to create a new binary and store it.
					TransformationResult result = obsTransformation.blockingGet();
					String hash = result.getHash();
					BinaryRoot binaryRoot = boot.get().meshRoot().getBinaryRoot();
					Binary binary = binaryRoot.findByHash(hash);

					// Check whether the binary was already stored.
					if (binary == null) {
						// Open the file again since we already read from it. We need to read it again in order to store it in the binary storage.
						Flowable<Buffer> data = fs.rxOpen(result.getFilePath(), new OpenOptions()).toFlowable().flatMap(RxUtil::toBufferFlow);
						binary = binaryRoot.create(hash, result.getSize());
						binaryStorage.store(data, binary.getUuid()).andThen(Single.just(result)).toCompletable().blockingAwait();
					} else {
						log.debug("Data of resized image with hash {" + hash + "} has already been stored. Skipping store.");
					}

					// Now create the binary field in which we store the information about the file
					BinaryGraphField oldField = newDraftVersion.getBinary(fieldName);
					BinaryGraphField field = newDraftVersion.createBinary(fieldName, binary);
					if (oldField != null) {
						oldField.copyTo(field);
						oldField.remove();
					}
					field.getBinary().setSize(result.getSize());
					field.setMimeType(result.getMimeType());
					// TODO should we rename the image, if the extension is wrong?
					field.getBinary().setImageHeight(result.getImageInfo().getHeight());
					field.getBinary().setImageWidth(result.getImageInfo().getWidth());
					String branchUuid = node.getProject().getBranchRoot().getLatestBranch().getUuid();
					batch.add(newDraftVersion.onCreated(branchUuid, DRAFT));
				});
				// Finally update the search index and return the updated node
				return node.transformToRest(ac, 0);
			} catch (GenericRestException e) {
				throw e;
			} catch (Exception e) {
				log.error("Error while transforming image", e);
				throw error(INTERNAL_SERVER_ERROR, "error_internal");
			}
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

}
