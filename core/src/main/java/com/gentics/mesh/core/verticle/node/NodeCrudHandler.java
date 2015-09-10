package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.json.JsonUtil.toJson;
import static com.gentics.mesh.util.VerticleHelper.createObject;
import static com.gentics.mesh.util.VerticleHelper.deleteObject;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.updateObject;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;

import java.io.File;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.etc.config.MeshUploadOptions;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.handler.HttpActionContext;
import com.gentics.mesh.util.FileUtils;
import com.gentics.mesh.util.VerticleHelper;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

@Component
public class NodeCrudHandler extends AbstractCrudHandler {

	private static final Logger log = LoggerFactory.getLogger(NodeCrudHandler.class);

	@Override
	public void handleCreate(ActionContext ac) {
		try (Trx tx = db.trx()) {
			createObject(ac, boot.meshRoot().getNodeRoot());
		}
	}

	@Override
	public void handleDelete(ActionContext ac) {
		//		Mesh.vertx().executeBlocking(bc -> {
		try (Trx tx = db.trx()) {
			String uuid = ac.getParameter("uuid");
			Project project = ac.getProject();
			if (project.getBaseNode().getUuid().equals(uuid)) {
				ac.fail(METHOD_NOT_ALLOWED, "node_basenode_not_deletable");
			} else {
				deleteObject(ac, "uuid", "node_deleted", ac.getProject().getNodeRoot());
			}
		}
		//		} , false, rh -> {
		//			if (rh.failed()) {
		//				rc.fail(new HttpStatusCodeErrorException(INTERNAL_SERVER_ERROR, rh.cause().getMessage()));
		//			}
		//		});
	}

	@Override
	public void handleUpdate(ActionContext ac) {
		//		Mesh.vertx().executeBlocking(bc -> {
		try (Trx tx = db.trx()) {
			Project project = ac.getProject();
			updateObject(ac, "uuid", project.getNodeRoot());
		}
		//		} , false, rh -> {
		//			if (rh.failed()) {
		//				rh.cause().printStackTrace();
		//			}
		//		});

	}

	@Override
	public void handleRead(ActionContext ac) {

		//			Mesh.vertx().executeBlocking(bc -> {
		try (Trx tx = db.trx()) {
			Project project = ac.getProject();
			loadTransformAndResponde(ac, "uuid", READ_PERM, project.getNodeRoot());
		}
		//			} , false, rh -> {
		//				if (rh.failed()) {
		//					rh.cause().printStackTrace();
		//				}
		//			});
	}

	@Override
	public void handleReadList(ActionContext ac) {
		try (Trx tx = db.trx()) {
			Project project = ac.getProject();
			loadTransformAndResponde(ac, project.getNodeRoot(), new NodeListResponse());
		}
	}

