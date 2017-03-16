package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
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
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.util.FileUtils;
import com.gentics.mesh.util.RxUtil;

import dagger.Lazy;
import io.vertx.core.MultiMap;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.file.FileSystem;
import rx.Completable;
import rx.Single;

/**
 * Handler which contains field API specific request handlers.
 */
public class BinaryFieldHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(BinaryFieldHandler.class);

	private ImageManipulator imageManipulator;

	private Database db;

	private Lazy<BootstrapInitializer> boot;

	private SearchQueue searchQueue;

	@Inject
	public BinaryFieldHandler(ImageManipulator imageManipulator, Database db, Lazy<BootstrapInitializer> boot, SearchQueue searchQueue) {
		this.imageManipulator = imageManipulator;
		this.db = db;
		this.boot = boot;
		this.searchQueue = searchQueue;
	}

	public void handleReadBinaryField(RoutingContext rc, String uuid, String fieldName) {
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
		db.operateNoTx(() -> {
			Project project = ac.getProject();
			Node node = project.getNodeRoot().loadObjectByUuid(ac, uuid, READ_PERM);
			//			Language language = boot.get().languageRoot().findByLanguageTag(languageTag);
			//			if (language == null) {
			//				throw error(NOT_FOUND, "error_language_not_found", languageTag);
			//			}

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
			db.noTx(() -> {
				BinaryFieldResponseHandler handler = new BinaryFieldResponseHandler(rc, imageManipulator);
				handler.handle(binaryField);
				return null;
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

		db.operateNoTx(() -> {
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
				//latestDraftVersion = node.createGraphFieldContainer(language, release, ac.getUser());
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
			return db.tx(() -> {
				// Create a new node version field container to store the upload
				NodeGraphFieldContainer newDraftVersion = node.createGraphFieldContainer(language, release, ac.getUser(), latestDraftVersion);
				BinaryGraphField field = newDraftVersion.createBinary(fieldName);
				String fieldUuid = field.getUuid();

				Single<ImageInfo> obsImage;

				// 3. Only gather image info for actual images. Otherwise return an empty image info object.
				if (contentType.startsWith("image/")) {
					try {
						obsImage = imageManipulator.readImageInfo(() -> {
							try {
								return new FileInputStream(ul.uploadedFileName());
							} catch (Exception e) {
								log.error("Could not load schema for node {" + node.getUuid() + "}");
								throw error(INTERNAL_SERVER_ERROR, "could not find upload file", e);
							}
						});
					} catch (Exception e) {
						log.error("Could not load schema for node {" + node.getUuid() + "}");
						throw error(INTERNAL_SERVER_ERROR, "could not find upload file", e);
					}

				} else {
					obsImage = Single.just(new ImageInfo());
				}

				// 4. Hash and store the file and update the field properties
				Single<String> obsHash = hashAndMoveBinaryFile(ul, fieldUuid, field.getSegmentedPath());
				Single<TransformationResult> resultObs = Single.zip(obsImage, obsHash, (imageInfo, sha512sum) -> {
					return new TransformationResult(sha512sum, 0, imageInfo);
				});

				TransformationResult info = resultObs.toBlocking().value();
				field.setFileName(fileName);
				field.setFileSize(ul.size());
				field.setMimeType(contentType);
				field.setSHA512Sum(info.getHash());
				field.setImageDominantColor(info.getImageInfo().getDominantColor());
				field.setImageHeight(info.getImageInfo().getHeight());
				field.setImageWidth(info.getImageInfo().getWidth());

				// If the binary field is the segment field, we need to  update the webroot info in the node
				if (field.getFieldKey().equals(newDraftVersion.getSchemaContainerVersion().getSchema().getSegmentField())) {
					newDraftVersion.updateWebrootPathInfo(release.getUuid(), "node_conflicting_segmentfield_upload");
				}

				return batch.store(node, release.getUuid(), DRAFT, false);
			}).processAsync().andThen(node.transformToRest(ac, 0));
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

		db.operateNoTx(() -> {
			// 1. Load needed elements
			Project project = ac.getProject();
			Node node = project.getNodeRoot().loadObjectByUuid(ac, uuid, UPDATE_PERM);

			Language language = boot.get().languageRoot().findByLanguageTag(transformation.getLanguage());
			if (language == null) {
				throw error(NOT_FOUND, "error_language_not_found", transformation.getLanguage());
			}

			NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(language);
			if (container == null) {
				throw error(NOT_FOUND, "error_language_not_found", language.getLanguageTag());
			}

			FieldSchema fieldSchema = container.getSchemaContainerVersion().getSchema().getField(fieldName);
			if (fieldSchema == null) {
				throw error(BAD_REQUEST, "error_schema_definition_not_found", fieldName);
			}
			if (!(fieldSchema instanceof BinaryFieldSchema)) {
				throw error(BAD_REQUEST, "error_found_field_is_not_binary", fieldName);
			}

			BinaryGraphField field = container.getBinary(fieldName);
			if (field == null) {
				throw error(NOT_FOUND, "error_binaryfield_not_found_with_name", fieldName);
			}

			if (!field.hasImage()) {
				throw error(BAD_REQUEST, "error_transformation_non_image", fieldName);
			}

			try {
				// Prepare the imageManipulationParameter using the transformation request as source
				ImageManipulationParametersImpl imageManipulationParameter = new ImageManipulationParametersImpl().setWidth(transformation.getWidth())
						.setHeight(transformation.getHeight()).setStartx(transformation.getCropx()).setStarty(transformation.getCropy())
						.setCropw(transformation.getCropw()).setCroph(transformation.getCroph());
				if (!imageManipulationParameter.isSet()) {
					throw error(BAD_REQUEST, "error_no_image_transformation", fieldName);
				}
				String fieldUuid = field.getUuid();
				String fieldSegmentedPath = field.getSegmentedPath();

				// Resize the image and store the result in the filesystem
				Single<TransformationResult> obsTransformation = imageManipulator
						.handleResize(field.getFile(), field.getSHA512Sum(), imageManipulationParameter).flatMap(buffer -> {
							return hashAndStoreBinaryFile(buffer, fieldUuid, fieldSegmentedPath).flatMap(hash -> {
								// The image was stored and hashed. Now we need to load the stored file again and check the image properties
								return db.noTx(() -> {
									String fieldPath = field.getFilePath();
									return imageManipulator.readImageInfo(() -> {
										try {
											return new FileInputStream(fieldPath);
										} catch (IOException e) {
											throw new RuntimeException(e);
										}
									}).map(info -> {
										// Return a POJO which hold all information that is needed to update the field
										return new TransformationResult(hash, buffer.length(), info);
									});
								});
							});
						});

				return obsTransformation.flatMap(info -> {

					// Update the binary field with the new information
					SearchQueueBatch batch = searchQueue.create();
					Node updatedNodeUuid = db.tx(() -> {

						field.setSHA512Sum(info.getHash());
						field.setFileSize(info.getSize());
						// The resized image will always be a jpeg
						field.setMimeType("image/jpeg");
						// TODO should we rename the image, if the extension is wrong?
						field.setImageHeight(info.getImageInfo().getHeight());
						field.setImageWidth(info.getImageInfo().getWidth());
						batch.store(container, node.getProject().getReleaseRoot().getLatestRelease().getUuid(), DRAFT, false);
						return node;
					});
					// Finally update the search index and return the updated node
					return batch.processAsync().andThen(updatedNodeUuid.transformToRest(ac, 0));
				});
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
	 * @param fileUpload
	 *            Upload which will be handled
	 * @param uuid
	 * @param segmentedPath
	 * @return
	 */
	protected Single<String> hashAndMoveBinaryFile(FileUpload fileUpload, String uuid, String segmentedPath) {
		MeshUploadOptions uploadOptions = Mesh.mesh().getOptions().getUploadOptions();
		File uploadFolder = new File(uploadOptions.getDirectory(), segmentedPath);
		File targetFile = new File(uploadFolder, uuid + ".bin");
		String targetPath = targetFile.getAbsolutePath();

		return hashFileupload(fileUpload).flatMap(sha512sum -> {
			checkUploadFolderExists(uploadFolder).await();
			deletePotentialUpload(targetPath).await();
			return moveUploadIntoPlace(fileUpload, targetPath).toSingleDefault(sha512sum);
		}).doOnError(error -> {
			log.error("Failed to handle upload file from {" + fileUpload.uploadedFileName() + "} / {" + targetPath + "}", error);
			throw error(INTERNAL_SERVER_ERROR, "node_error_upload_failed", error);
		});

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
	 * @return single emitting the sha512 checksum
	 */
	public Single<String> hashAndStoreBinaryFile(Buffer buffer, String uuid, String segmentedPath) {
		MeshUploadOptions uploadOptions = Mesh.mesh().getOptions().getUploadOptions();
		File uploadFolder = new File(uploadOptions.getDirectory(), segmentedPath);
		File targetFile = new File(uploadFolder, uuid + ".bin");
		String targetPath = targetFile.getAbsolutePath();

		return hashBuffer(buffer).flatMap(sha512sum -> {
			checkUploadFolderExists(uploadFolder).await();
			deletePotentialUpload(targetPath).await();
			return storeBuffer(buffer, targetPath).toSingleDefault(sha512sum);
		});
	}

	/**
	 * Hash the given fileupload and return a sha512 checksum.
	 * 
	 * @param fileUpload
	 * @return
	 */
	protected Single<String> hashFileupload(FileUpload fileUpload) {
		return FileUtils.generateSha512Sum(fileUpload.uploadedFileName()).doOnError(error -> {
			log.error("Error while hashing fileupload {" + fileUpload.uploadedFileName() + "}", error);
		});
	}

	/**
	 * Hash the given buffer and return a sha512 checksum.
	 * 
	 * @param buffer
	 *            buffer
	 * @return single emitting the sha512 checksum
	 */
	protected Single<String> hashBuffer(Buffer buffer) {
		Single<String> obsHash = FileUtils.generateSha512Sum(buffer).doOnError(error -> {
			log.error("Error while hashing data", error);
			throw error(INTERNAL_SERVER_ERROR, "node_error_upload_failed", error);
		});
		return obsHash;
	}

	/**
	 * Delete potential existing file uploads from the given path.
	 * 
	 * @param targetPath
	 * @return
	 */
	protected Completable deletePotentialUpload(String targetPath) {
		Vertx rxVertx = Vertx.newInstance(Mesh.vertx());
		FileSystem fileSystem = rxVertx.fileSystem();
		// Deleting of existing binary file
		Completable obsDeleteExisting = fileSystem.rxDelete(targetPath).toCompletable().doOnError(error -> {
			log.error("Error while attempting to delete target file {" + targetPath + "}", error);
		});

		Single<Boolean> obsUploadExistsCheck = fileSystem.rxExists(targetPath).doOnError(error -> {
			log.error("Unable to check existence of file at location {" + targetPath + "}");
		});

		return RxUtil.andThenCompletable(obsUploadExistsCheck, uploadAlreadyExists -> {
			if (uploadAlreadyExists) {
				return obsDeleteExisting;
			}
			return Completable.complete();
		});
	}

	/**
	 * Move the fileupload from the temporary upload directory to the given target path.
	 * 
	 * @param fileUpload
	 * @param targetPath
	 * @return
	 */
	protected Completable moveUploadIntoPlace(FileUpload fileUpload, String targetPath) {
		Vertx rxVertx = Vertx.newInstance(Mesh.vertx());
		FileSystem fileSystem = rxVertx.fileSystem();
		return fileSystem.rxMove(fileUpload.uploadedFileName(), targetPath).toCompletable().doOnError(error -> {
			log.error("Failed to move upload file from {" + fileUpload.uploadedFileName() + "} to {" + targetPath + "}", error);
		}).doOnCompleted(() -> {
			if (log.isDebugEnabled()) {
				log.debug("Moved upload file from {" + fileUpload.uploadedFileName() + "} to {" + targetPath + "}");
			}
		});
	}

	/**
	 * Store the data in the buffer into the given place.
	 * 
	 * @param buffer
	 *            buffer
	 * @param targetPath
	 *            target path
	 * @return empty observable
	 */
	protected Completable storeBuffer(Buffer buffer, String targetPath) {
		Vertx rxVertx = Vertx.newInstance(Mesh.vertx());
		FileSystem fileSystem = rxVertx.fileSystem();
		return fileSystem.rxWriteFile(targetPath, buffer).toCompletable().doOnError(error -> {
			log.error("Failed to save file to {" + targetPath + "}", error);
			throw error(INTERNAL_SERVER_ERROR, "node_error_upload_failed", error);
		});
	}

	/**
	 * Check the target upload folder and create it if needed.
	 * 
	 * @param uploadFolder
	 * @return
	 */
	protected Completable checkUploadFolderExists(File uploadFolder) {
		Vertx rxVertx = Vertx.newInstance(Mesh.vertx());
		FileSystem fileSystem = rxVertx.fileSystem();
		Single<Boolean> obs = fileSystem.rxExists(uploadFolder.getAbsolutePath()).doOnError(error -> {
			log.error("Could not check whether target directory {" + uploadFolder.getAbsolutePath() + "} exists.", error);
			throw error(BAD_REQUEST, "node_error_upload_failed", error);
		});

		return RxUtil.andThenCompletable(obs, folderExists -> {
			if (!folderExists) {
				return fileSystem.rxMkdirs(uploadFolder.getAbsolutePath()).toCompletable().doOnError(error -> {
					log.error("Failed to create target folder {" + uploadFolder.getAbsolutePath() + "}", error);
					throw error(BAD_REQUEST, "node_error_upload_failed", error);
				}).doOnCompleted(() -> {
					if (log.isDebugEnabled()) {
						log.debug("Created folder {" + uploadFolder.getAbsolutePath() + "}");
					}
				});
			} else {
				return Completable.complete();
			}
		});

	}

}
