package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.util.RxUtil.readEntireFile;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.image.spi.ImageInfo;
import com.gentics.mesh.core.image.spi.ImageManipulator;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.error.NodeVersionConflictException;
import com.gentics.mesh.core.rest.node.field.BinaryFieldTransformRequest;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.etc.config.MeshUploadOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.util.FileUtils;

import dagger.Lazy;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import rx.Single;

/**
 * Handler which contains field API specific request handlers.
 */
public class BinaryFieldHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(BinaryFieldHandler.class);

	private ImageManipulator imageManipulator;

	private Database db;

	private Lazy<BootstrapInitializer> boot;

	private BinaryFieldResponseHandler binaryFieldResponseHandler;

	private SearchQueue searchQueue;

	@Inject
	public BinaryFieldHandler(ImageManipulator imageManipulator, Database db, Lazy<BootstrapInitializer> boot, SearchQueue searchQueue,
			BinaryFieldResponseHandler binaryFieldResponseHandler) {
		this.imageManipulator = imageManipulator;
		this.db = db;
		this.boot = boot;
		this.searchQueue = searchQueue;
		this.binaryFieldResponseHandler = binaryFieldResponseHandler;
	}

	public void handleReadBinaryField(RoutingContext rc, String uuid, String fieldName) {
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
		db.operateTx(() -> {
			Project project = ac.getProject();
			Node node = project.getNodeRoot().loadObjectByUuid(ac, uuid, READ_PUBLISHED_PERM);
			// Language language = boot.get().languageRoot().findByLanguageTag(languageTag);
			// if (language == null) {
			// throw error(NOT_FOUND, "error_language_not_found", languageTag);
			// }

			Release release = ac.getRelease(node.getProject());
			NodeGraphFieldContainer fieldContainer = node.findNextMatchingFieldContainer(ac.getNodeParameters().getLanguageList(), release.getUuid(),
					ac.getVersioningParameters().getVersion());
			if (fieldContainer == null) {
				throw error(NOT_FOUND, "object_not_found_for_version", ac.getVersioningParameters().getVersion());
			}
			BinaryGraphField binaryField = fieldContainer.getBinary(fieldName);
			if (binaryField == null) {
				throw error(NOT_FOUND, "error_binaryfield_not_found_with_name", fieldName);
			}
			return Single.just(binaryField);
		}).subscribe(binaryField -> {
			db.tx(() -> {
				binaryFieldResponseHandler.handle(rc, binaryField);
			});
		}, ac::fail);
	}

	/**
	 * Handle a request to create a new field.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the node which should be updated
	 * @param fieldName
	 *            Name of the field which should be created
	 * @param attributes
	 *            Additional form data attributes
	 */
	public void handleUpdateBinaryField(InternalActionContext ac, String uuid, String fieldName, MultiMap attributes) {
		validateParameter(uuid, "uuid");
		validateParameter(fieldName, "fieldName");

		String languageTag = attributes.get("language");
		if (isEmpty(languageTag)) {
			throw error(BAD_REQUEST, "upload_error_no_language");
		}

		String nodeVersion = attributes.get("version");
		if (isEmpty(nodeVersion)) {
			throw error(BAD_REQUEST, "upload_error_no_version");
		}

		MeshUploadOptions uploadOptions = Mesh.mesh().getOptions().getUploadOptions();
		Set<FileUpload> fileUploads = ac.getFileUploads();
		if (fileUploads.isEmpty()) {
			throw error(BAD_REQUEST, "node_error_no_binarydata_found");
		}

		// Check the file upload limit
		if (fileUploads.size() > 1) {
			throw error(BAD_REQUEST, "node_error_more_than_one_binarydata_included");
		}
		FileUpload ul = fileUploads.iterator().next();
		long byteLimit = uploadOptions.getByteLimit();

		if (ul.size() > byteLimit) {
			if (log.isDebugEnabled()) {
				log.debug("Upload size of {" + ul.size() + "} exeeds limit of {" + byteLimit + "} by {" + (ul.size() - byteLimit) + "} bytes.");
			}
			String humanReadableFileSize = org.apache.commons.io.FileUtils.byteCountToDisplaySize(ul.size());
			String humanReadableUploadLimit = org.apache.commons.io.FileUtils.byteCountToDisplaySize(byteLimit);
			throw error(BAD_REQUEST, "node_error_uploadlimit_reached", humanReadableFileSize, humanReadableUploadLimit);
		}

		String contentType = ul.contentType();
		String fileName = ul.fileName();

		// This the name and path of the file to be moved to a new location.
		// This will be changed because it is possible that the file has to be moved multiple times
		// (if the transaction failed and has to be repeated).
		ac.put("sourceFile", ul.uploadedFileName());
		String hashSum = FileUtils.generateSha512Sum(ul.uploadedFileName());

		db.tx(() -> {
			Project project = ac.getProject();
			Release release = ac.getRelease();
			Node node = project.getNodeRoot().loadObjectByUuid(ac, uuid, UPDATE_PERM);

			Language language = boot.get().languageRoot().findByLanguageTag(languageTag);
			if (language == null) {
				throw error(NOT_FOUND, "error_language_not_found", languageTag);
			}

			// Load the current latest draft
			NodeGraphFieldContainer latestDraftVersion = node.getGraphFieldContainer(language, release, ContainerType.DRAFT);

			if (latestDraftVersion == null) {
				// latestDraftVersion = node.createGraphFieldContainer(language, release, ac.getUser());
				// TODO Maybe it would be better to just create a new field container for the language?
				// In that case we would also need to:
				// * check for segment field conflicts
				// * update display name
				// * fail if mandatory fields are missing
				throw error(NOT_FOUND, "error_language_not_found", languageTag);
			}

			// Load the base version field container in order to create the diff
			NodeGraphFieldContainer baseVersionContainer = node.findNextMatchingFieldContainer(Arrays.asList(languageTag), release.getUuid(),
					nodeVersion);
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

			SearchQueueBatch batch = searchQueue.create();
			// Create a new node version field container to store the upload
			NodeGraphFieldContainer newDraftVersion = node.createGraphFieldContainer(language, release, ac.getUser(), latestDraftVersion, true);
			BinaryGraphField field = newDraftVersion.createBinary(fieldName);
			String fieldUuid = field.getUuid();

			Single<ImageInfo> obsImage;

			// 3. Only gather image info for actual images. Otherwise return an empty image info object.
			if (contentType.startsWith("image/")) {
				try {
					// Caches the image info in the action context so that it does not need to be
					// calculated again if the transaction failed
					ImageInfo imageInfo = ac.get("imageInfo");
					if (imageInfo != null) {
						obsImage = Single.just(imageInfo);
					} else {
						obsImage = imageManipulator.readImageInfo(() -> {
						try {
							return new FileInputStream(ul.uploadedFileName());
						} catch (Exception e) {
							log.error("Could not load schema for node {" + node.getUuid() + "}");
							throw error(INTERNAL_SERVER_ERROR, "could not find upload file", e);
						}
						}).doOnSuccess(ii -> ac.put("imageInfo", ii));
					}
				} catch (Exception e) {
					log.error("Could not load schema for node {" + node.getUuid() + "}");
					throw error(INTERNAL_SERVER_ERROR, "could not find upload file", e);
				}

			} else {
				obsImage = Single.just(new ImageInfo());
			}

			// 4. Hash and store the file and update the field properties
			Single<TransformationResult> resultObs = obsImage.map((imageInfo) -> {
				moveBinaryFile(ac, fieldUuid, field.getSegmentedPath());
				return new TransformationResult(hashSum, 0, imageInfo);
			});

			TransformationResult info = resultObs.toBlocking().value();
			field.setFileName(fileName);
			field.setFileSize(ul.size());
			field.setMimeType(contentType);
			field.setSHA512Sum(info.getHash());
			field.setImageDominantColor(info.getImageInfo().getDominantColor());
			field.setImageHeight(info.getImageInfo().getHeight());
			field.setImageWidth(info.getImageInfo().getWidth());

			// If the binary field is the segment field, we need to update the webroot info in the node
			if (field.getFieldKey().equals(newDraftVersion.getSchemaContainerVersion().getSchema().getSegmentField())) {
				newDraftVersion.updateWebrootPathInfo(release.getUuid(), "node_conflicting_segmentfield_upload");
			}

			return batch.store(node, release.getUuid(), DRAFT, false).processAsync().andThen(node.transformToRest(ac, 0));
		}).subscribe(model -> ac.send(model, CREATED), ac::fail);
	}

	/**
	 * Handle image transformation.
	 * 
	 * @param rc
	 *            routing context
	 */
	public void handleTransformImage(RoutingContext rc, String uuid, String fieldName) {
		validateParameter(uuid, "uuid");
		validateParameter(fieldName, "fieldName");
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
		BinaryFieldTransformRequest transformation = JsonUtil.readValue(ac.getBodyAsString(), BinaryFieldTransformRequest.class);
		if (isEmpty(transformation.getLanguage())) {
			throw error(BAD_REQUEST, "image_error_language_not_set");
		}

		db.operateTx(() -> {
			// Load needed elements
			Project project = ac.getProject();
			Node node = project.getNodeRoot().loadObjectByUuid(ac, uuid, UPDATE_PERM);

			Language language = boot.get().languageRoot().findByLanguageTag(transformation.getLanguage());
			if (language == null) {
				throw error(NOT_FOUND, "error_language_not_found", transformation.getLanguage());
			}

			NodeGraphFieldContainer latestDraftVersion = node.getLatestDraftFieldContainer(language);
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

			if (!initialField.hasImage()) {
				throw error(BAD_REQUEST, "error_transformation_non_image", fieldName);
			}

			try {
				// Prepare the imageManipulationParameter using the transformation request as source
				ImageManipulationParameters imageManipulationParameter = new ImageManipulationParametersImpl().setWidth(transformation.getWidth())
						.setHeight(transformation.getHeight()).setStartx(transformation.getCropx()).setStarty(transformation.getCropy())
						.setCropw(transformation.getCropw()).setCroph(transformation.getCroph());
				if (!imageManipulationParameter.isSet()) {
					throw error(BAD_REQUEST, "error_no_image_transformation", fieldName);
				}

				// Update the binary field with the new information
				SearchQueueBatch sqb = db.tx(() -> {
					SearchQueueBatch batch = searchQueue.create();
					Release release = ac.getRelease();

					// Create a new node version field container to store the upload
					NodeGraphFieldContainer newDraftVersion = node.createGraphFieldContainer(language, release, ac.getUser(), latestDraftVersion, true);
					BinaryGraphField field = newDraftVersion.createBinary(fieldName);
					String fieldUuid = field.getUuid();
					String fieldSegmentedPath = field.getSegmentedPath();
					String fieldPath = field.getFilePath();

					// 1. Resize the original image and store the result in the filesystem
					Single<TransformationResult> obsTransformation = imageManipulator
							.handleResize(initialField.getFile(), field.getSHA512Sum(), imageManipulationParameter)
							.flatMap(file -> {
								// 2. Hash the resized image data and store it using the computed fieldUuid + hash
								return readEntireFile(file.getFile()).map(buffer -> hashAndStoreBinaryFile(buffer, fieldUuid, fieldSegmentedPath))
								.flatMap(hash -> {
									// 3. The image was stored and hashed. Now we need to load the stored file again and check the image properties
									return imageManipulator.readImageInfo(() -> {
										try {
											return new FileInputStream(fieldPath);
										} catch (IOException e) {
											throw new RuntimeException(e);
										}
									}).map(info -> {
										// Return a POJO which hold all information that is needed to update the field
										return new TransformationResult(hash, file.getProps().size(), info);
									});
								});
							});

					TransformationResult result = obsTransformation.toBlocking().value();

					field.setSHA512Sum(result.getHash());
					field.setFileSize(result.getSize());
					// The resized image will always be a JPEG
					field.setMimeType("image/jpeg");
					// TODO should we rename the image, if the extension is wrong?
					field.setImageHeight(result.getImageInfo().getHeight());
					field.setImageWidth(result.getImageInfo().getWidth());
					batch.store(newDraftVersion, node.getProject().getReleaseRoot().getLatestRelease().getUuid(), DRAFT, false);
					return batch;
				});
				// Finally update the search index and return the updated node
				return sqb.processAsync().andThen(node.transformToRest(ac, 0));
			} catch (GenericRestException e) {
				throw e;
			} catch (Exception e) {
				log.error("Error while transforming image", e);
				throw error(INTERNAL_SERVER_ERROR, "error_internal");
			}
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	/**
	 * Hash the file upload data and move the temporary uploaded file to its final destination.
	 * 
	 * @param ac Action context
	 * @param uuid
	 * @param segmentedPath
	 * @return calculated SHA 512 sum
	 */
	public void moveBinaryFile(InternalActionContext ac, String uuid, String segmentedPath) {
		MeshUploadOptions uploadOptions = Mesh.mesh().getOptions().getUploadOptions();
		File uploadFolder = new File(uploadOptions.getDirectory(), segmentedPath);
		File targetFile = new File(uploadFolder, uuid + ".bin");
		String targetPath = targetFile.getAbsolutePath();

		checkUploadFolderExists(uploadFolder);
		deletePotentialUpload(targetPath);
		moveUploadIntoPlace(ac.get("sourceFile"), targetPath);
		// Since this function can be called multiple times (if a transaction fails), we have to
		// update the path so that moving the file works again.
		ac.put("sourceFile", targetPath);
	}

	/**
	 * Hash the data from the buffer and store it to its final destination.
	 * 
	 * @param buffer
	 *            buffer which will be handled
	 * @param uuid
	 *            uuid of the binary field
	 * @param segmentedPath
	 *            path to store the binary data
	 * @return The sha512 checksum
	 */
	public String hashAndStoreBinaryFile(Buffer buffer, String uuid, String segmentedPath) {
		MeshUploadOptions uploadOptions = Mesh.mesh().getOptions().getUploadOptions();
		File uploadFolder = new File(uploadOptions.getDirectory(), segmentedPath);
		File targetFile = new File(uploadFolder, uuid + ".bin");
		String targetPath = targetFile.getAbsolutePath();

		String sha512sum = hashBuffer(buffer);
		checkUploadFolderExists(uploadFolder);
		deletePotentialUpload(targetPath);
		storeBuffer(buffer, targetPath);
		return sha512sum;
	}

	/**
	 * Hash the given buffer and return a sha512 checksum.
	 * 
	 * @param buffer
	 *            buffer
	 * @return sha512 checksum
	 */
	protected String hashBuffer(Buffer buffer) {
		return FileUtils.generateSha512Sum(buffer);
	}

	/**
	 * Delete potential existing file uploads from the given path.
	 * 
	 * @param targetPath
	 */
	protected void deletePotentialUpload(String targetPath) {
		FileSystem fileSystem = Mesh.vertx().fileSystem();
		if (fileSystem.existsBlocking(targetPath)) {
			// Deleting of existing binary file
			fileSystem.deleteBlocking(targetPath);
		}
		// log.error("Error while attempting to delete target file {" + targetPath + "}", error);
		// log.error("Unable to check existence of file at location {" + targetPath + "}");

	}

	/**
	 * Move the file upload from the temporary upload directory to the given target path.
	 * 
	 * @param fileUpload
	 * @param targetPath
	 */
	protected void moveUploadIntoPlace(String fileUpload, String targetPath) {
		FileSystem fileSystem = Mesh.vertx().fileSystem();
		fileSystem.moveBlocking(fileUpload, targetPath);
		if (log.isDebugEnabled()) {
			log.debug("Moved upload file from {" + fileUpload + "} to {" + targetPath + "}");
		}
		// log.error("Failed to move upload file from {" + fileUpload.uploadedFileName() + "} to {" + targetPath + "}", error);
	}

	/**
	 * Store the data in the buffer into the given place.
	 * 
	 * @param buffer
	 *            buffer
	 * @param targetPath
	 *            target path
	 */
	protected void storeBuffer(Buffer buffer, String targetPath) {
		FileSystem fileSystem = Mesh.vertx().fileSystem();
		fileSystem.writeFileBlocking(targetPath, buffer);
		// log.error("Failed to save file to {" + targetPath + "}", error);
		// throw error(INTERNAL_SERVER_ERROR, "node_error_upload_failed", error);
	}

	/**
	 * Check the target upload folder and create it if needed.
	 * 
	 * @param uploadFolder
	 */
	protected void checkUploadFolderExists(File uploadFolder) {

		boolean folderExists = uploadFolder.exists();
		// log.error("Could not check whether target directory {" + uploadFolder.getAbsolutePath() + "} exists.", error);
		// throw error(BAD_REQUEST, "node_error_upload_failed", error);

		if (!folderExists) {
			uploadFolder.mkdirs();

			// log.error("Failed to create target folder {" + uploadFolder.getAbsolutePath() + "}", error);
			// throw error(BAD_REQUEST, "node_error_upload_failed", error);

			if (log.isDebugEnabled()) {
				log.debug("Created folder {" + uploadFolder.getAbsolutePath() + "}");
			}
		}

	}

}