	public void handleMove(ActionContext ac) {
		try (Trx tx = db.trx()) {
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

							sourceNode.moveTo(ac, targetNode, mh -> {
								if (mh.failed()) {
									ac.fail(mh.cause());
								} else {
									ac.send(toJson(new GenericMessageResponse(ac.i18n("node_moved_to", uuid, toUuid))));
								}
							});

						}
					});
				}
			});
		}
	}

	//TODO abstract rc away
	public void handleDownload(RoutingContext rc) {
		try (Trx tx = db.trx()) {
			ActionContext ac = ActionContext.create(rc);
			Project project = ac.getProject();
			loadObject(ac, "uuid", READ_PERM, project.getNodeRoot(), rh -> {
				if (hasSucceeded(ac, rh)) {
					Node node = rh.result();
					node.getBinaryFileBuffer().setHandler(bh -> {
						rc.response().putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(node.getBinaryFileSize()));
						rc.response().putHeader(HttpHeaders.CONTENT_TYPE, node.getBinaryContentType());
						// TODO encode filename?
						rc.response().putHeader("content-disposition", "attachment; filename=" + node.getBinaryFileName());
						rc.response().end(bh.result());
					});
				}
			});
		}
	}

	public void handleUpload(HttpActionContext ac) {

		try (Trx tx = db.trx()) {
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

									hashAndMoveBinaryFile(ac, ul, node.getUuid(), node.getSegmentedPath(), fh -> {
										if (fh.failed()) {
											ac.fail(fh.cause());
										} else {
											SearchQueueBatch batch;
											try (Trx txUpdate = db.trx()) {
												node.setBinaryFileName(fileName);
												node.setBinaryFileSize(ul.size());
												node.setBinaryContentType(contentType);
												node.setBinarySHA512Sum(fh.result());
												// node.setBinaryImageDPI(dpi);
												// node.setBinaryImageHeight(heigth);
												// node.setBinaryImageWidth(width);
												batch = node.addIndexBatch(SearchQueueEntryAction.UPDATE_ACTION);
												txUpdate.success();
											}
											VerticleHelper.processOrFail(ac, batch, ch -> {
												ac.send(toJson(
														new GenericMessageResponse(ac.i18n("node_binary_field_updated", ch.result().getUuid()))));
											} , node);

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
		}
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
				String targetPath = new File(targetFolder, uuid + ".bin").getAbsolutePath();
				if (log.isDebugEnabled()) {
					log.debug("Moving file from {" + fileUpload.uploadedFileName() + "} to {" + targetPath + "}");
				}

				fileSystem.move(fileUpload.uploadedFileName(), targetPath, mh -> {
					if (mh.succeeded()) {
						handler.handle(Future.succeededFuture(hashSum.get()));
					} else {
						log.error("Failed to move file to {" + targetPath + "}", mh.cause());
						handler.handle(ac.failedFuture(INTERNAL_SERVER_ERROR, "node_error_upload_failed", mh.cause()));
					}
				});
			} else {
				handler.handle(ac.failedFuture(INTERNAL_SERVER_ERROR, "node_error_upload_failed", tfc.cause()));
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
									handler.handle(ac.failedFuture(BAD_REQUEST, "node_error_upload_failed"));
								}
							});
						} else {
							targetFolderChecked.handle(Future.succeededFuture(folder));
						}
					} else {
						log.error("Could not check whether target directory {" + folder.getAbsolutePath() + "} exists.", deh.cause());
						handler.handle(ac.failedFuture(BAD_REQUEST, "node_error_upload_failed", deh.cause()));
					}
				});
			} else {
				handler.handle(ac.failedFuture(BAD_REQUEST, "node_error_hashing_failed"));
			}
		});

	}

	public void handleReadChildren(ActionContext ac) {
		try (Trx tx = db.trx()) {
			Project project = ac.getProject();
			loadObject(ac, "uuid", READ_PERM, project.getNodeRoot(), rh -> {
				if (hasSucceeded(ac, rh)) {
					Node node = rh.result();
					try {
						Page<? extends Node> page = node.getChildren(ac.getUser(), ac.getSelectedLanguageTags(), ac.getPagingInfo());
						transformAndResponde(ac, page, new NodeListResponse());
					} catch (Exception e) {
						ac.fail(e);
					}
				}
			});
		}
	}

	public void readTags(ActionContext ac) {
		try (Trx tx = db.trx()) {
			Project project = ac.getProject();
			loadObject(ac, "uuid", READ_PERM, project.getNodeRoot(), rh -> {
				if (hasSucceeded(ac, rh)) {
					Node node = rh.result();
					try {
						Page<? extends Tag> tagPage = node.getTags(ac);
						transformAndResponde(ac, tagPage, new TagListResponse());
					} catch (Exception e) {
						ac.fail(e);
					}
				}
			});
		}
	}

	public void handleAddTag(ActionContext ac) {
		try (Trx tx = db.trx()) {
			Project project = ac.getProject();
			if (project == null) {
				ac.fail(BAD_REQUEST, "Project not found");
				// TODO i18n error
			} else {
				loadObject(ac, "uuid", UPDATE_PERM, project.getNodeRoot(), rh -> {
					if (hasSucceeded(ac, rh)) {
						Node node = rh.result();
						loadObject(ac, "tagUuid", READ_PERM, project.getTagRoot(), th -> {
							if (hasSucceeded(ac, th)) {
								Tag tag = th.result();
								try (Trx txAdd = db.trx()) {
									node.addTag(tag);
									txAdd.success();
								}
								transformAndResponde(ac, node);
							}
						});
					}
				});
			}
		}
	}

	public void handleRemoveTag(ActionContext ac) {
		try (Trx tx = db.trx()) {
			Project project = ac.getProject();
			loadObject(ac, "uuid", UPDATE_PERM, project.getNodeRoot(), rh -> {
				loadObject(ac, "tagUuid", READ_PERM, project.getTagRoot(), srh -> {
					if (hasSucceeded(ac, srh) && hasSucceeded(ac, rh)) {
						Node node = rh.result();
						Tag tag = srh.result();
						try (Trx txRemove = db.trx()) {
							node.removeTag(tag);
							txRemove.success();
						}
						transformAndResponde(ac, node);
					}
				});
			});
		}
	}

	public void handleReadField(ActionContext ac) {
		try (Trx tx = db.trx()) {
			Project project = ac.getProject();
			loadObject(ac, "uuid", READ_PERM, project.getNodeRoot(), rh -> {

			});
		}
	}

	public void handleAddFieldItem(ActionContext ac) {
		try (Trx tx = db.trx()) {
			Project project = ac.getProject();
			loadObject(ac, "uuid", UPDATE_PERM, project.getNodeRoot(), rh -> {

			});
		}
	}

	public void handleUpdateField(ActionContext ac) {
		try (Trx tx = db.trx()) {
			Project project = ac.getProject();
			loadObject(ac, "uuid", UPDATE_PERM, project.getNodeRoot(), rh -> {

			});
		}
	}

	public void handleRemoveField(ActionContext ac) {
		try (Trx tx = db.trx()) {
			Project project = ac.getProject();
			loadObject(ac, "uuid", UPDATE_PERM, project.getNodeRoot(), rh -> {

			});
		}
	}

	public void handleRemoveFieldItem(ActionContext ac) {
		try (Trx tx = db.trx()) {
			Project project = ac.getProject();
			loadObject(ac, "uuid", UPDATE_PERM, project.getNodeRoot(), rh -> {

			});
		}
	}

	public void handleUpdateFieldItem(ActionContext ac) {
		try (Trx tx = db.trx()) {
			Project project = ac.getProject();
			loadObject(ac, "uuid", UPDATE_PERM, project.getNodeRoot(), rh -> {

			});
		}
	}

	public void handleReadFieldItem(ActionContext ac) {
		try (Trx tx = db.trx()) {
			Project project = ac.getProject();
			loadObject(ac, "uuid", READ_PERM, project.getNodeRoot(), rh -> {

			});
		}
	}

	public void handleMoveFieldItem(ActionContext ac) {
		try (Trx tx = db.trx()) {
			Project project = ac.getProject();
			loadObject(ac, "uuid", UPDATE_PERM, project.getNodeRoot(), rh -> {

			});
		}
	}
}
