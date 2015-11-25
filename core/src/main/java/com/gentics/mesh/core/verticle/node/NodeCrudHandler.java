package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.UPDATE_ACTION;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.error;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.failedFuture;
import static com.gentics.mesh.util.VerticleHelper.createObject;
import static com.gentics.mesh.util.VerticleHelper.deleteObject;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.processOrFail;
import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.updateObject;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.io.File;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.elasticsearch.common.collect.Tuple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.image.spi.ImageManipulator;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.etc.config.MeshUploadOptions;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.handler.InternalHttpActionContext;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.FileUtils;
import com.gentics.mesh.util.VerticleHelper;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import rx.Observable;

@Component
public class NodeCrudHandler extends AbstractCrudHandler {

	private static final Logger log = LoggerFactory.getLogger(NodeCrudHandler.class);

	@Autowired
	ImageManipulator imageProvider;

	@Override
	public void handleCreate(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			createObject(ac, boot.meshRoot().getNodeRoot());
		} , ac.errorHandler());
	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			String uuid = ac.getParameter("uuid");
			Project project = ac.getProject();
			if (project.getBaseNode().getUuid().equals(uuid)) {
				ac.fail(METHOD_NOT_ALLOWED, "node_basenode_not_deletable");
			} else {
				deleteObject(ac, "uuid", "node_deleted", ac.getProject().getNodeRoot());
			}
		} , ac.errorHandler());
	}

	public void handelDeleteLanguage(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			Project project = ac.getProject();
			loadObject(ac, "uuid", DELETE_PERM, project.getNodeRoot(), rh -> {
				if (hasSucceeded(ac, rh)) {
					Node node = rh.result();
					String languageTag = ac.getParameter("languageTag");
					Language language = MeshRoot.getInstance().getLanguageRoot().findByLanguageTag(languageTag);
					if (language == null) {
						tc.fail(error(NOT_FOUND, "error_language_not_found", languageTag));
						return;
					}
					node.deleteLanguageContainer(ac, language, dh -> {
						if (dh.failed()) {
							tc.fail(dh.cause());
						} else {
							ac.sendMessage(OK, "node_deleted_language", node.getUuid(), languageTag);
						}
					});
				}
			});
		} , ac.errorHandler());

	}

	@Override
	public void handleUpdate(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			Project project = ac.getProject();
			updateObject(ac, "uuid", project.getNodeRoot());
		} , ac.errorHandler());
	}

	@Override
	public void handleRead(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			Project project = ac.getProject();
			loadTransformAndResponde(ac, "uuid", READ_PERM, project.getNodeRoot(), OK);
		} , ac.errorHandler());
	}

	@Override
	public void handleReadList(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			Project project = ac.getProject();
			loadTransformAndResponde(ac, project.getNodeRoot(), new NodeListResponse(), OK);
		} , ac.errorHandler());
	}

	public void handleMove(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			Project project = ac.getProject();
			// Load the node that should be moved
			String uuid = ac.getParameter("uuid");
			String toUuid = ac.getParameter("toUuid");
			loadObject(ac, "uuid", UPDATE_PERM, project.getNodeRoot(), sourceNodeHandler -> {
				if (hasSucceeded(ac, sourceNodeHandler)) {
					loadObject(ac, "toUuid", UPDATE_PERM, project.getNodeRoot(), targetNodeHandler -> {
						if (hasSucceeded(ac, targetNodeHandler)) {
							Node sourceNode = sourceNodeHandler.result();
							Node targetNode = targetNodeHandler.result();
							// TODO Update SQB
							sourceNode.moveTo(ac, targetNode, mh -> {
								if (mh.failed()) {
									ac.fail(mh.cause());
								} else {
									ac.sendMessage(OK, "node_moved_to", uuid, toUuid);
								}
							});

						}
					});
				}
			});
		} , ac.errorHandler());
	}

	// TODO abstract rc away
	public void handleDownload(RoutingContext rc) {
		InternalActionContext ac = InternalActionContext.create(rc);
		db.asyncNoTrx(tc -> {
			Project project = ac.getProject();
			loadObject(ac, "uuid", READ_PERM, project.getNodeRoot(), rh -> {
				db.noTrx(noTx -> {
					if (hasSucceeded(ac, rh)) {
						Node node = rh.result();
						if (!node.getSchema().isBinary()) {
							rc.fail(error(NOT_FOUND, "node_error_no_binary_node"));
							return;
						}
						File binaryFile = node.getBinaryFile();
						if (!binaryFile.exists()) {
							rc.fail(error(NOT_FOUND, "node_error_binary_data_not_found"));
							return;
						} else {
							String contentLength = String.valueOf(node.getBinaryFileSize());
							String fileName = node.getBinaryFileName();
							String contentType = node.getBinaryContentType();
							// Resize the image if needed
							if (node.hasBinaryImage() && ac.getImageRequestParameter().isSet()) {
								Observable<io.vertx.rxjava.core.buffer.Buffer> buffer = imageProvider.handleResize(node.getBinaryFile(),
										node.getBinarySHA512Sum(), ac.getImageRequestParameter());
								buffer.subscribe(imageBuffer -> {
									rc.response().putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(imageBuffer.length()));
									rc.response().putHeader(HttpHeaders.CONTENT_TYPE, "image/jpeg");
									// TODO encode filename?
									rc.response().putHeader("content-disposition", "attachment; filename=" + fileName);
									rc.response().end((Buffer) imageBuffer.getDelegate());
								});
							} else {
								node.getBinaryFileBuffer().setHandler(bh -> {

									Buffer buffer = bh.result();

									rc.response().putHeader(HttpHeaders.CONTENT_LENGTH, contentLength);
									rc.response().putHeader(HttpHeaders.CONTENT_TYPE, contentType);
									// TODO encode filename?
									rc.response().putHeader("content-disposition", "attachment; filename=" + fileName);
									rc.response().end(buffer);
								});
							}
						}
					}
				});
			});
		} , ac.errorHandler());
	}

	public void handleUpload(InternalHttpActionContext ac) {
		db.asyncNoTrx(tc -> {
			Project project = ac.getProject();
			MeshUploadOptions uploadOptions = Mesh.mesh().getOptions().getUploadOptions();
			loadObject(ac, "uuid", UPDATE_PERM, project.getNodeRoot(), rh -> {
				if (hasSucceeded(ac, rh)) {
					Node node = rh.result();
					try {
						Schema schema = node.getSchema();
						if (!schema.isBinary()) {
							ac.fail(BAD_REQUEST, "node_error_no_binary_node");
						} else {
							Set<FileUpload> fileUploads = ac.getFileUploads();
							if (fileUploads.isEmpty()) {
								ac.fail(BAD_REQUEST, "node_error_no_binarydata_found");
							} else if (fileUploads.size() > 1) {
								ac.fail(BAD_REQUEST, "node_error_more_than_one_binarydata_included");
							} else {
								FileUpload ul = fileUploads.iterator().next();
								long byteLimit = uploadOptions.getByteLimit();
								if (ul.size() > byteLimit) {
									if (log.isDebugEnabled()) {
										log.debug("Upload size of {" + ul.size() + "} exeeds limit of {" + byteLimit + "} by {"
												+ (ul.size() - byteLimit) + "} bytes.");
									}
									String humanReadableFileSize = org.apache.commons.io.FileUtils.byteCountToDisplaySize(ul.size());
									String humanReadableUploadLimit = org.apache.commons.io.FileUtils.byteCountToDisplaySize(byteLimit);
									ac.fail(BAD_REQUEST, "node_error_uploadlimit_reached", humanReadableFileSize, humanReadableUploadLimit);
								} else {
									String contentType = ul.contentType();
									String fileName = ul.fileName();
									String nodeUuid = node.getUuid();
									hashAndMoveBinaryFile(ac, ul, nodeUuid, node.getBinarySegmentedPath(), fh -> {
										if (fh.failed()) {
											ac.fail(fh.cause());
										} else {
											db.trx(txUpdate -> {
												node.setBinaryFileName(fileName);
												node.setBinaryFileSize(ul.size());
												node.setBinaryContentType(contentType);
												node.setBinarySHA512Sum(fh.result());
												// node.setBinaryImageDPI(dpi);
												// node.setBinaryImageHeight(heigth);
												// node.setBinaryImageWidth(width);
												SearchQueueBatch batch = node.addIndexBatch(SearchQueueEntryAction.UPDATE_ACTION);
												txUpdate.complete(Tuple.tuple(batch, node));
											} , (AsyncResult<Tuple<SearchQueueBatch, Node>> txUpdated) -> {
												if (txUpdated.failed()) {
													ac.errorHandler().handle(Future.failedFuture(txUpdated.cause()));
												} else {
													VerticleHelper.processOrFail(ac, txUpdated.result().v1(), ch -> {
														ac.sendMessage(OK, "node_binary_field_updated", nodeUuid);
													} , txUpdated.result().v2());
												}
											});

										}
									});
								}
							}
						}
					} catch (Exception e) {
						log.error("Could not load schema for node {" + node.getUuid() + "}");
						ac.fail(e);
					}

				}
			});
		} , ac.errorHandler());
	}

	private void hashAndMoveBinaryFile(ActionContext ac, FileUpload fileUpload, String uuid, String segmentedPath,
			Handler<AsyncResult<String>> handler) {
		MeshUploadOptions uploadOptions = Mesh.mesh().getOptions().getUploadOptions();
		FileSystem fileSystem = Mesh.vertx().fileSystem();

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
						handler.handle(Future.failedFuture(eh.cause()));
						return;
					}
					if (eh.result()) {
						fileSystem.delete(targetPath, dh -> {
							if (dh.succeeded()) {
								fileSystem.move(fileUpload.uploadedFileName(), targetPath, mh -> {
									if (mh.succeeded()) {
										handler.handle(Future.succeededFuture(hashSum.get()));
									} else {
										log.error("Failed to move file to {" + targetPath + "}", mh.cause());
										handler.handle(failedFuture(INTERNAL_SERVER_ERROR, "node_error_upload_failed", mh.cause()));
										return;
									}
								});
							} else {
								handler.handle(Future.failedFuture(eh.cause()));
								return;
							}
						});
					} else {
						fileSystem.move(fileUpload.uploadedFileName(), targetPath, mh -> {
							if (mh.succeeded()) {
								handler.handle(Future.succeededFuture(hashSum.get()));
							} else {
								log.error("Failed to move file to {" + targetPath + "}", mh.cause());
								handler.handle(failedFuture(INTERNAL_SERVER_ERROR, "node_error_upload_failed", mh.cause()));
							}
						});
					}

				});

			} else {
				handler.handle(failedFuture(INTERNAL_SERVER_ERROR, "node_error_upload_failed", tfc.cause()));
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
									handler.handle(failedFuture(BAD_REQUEST, "node_error_upload_failed"));
								}
							});
						} else {
							targetFolderChecked.handle(Future.succeededFuture(folder));
						}
					} else {
						log.error("Could not check whether target directory {" + folder.getAbsolutePath() + "} exists.", deh.cause());
						handler.handle(failedFuture(BAD_REQUEST, "node_error_upload_failed", deh.cause()));
					}
				});
			} else {
				handler.handle(failedFuture(BAD_REQUEST, "node_error_hashing_failed"));
			}
		});

	}

	public void handleReadChildren(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			Project project = ac.getProject();
			loadObject(ac, "uuid", READ_PERM, project.getNodeRoot(), rh -> {
				if (hasSucceeded(ac, rh)) {
					Node node = rh.result();
					try {
						Page<? extends Node> page = node.getChildren(ac.getUser(), ac.getSelectedLanguageTags(), ac.getPagingParameter());
						transformAndResponde(ac, page, new NodeListResponse(), OK);
					} catch (Exception e) {
						ac.fail(e);
					}
				}
			});
		} , ac.errorHandler());
	}

	public void readTags(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			Project project = ac.getProject();
			loadObject(ac, "uuid", READ_PERM, project.getNodeRoot(), rh -> {
				if (hasSucceeded(ac, rh)) {
					Node node = rh.result();
					try {
						Page<? extends Tag> tagPage = node.getTags(ac);
						transformAndResponde(ac, tagPage, new TagListResponse(), OK);
					} catch (Exception e) {
						ac.fail(e);
					}
				}
			});
		} , ac.errorHandler());
	}

	public void handleAddTag(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			Project project = ac.getProject();
			if (project == null) {
				ac.fail(BAD_REQUEST, "Project not found");
				// TODO i18n error
			} else {
				loadObject(ac, "uuid", UPDATE_PERM, project.getNodeRoot(), rh -> {
					if (hasSucceeded(ac, rh)) {
						Node node = rh.result();
						loadObject(ac, "tagUuid", READ_PERM, project.getTagRoot(), th -> {
							// TODO check whether the tag has already been assigned to the node. In this case we need to do nothing.
							if (hasSucceeded(ac, th)) {
								Tag tag = th.result();
								db.trx(txAdd -> {
									SearchQueueBatch batch = node.addIndexBatch(UPDATE_ACTION);
									node.addTag(tag);
									txAdd.complete(Tuple.tuple(batch, node));
								} , (AsyncResult<Tuple<SearchQueueBatch, Node>> txAdded) -> {
									if (txAdded.failed()) {
										ac.errorHandler().handle(Future.failedFuture(txAdded.cause()));
									} else {
										processOrFail(ac, txAdded.result().v1(), ch -> {
											transformAndResponde(ac, ch.result(), OK);
										} , txAdded.result().v2());
									}
								});

							}
						});
					}
				});
			}
		} , ac.errorHandler());
	}

	public void handleRemoveTag(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			Project project = ac.getProject();
			loadObject(ac, "uuid", UPDATE_PERM, project.getNodeRoot(), rh -> {
				loadObject(ac, "tagUuid", READ_PERM, project.getTagRoot(), srh -> {
					if (hasSucceeded(ac, srh) && hasSucceeded(ac, rh)) {
						Node node = rh.result();
						Tag tag = srh.result();
						db.trx(txRemove -> {
							SearchQueueBatch batch = node.addIndexBatch(SearchQueueEntryAction.UPDATE_ACTION);
							node.removeTag(tag);
							txRemove.complete(Tuple.tuple(batch, node));
						} , (AsyncResult<Tuple<SearchQueueBatch, Node>> txAdded) -> {
							if (txAdded.failed()) {
								ac.errorHandler().handle(Future.failedFuture(txAdded.cause()));
							} else {
								processOrFail(ac, txAdded.result().v1(), ch -> {
									transformAndResponde(ac, ch.result(), OK);
								} , txAdded.result().v2());
							}
						});
					}
				});
			});
		} , ac.errorHandler());
	}

	public void handleReadField(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			Project project = ac.getProject();
			loadObject(ac, "uuid", READ_PERM, project.getNodeRoot(), rh -> {
				if (hasSucceeded(ac, rh)) {
					Node node = rh.result();
					NodeGraphFieldContainer container = node.findNextMatchingFieldContainer(ac);
					if (container == null) {
						// TODO fail
					} else {
						// node.getSchema().getFields().
						// container.getRestFieldFromGraph(ac, fieldKey, fieldSchema, expandField, handler);
					}
				}
			});
		} , ac.errorHandler());
	}

	public void handleAddFieldItem(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			Project project = ac.getProject();
			loadObject(ac, "uuid", UPDATE_PERM, project.getNodeRoot(), rh -> {
				// TODO Update SQB
				if (hasSucceeded(ac, rh)) {
					Node node = rh.result();
				}
			});
		} , ac.errorHandler());
	}

	public void handleUpdateField(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			Project project = ac.getProject();
			loadObject(ac, "uuid", UPDATE_PERM, project.getNodeRoot(), rh -> {
				// TODO Update SQB
				if (hasSucceeded(ac, rh)) {
					Node node = rh.result();
				}
			});
		} , ac.errorHandler());
	}

	public void handleRemoveField(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			Project project = ac.getProject();
			loadObject(ac, "uuid", UPDATE_PERM, project.getNodeRoot(), rh -> {
				// TODO Update SQB
				if (hasSucceeded(ac, rh)) {
					Node node = rh.result();
				}
			});
		} , ac.errorHandler());
	}

	public void handleRemoveFieldItem(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			Project project = ac.getProject();
			loadObject(ac, "uuid", UPDATE_PERM, project.getNodeRoot(), rh -> {
				// TODO Update SQB
				if (hasSucceeded(ac, rh)) {
					Node node = rh.result();
				}
			});
		} , ac.errorHandler());
	}

	public void handleUpdateFieldItem(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			Project project = ac.getProject();
			loadObject(ac, "uuid", UPDATE_PERM, project.getNodeRoot(), rh -> {
				// TODO Update SQB
				if (hasSucceeded(ac, rh)) {
					Node node = rh.result();
				}
			});
		} , ac.errorHandler());
	}

	public void handleReadFieldItem(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			Project project = ac.getProject();
			loadObject(ac, "uuid", READ_PERM, project.getNodeRoot(), rh -> {
				if (hasSucceeded(ac, rh)) {
					Node node = rh.result();
				}
			});
		} , ac.errorHandler());
	}

	public void handleMoveFieldItem(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			Project project = ac.getProject();
			loadObject(ac, "uuid", UPDATE_PERM, project.getNodeRoot(), rh -> {
				// TODO Update SQB
				if (hasSucceeded(ac, rh)) {
					Node node = rh.result();
				}
			});
		} , ac.errorHandler());
	}

	public void handelReadBreadcrumb(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			Project project = ac.getProject();
			loadObject(ac, "uuid", READ_PERM, project.getNodeRoot(), rh -> {
				if (hasSucceeded(ac, rh)) {
					Node node = rh.result();
					node.transformToBreadcrumb(ac, th -> {
						ac.send(JsonUtil.toJson(th.result()), OK);
					});
				}
			});
		} , ac.errorHandler());
	}

}
