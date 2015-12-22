package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.common.GenericMessageResponse.message;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.io.File;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.elasticsearch.common.collect.Tuple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.image.spi.ImageManipulator;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.etc.config.MeshUploadOptions;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.util.FileUtils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.file.FileSystem;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import rx.Observable;

@Component
public class NodeFieldAPIHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(NodeFieldAPIHandler.class);

	@Autowired
	private ImageManipulator imageManipulator;

	public void handleReadField(RoutingContext rc) {
		InternalActionContext ac = InternalActionContext.create(rc);
		db.asyncNoTrx(() -> {
			Project project = ac.getProject();
			String languageTag = ac.getParameter("languageTag");
			String fieldName = ac.getParameter("fieldName");
			return project.getNodeRoot().loadObject(ac, "uuid", READ_PERM).map(node -> {
				Language language = boot.languageRoot().findByLanguageTag(languageTag);
				if (language == null) {
					throw error(NOT_FOUND, "error_language_not_found", languageTag);
				}

				NodeGraphFieldContainer container = node.getGraphFieldContainer(language);
				if (container == null) {
					throw error(NOT_FOUND, "error_language_not_found", languageTag);
				}

				BinaryGraphField binaryField = container.getBinary(fieldName);
				if (binaryField == null) {
					throw error(NOT_FOUND, "Binary field for fieldname {" + fieldName + "} could not be found.");
				}
				return binaryField;
			}).toBlocking().first();
		}).subscribe(binaryField -> {
			BinaryFieldResponseHandler handler = new BinaryFieldResponseHandler(rc, imageManipulator);
			handler.handle(binaryField);
		} , ac::fail);
	}

	public void handleCreateField(RoutingContext rc) {

		InternalActionContext ac = InternalActionContext.create(rc);
		db.asyncNoTrx(() -> {
			Project project = ac.getProject();
			String languageTag = ac.getParameter("languageTag");
			String fieldName = ac.getParameter("fieldName");
			return project.getNodeRoot().loadObject(ac, "uuid", UPDATE_PERM).map(node -> {
				// TODO Update SQB
				Language language = boot.languageRoot().findByLanguageTag(languageTag);
				if (language == null) {
					throw error(NOT_FOUND, "error_language_not_found", languageTag);
				}
				NodeGraphFieldContainer container = node.getGraphFieldContainer(language);
				if (container == null) {
					throw error(NOT_FOUND, "error_language_not_found", languageTag);
				}

				FieldSchema fieldSchema = node.getSchema().getFieldSchema(fieldName);

				BinaryGraphField field = container.createBinary(fieldName);
				if (field == null) {
					// ac.fail(BAD_REQUEST, "Binary field {" + fieldName + "} could not be found.");
					// return;
				}

				MeshUploadOptions uploadOptions = Mesh.mesh().getOptions().getUploadOptions();
				try {
					Schema schema = node.getSchema();
					Set<FileUpload> fileUploads = rc.fileUploads();
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
							log.debug("Upload size of {" + ul.size() + "} exeeds limit of {" + byteLimit + "} by {" + (ul.size() - byteLimit)
									+ "} bytes.");
						}
						String humanReadableFileSize = org.apache.commons.io.FileUtils.byteCountToDisplaySize(ul.size());
						String humanReadableUploadLimit = org.apache.commons.io.FileUtils.byteCountToDisplaySize(byteLimit);
						throw error(BAD_REQUEST, "node_error_uploadlimit_reached", humanReadableFileSize, humanReadableUploadLimit);
					}
					String contentType = ul.contentType();
					String fileName = ul.fileName();
					String fieldUuid = field.getUuid();
					Observable<String> obsHash = hashAndMoveBinaryFile(ac, ul, fieldUuid, field.getSegmentedPath());
					return obsHash.flatMap(sha512sum -> {
						Tuple<SearchQueueBatch, String> tuple = db.trx(() -> {
							field.setFileName(fileName);
							field.setFileSize(ul.size());
							field.setMimeType(contentType);
							field.setSHA512Sum(sha512sum);
							//TODO handle image properties as well
							// node.setBinaryImageDPI(dpi);
							// node.setBinaryImageHeight(heigth);
							// node.setBinaryImageWidth(width);
							SearchQueueBatch batch = node.addIndexBatch(SearchQueueEntryAction.UPDATE_ACTION);
							return Tuple.tuple(batch, node.getUuid());
						});

						SearchQueueBatch batch = tuple.v1();
						String updatedNodeUuid = tuple.v2();
						return batch.process().map(done -> {
							return message(ac, "node_binary_field_updated", updatedNodeUuid);
						});
					});

				} catch (Exception e) {
					log.error("Could not load schema for node {" + node.getUuid() + "}");
					throw e;
				}
			}).flatMap(x->x).toBlocking().first();
		}).subscribe(model -> ac.respond(model, CREATED), ac::fail);

	}

	public void handleUpdateField(InternalActionContext ac) {
		db.asyncNoTrx(() -> {
			Project project = ac.getProject();
			return project.getNodeRoot().loadObject(ac, "uuid", UPDATE_PERM).map(node -> {
				// TODO Update SQB
				return new GenericMessageResponse("Not yet implemented");
			}).toBlocking().first();
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	public void handleRemoveField(InternalActionContext ac) {
		db.asyncNoTrx(() -> {
			Project project = ac.getProject();
			return project.getNodeRoot().loadObject(ac, "uuid", UPDATE_PERM).map(node -> {
				// TODO Update SQB
				return new GenericMessageResponse("Not yet implemented");
			}).toBlocking().first();
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	public void handleRemoveFieldItem(InternalActionContext ac) {
		db.asyncNoTrx(() -> {
			Project project = ac.getProject();
			return project.getNodeRoot().loadObject(ac, "uuid", UPDATE_PERM).map(node -> {
				// TODO Update SQB
				return new GenericMessageResponse("Not yet implemented");
			}).toBlocking().first();
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	public void handleUpdateFieldItem(InternalActionContext ac) {
		db.asyncNoTrx(() -> {
			Project project = ac.getProject();
			return project.getNodeRoot().loadObject(ac, "uuid", UPDATE_PERM).map(node -> {
				// TODO Update SQB
				return new GenericMessageResponse("Not yet implemented");
			}).toBlocking().first();
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	public void handleReadFieldItem(InternalActionContext ac) {
		db.asyncNoTrx(() -> {
			Project project = ac.getProject();
			return project.getNodeRoot().loadObject(ac, "uuid", READ_PERM).map(node -> {
				return new GenericMessageResponse("Not yet implemented");
			}).toBlocking().first();
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	public void handleMoveFieldItem(InternalActionContext ac) {
		db.asyncNoTrx(() -> {
			Project project = ac.getProject();
			return project.getNodeRoot().loadObject(ac, "uuid", UPDATE_PERM).map(node -> {
				// TODO Update SQB
				return new GenericMessageResponse("Not yet implemented");
			}).toBlocking().first();
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	// TODO abstract rc away
	public void handleDownload(RoutingContext rc) {
		InternalActionContext ac = InternalActionContext.create(rc);
		// NodeBinaryFieldAPIHandler binaryHandler = new NodeBinaryFieldAPIHandler(rc, imageManipulator);
		// db.asyncNoTrx(tc -> {
		// Project project = ac.getProject();
		// loadObject(ac, "uuid", READ_PERM, project.getNodeRoot(), rh -> {
		// db.noTrx(noTx -> {
		// if (hasSucceeded(ac, rh)) {
		// Node node = rh.result();
		// binaryHandler.handle(node);
		// }
		// });
		// });
		// } , ac.errorHandler());
	}

	/**
	 * 
	 * @param ac
	 * @param fileUpload
	 * @param uuid
	 * @param segmentedPath
	 * @return
	 */
	private Observable<String> hashAndMoveBinaryFile(ActionContext ac, FileUpload fileUpload, String uuid, String segmentedPath) {
		MeshUploadOptions uploadOptions = Mesh.mesh().getOptions().getUploadOptions();
		FileSystem fileSystem = Mesh.vertx().fileSystem();

		return Observable.create(sub -> {

			AtomicReference<String> hashSum = new AtomicReference<>();
			// Handler that is invoked when the fileupload folder was checked and created if missing.
			Handler<AsyncResult<File>> targetFolderChecked = tfc -> {
				if (tfc.succeeded()) {
					File targetFolder = tfc.result();
					File targetFile = new File(targetFolder, uuid + ".bin");
					String targetPath = targetFile.getAbsolutePath();
					if (log.isDebugEnabled()) {
						log.debug("Moving file from {" + fileUpload.uploadedFileName() + "} to {" + targetPath + "}");
					}
					fileSystem.exists(targetPath, eh -> {
						if (eh.failed()) {
							sub.onError(eh.cause());
						}
						if (eh.result()) {
							fileSystem.delete(targetPath, dh -> {
								if (dh.succeeded()) {
									fileSystem.move(fileUpload.uploadedFileName(), targetPath, mh -> {
										if (mh.succeeded()) {
											sub.onNext(hashSum.get());
											sub.onCompleted();
										} else {
											log.error("Failed to move file to {" + targetPath + "}", mh.cause());
											sub.onError(error(INTERNAL_SERVER_ERROR, "node_error_upload_failed", mh.cause()));
										}
									});
								} else {
									sub.onError(eh.cause());
								}
							});
						} else {
							fileSystem.move(fileUpload.uploadedFileName(), targetPath, mh -> {
								if (mh.succeeded()) {
									sub.onNext(hashSum.get());
									sub.onCompleted();
								} else {
									log.error("Failed to move file to {" + targetPath + "}", mh.cause());
									sub.onError(error(INTERNAL_SERVER_ERROR, "node_error_upload_failed", mh.cause()));
								}
							});
						}

					});

				} else {
					sub.onError(error(INTERNAL_SERVER_ERROR, "node_error_upload_failed", tfc.cause()));
				}
			};

			FileUtils.generateSha512Sum(fileUpload.uploadedFileName(), hash -> {
				if (hash.succeeded()) {
					hashSum.set(hash.result());
					File folder = new File(uploadOptions.getDirectory(), segmentedPath);
					if (log.isDebugEnabled()) {
						log.debug("Creating folder {" + folder.getAbsolutePath() + "}");
					}
					fileSystem.exists(folder.getAbsolutePath(), deh -> {
						if (deh.succeeded()) {
							if (!deh.result()) {
								fileSystem.mkdirs(folder.getAbsolutePath(), mkh -> {
									if (mkh.succeeded()) {
										targetFolderChecked.handle(Future.succeededFuture(folder));
									} else {
										log.error("Failed to create target folder {" + folder.getAbsolutePath() + "}", mkh.cause());
										sub.onError(error(BAD_REQUEST, "node_error_upload_failed"));
									}
								});
							} else {
								targetFolderChecked.handle(Future.succeededFuture(folder));
							}
						} else {
							log.error("Could not check whether target directory {" + folder.getAbsolutePath() + "} exists.", deh.cause());
							sub.onError(error(BAD_REQUEST, "node_error_upload_failed", deh.cause()));
						}
					});
				} else {
					sub.onError(error(BAD_REQUEST, "node_error_hashing_failed"));
				}
			});
		});

	}

}
