package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
import static com.gentics.mesh.core.rest.common.GenericMessageResponse.message;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.verticle.handler.HandlerUtilities.operateNoTx;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.io.File;
import java.io.FileInputStream;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.elasticsearch.common.collect.Tuple;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.image.spi.ImageInfo;
import com.gentics.mesh.core.image.spi.ImageManipulator;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.node.field.BinaryFieldTransformRequest;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.etc.config.MeshUploadOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.ImageManipulationParameters;
import com.gentics.mesh.util.FileUtils;
import com.gentics.mesh.util.RxUtil;

import dagger.Lazy;
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
public class NodeFieldAPIHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(NodeFieldAPIHandler.class);

	private ImageManipulator imageManipulator;

	private Database db;

	private Lazy<BootstrapInitializer> boot;

	@Inject
	public NodeFieldAPIHandler(ImageManipulator imageManipulator, Database db, Lazy<BootstrapInitializer> boot) {
		this.imageManipulator = imageManipulator;
		this.db = db;
		this.boot = boot;
	}

	public void handleReadField(RoutingContext rc, String uuid, String languageTag, String fieldName) {
		InternalActionContext ac = InternalActionContext.create(rc);
		operateNoTx(() -> {
			Project project = ac.getProject();
			Node node = project.getNodeRoot().loadObjectByUuid(ac, uuid, READ_PERM);
			Language language = boot.get().languageRoot().findByLanguageTag(languageTag);
			if (language == null) {
				throw error(NOT_FOUND, "error_language_not_found", languageTag);
			}

			NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(language);
			if (container == null) {
				throw error(NOT_FOUND, "error_language_not_found", languageTag);
			}

			BinaryGraphField binaryField = container.getBinary(fieldName);
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
	 * @param languageTag
	 *            Language tag of the node language which should be modified
	 * @param fieldName
	 *            Name of the field which should be created
	 */
	public void handleCreateField(InternalActionContext ac, String uuid, String languageTag, String fieldName) {
		validateParameter(uuid, "uuid");
		validateParameter(languageTag, "languageTag");
		validateParameter(fieldName, "fieldName");
		operateNoTx(() -> {
			Project project = ac.getProject();
			Release release = ac.getRelease(null);
			Node node = project.getNodeRoot().loadObjectByUuid(ac, uuid, UPDATE_PERM);
			Language language = boot.get().languageRoot().findByLanguageTag(languageTag);
			if (language == null) {
				throw error(NOT_FOUND, "error_language_not_found", languageTag);
			}

			// Create new field container as clone of the existing
			NodeGraphFieldContainer latestDraftVersion = node.getGraphFieldContainer(language, release, ContainerType.DRAFT);

			if (latestDraftVersion == null) {
				// Create a new field container
				// latestDraftVersion = node.createGraphFieldContainer(language, release, ac.getUser());
				throw error(NOT_FOUND, "error_language_not_found", languageTag);
			}

			Optional<FieldSchema> fieldSchema = latestDraftVersion.getSchemaContainerVersion().getSchema().getFieldSchema(fieldName);
			if (!fieldSchema.isPresent()) {
				throw error(BAD_REQUEST, "error_schema_definition_not_found", fieldName);
			}
			if (!(fieldSchema.get() instanceof BinaryFieldSchema)) {
				// TODO Add support for other field types
				throw error(BAD_REQUEST, "error_found_field_is_not_binary", fieldName);
			}

			NodeGraphFieldContainer newDraftVersion = node.createGraphFieldContainer(language, release, ac.getUser(), latestDraftVersion);
			BinaryGraphField field = newDraftVersion.createBinary(fieldName);

			MeshUploadOptions uploadOptions = Mesh.mesh().getOptions().getUploadOptions();
			Set<FileUpload> fileUploads = ac.getFileUploads();
			if (fileUploads.isEmpty()) {
				throw error(BAD_REQUEST, "node_error_no_binarydata_found");
			}
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
			String fieldUuid = field.getUuid();
			Single<ImageInfo> obsImage;

			// Only gather image info for actual images. Otherwise return an empty image info object.
			if (contentType.startsWith("image/")) {
				try {
					obsImage = imageManipulator.readImageInfo(new FileInputStream(ul.uploadedFileName()));
				} catch (Exception e) {
					log.error("Could not load schema for node {" + node.getUuid() + "}");
					throw error(INTERNAL_SERVER_ERROR, "could not find upload file", e);
				}

			} else {
				obsImage = Single.just(new ImageInfo());
			}

			Single<String> obsHash = hashAndMoveBinaryFile(ul, fieldUuid, field.getSegmentedPath());
			return Single.zip(obsImage, obsHash, (imageInfo, sha512sum) -> {
				SearchQueueBatch batch = db.tx(() -> {
					SearchQueue queue = MeshInternal.get().boot().meshRoot().getSearchQueue();
					SearchQueueBatch sqb = queue.createBatch();

					field.setFileName(fileName);
					field.setFileSize(ul.size());
					field.setMimeType(contentType);
					field.setSHA512Sum(sha512sum);
					field.setImageDominantColor(imageInfo.getDominantColor());
					field.setImageHeight(imageInfo.getHeight());
					field.setImageWidth(imageInfo.getWidth());

					// if the binary field is the segment field, we need to update the webroot info in the node
					if (field.getFieldKey().equals(newDraftVersion.getSchemaContainerVersion().getSchema().getSegmentField())) {
						newDraftVersion.updateWebrootPathInfo(release.getUuid(), "node_conflicting_segmentfield_upload");
					}

					return node.addIndexBatchEntry(sqb, STORE_ACTION);
				});

				return batch.processAsync().andThen(Single.just(message(ac, "node_binary_field_updated", fieldName)));

			}).flatMap(x -> x);
		}).subscribe(model -> ac.send(model, CREATED), ac::fail);

	}

	/**
	 * Update a specific node field.
	 * 
	 * @param ac
	 * @param uuid
	 *            Node uuid
	 * @param languageTag
	 *            Language which should be handled
	 * @param fieldName
	 *            Field which should be updated
	 */
	public void handleUpdateField(InternalActionContext ac, String uuid, String languageTag, String fieldName) {
		handleCreateField(ac, uuid, languageTag, fieldName);
	}

	/**
	 * Remove the field with the given name from the node language.
	 * 
	 * @param ac
	 * @param uuid
	 * @param languageTag
	 * @param fieldName
	 */
	public void handleRemoveField(InternalActionContext ac, String uuid, String languageTag, String fieldName) {
		validateParameter(uuid, "uuid");
		validateParameter(languageTag, "languageTag");
		validateParameter(fieldName, "fieldName");
		operateNoTx(() -> {
			Project project = ac.getProject();
			Node node = project.getNodeRoot().loadObjectByUuid(ac, uuid, UPDATE_PERM);
			// TODO Update SQB
			return Single.just(new GenericMessageResponse("Not yet implemented"));
		}).subscribe(model -> ac.send(NO_CONTENT), ac::fail);
	}

	public void handleRemoveFieldItem(InternalActionContext ac, String uuid, String languageTag, String fieldName) {
		operateNoTx(() -> {
			Project project = ac.getProject();
			Node node = project.getNodeRoot().loadObjectByUuid(ac, uuid, UPDATE_PERM);
			// TODO Update SQB
			return Single.just(new GenericMessageResponse("Not yet implemented"));
		}).subscribe(model -> ac.send(NO_CONTENT), ac::fail);
	}

	public void handleUpdateFieldItem(InternalActionContext ac, String uuid, String languageTag, String fieldName) {
		operateNoTx(() -> {
			Project project = ac.getProject();
			Node node = project.getNodeRoot().loadObjectByUuid(ac, uuid, UPDATE_PERM);
			// TODO Update SQB
			return Single.just(new GenericMessageResponse("Not yet implemented"));
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	public void handleReadFieldItem(InternalActionContext ac, String uuid, String languageTag, String fieldName) {
		operateNoTx(() -> {
			Project project = ac.getProject();
			Node node = project.getNodeRoot().loadObjectByUuid(ac, uuid, READ_PERM);
			return Single.just(new GenericMessageResponse("Not yet implemented"));
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	public void handleMoveFieldItem(InternalActionContext ac, String uuid, String languageTag, String fieldName) {
		operateNoTx(() -> {
			Project project = ac.getProject();
			Node node = project.getNodeRoot().loadObjectByUuid(ac, uuid, UPDATE_PERM);
			// TODO Update SQB
			return Single.just(new GenericMessageResponse("Not yet implemented"));
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	/**
	 * Handle image transformation.
	 * 
	 * @param rc
	 *            routing context
	 */
	public void handleTransformImage(RoutingContext rc, String uuid, String languageTag, String fieldName) {
		InternalActionContext ac = InternalActionContext.create(rc);
		operateNoTx(() -> {
			Project project = ac.getProject();
			Node node = project.getNodeRoot().loadObjectByUuid(ac, uuid, UPDATE_PERM);
			// TODO Update SQB
			Language language = boot.get().languageRoot().findByLanguageTag(languageTag);
			if (language == null) {
				throw error(NOT_FOUND, "error_language_not_found", languageTag);
			}
			NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(language);
			if (container == null) {
				throw error(NOT_FOUND, "error_language_not_found", languageTag);
			}

			Optional<FieldSchema> fieldSchema = container.getSchemaContainerVersion().getSchema().getFieldSchema(fieldName);
			if (!fieldSchema.isPresent()) {
				throw error(BAD_REQUEST, "error_schema_definition_not_found", fieldName);
			}
			if (!(fieldSchema.get() instanceof BinaryFieldSchema)) {
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
				BinaryFieldTransformRequest transformation = JsonUtil.readValue(ac.getBodyAsString(), BinaryFieldTransformRequest.class);
				ImageManipulationParameters imageManipulationParameter = new ImageManipulationParameters().setWidth(transformation.getWidth())
						.setHeight(transformation.getHeight()).setStartx(transformation.getCropx()).setStarty(transformation.getCropy())
						.setCropw(transformation.getCropw()).setCroph(transformation.getCroph());
				if (!imageManipulationParameter.isSet()) {
					throw error(BAD_REQUEST, "error_no_image_transformation", fieldName);
				}
				String fieldUuid = field.getUuid();
				String fieldSegmentedPath = field.getSegmentedPath();

				Single<Tuple<String, Integer>> obsHashAndSize = imageManipulator
						.handleResize(field.getFile(), field.getSHA512Sum(), imageManipulationParameter).flatMap(buffer -> {
							return hashAndStoreBinaryFile(buffer, fieldUuid, fieldSegmentedPath).map(hash -> {
								return Tuple.tuple(hash, buffer.length());
							});
						});

				return obsHashAndSize.flatMap(hashAndSize -> {
					Tuple<SearchQueueBatch, String> tuple = db.tx(() -> {
						SearchQueue queue = MeshInternal.get().boot().meshRoot().getSearchQueue();
						SearchQueueBatch batch = queue.createBatch();

						field.setSHA512Sum(hashAndSize.v1());
						field.setFileSize(hashAndSize.v2());
						// resized images will always be jpeg
						field.setMimeType("image/jpeg");

						// TODO should we rename the image, if the extension is wrong?

						// TODO handle image properties as well
						// node.setBinaryImageDPI(dpi);
						// node.setBinaryImageHeight(heigth);
						// node.setBinaryImageWidth(width);
						node.addIndexBatchEntry(batch, STORE_ACTION);
						return Tuple.tuple(batch, node.getUuid());
					});

					SearchQueueBatch batch = tuple.v1();
					String updatedNodeUuid = tuple.v2();
					return batch.processAsync().toSingleDefault(message(ac, "node_binary_field_updated", updatedNodeUuid));
				});
			} catch (GenericRestException e) {
				throw e;
			} catch (Exception e) {
				log.error("Error while transforming image", e);
				throw error(INTERNAL_SERVER_ERROR, "error_internal");
			}
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	// // TODO abstract rc away
	// public void handleDownload(RoutingContext rc) {
	// InternalActionContext ac = InternalActionContext.create(rc);
	// BinaryFieldResponseHandler binaryHandler = new BinaryFieldResponseHandler(rc, imageManipulator);
	// db.asyncNoTrx(() -> {
	// Project project = ac.getProject();
	// return project.getNodeRoot().loadObject(ac, "uuid", READ_PERM).map(node-> {
	// db.noTrx(()-> {
	// Node node = rh.result();
	// binaryHandler.handle(node);
	// });
	// });
	// }).subscribe(binaryField -> {
	// }, ac::fail);
	// }

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
		Completable obsDeleteExisting = fileSystem.deleteObservable(targetPath).toCompletable().doOnError(error -> {
			log.error("Error while attempting to delete target file {" + targetPath + "}", error);
		});

		Single<Boolean> obsUploadExistsCheck = fileSystem.existsObservable(targetPath).toSingle().doOnError(error -> {
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
		return fileSystem.moveObservable(fileUpload.uploadedFileName(), targetPath).toCompletable().doOnError(error -> {
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
		return fileSystem.writeFileObservable(targetPath, buffer).toCompletable().doOnError(error -> {
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
		Single<Boolean> obs = fileSystem.existsObservable(uploadFolder.getAbsolutePath()).doOnError(error -> {
			log.error("Could not check whether target directory {" + uploadFolder.getAbsolutePath() + "} exists.", error);
			throw error(BAD_REQUEST, "node_error_upload_failed", error);
		}).toSingle();

		return RxUtil.andThenCompletable(obs, folderExists -> {
			if (!folderExists) {
				return fileSystem.mkdirsObservable(uploadFolder.getAbsolutePath()).toCompletable().doOnError(error -> {
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
