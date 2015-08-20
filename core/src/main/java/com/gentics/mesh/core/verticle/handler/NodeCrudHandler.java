package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.search.SearchQueue.SEARCH_QUEUE_ENTRY_ADDRESS;
import static com.gentics.mesh.json.JsonUtil.toJson;
import static com.gentics.mesh.util.VerticleHelper.fail;
import static com.gentics.mesh.util.VerticleHelper.getPagingInfo;
import static com.gentics.mesh.util.VerticleHelper.getSelectedLanguageTags;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.loadObjectByUuid;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.responde;
import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReferenceInfo;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.error.EntityNotFoundException;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.etc.config.MeshUploadOptions;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.FileUtils;

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

	@Autowired
	private ServerSchemaStorage schemaStorage;

	@Override
	public void handleCreate(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			Project project = getProject(rc);
			MeshAuthUser requestUser = getUser(rc);

			String body = rc.getBodyAsString();
			SchemaReferenceInfo schemaInfo;
			try {
				schemaInfo = JsonUtil.readValue(body, SchemaReferenceInfo.class);
			} catch (Exception e) {
				rc.fail(e);
				return;
			}

			boolean missingSchemaInfo = schemaInfo.getSchema() == null
					|| (StringUtils.isEmpty(schemaInfo.getSchema().getUuid()) && StringUtils.isEmpty(schemaInfo.getSchema().getName()));
			if (missingSchemaInfo) {
				rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "error_schema_parameter_missing")));
				return;
			}

			Handler<AsyncResult<SchemaContainer>> containerFoundHandler = rh -> {
				/*
				 * SchemaContainer schema = boot.schemaContainerRoot().findByName(requestModel.getSchema().getName()); if (schema == null) { rc.fail(new
				 * HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "schema_not_found", requestModel.getSchema() .getName()))); return; }
				 */
				SchemaContainer schemaContainer = rh.result();
				try {
					Schema schema = schemaContainer.getSchema();
					NodeCreateRequest requestModel = JsonUtil.readNode(body, NodeCreateRequest.class, schemaStorage);
					if (StringUtils.isEmpty(requestModel.getParentNodeUuid())) {
						rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "node_missing_parentnode_field")));
						return;
					}
					if (StringUtils.isEmpty(requestModel.getLanguage())) {
						rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "node_no_languagecode_specified")));
						return;
					}

					Handler<AsyncResult<Node>> handler = ch -> {
						if (hasSucceeded(rc, ch)) {
							Node node = ch.result();
							transformAndResponde(rc, node);
						}
					};
					Future<Node> nodeCreated = Future.future();
					nodeCreated.setHandler(handler);

					Handler<AsyncResult<Node>> nodeCreatedHandler = pnh -> {
						Node node = pnh.result();

						node.setPublished(requestModel.isPublished());
						Language language = boot.languageRoot().findByLanguageTag(requestModel.getLanguage());
						if (language == null) {
							nodeCreated.fail(new HttpStatusCodeErrorException(BAD_REQUEST,
									i18n.get(rc, "node_no_language_found", requestModel.getLanguage())));
						} else {
							try {
								NodeFieldContainer container = node.getOrCreateFieldContainer(language);
								container.setFieldFromRest(rc, requestModel.getFields(), schema);

								// Inform elasticsearch about the new element
								searchQueue().put(node.getUuid(), Node.TYPE, SearchQueueEntryAction.CREATE_ACTION);
								vertx.eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, null);
								tx.success();
								nodeCreated.complete(node);
							} catch (Exception e) {
								rc.fail(e);
								return;
							}
						}
					};

					loadObjectByUuid(rc, requestModel.getParentNodeUuid(), CREATE_PERM, project.getNodeRoot(), rhp -> {
						if (hasSucceeded(rc, rhp)) {
							Node parentNode = rhp.result();
							Node node = parentNode.create(requestUser, schemaContainer, project);
							requestUser.addCRUDPermissionOnRole(parentNode, CREATE_PERM, node);
							nodeCreatedHandler.handle(Future.succeededFuture(node));
						}
					});
				} catch (Exception e) {
					rc.fail(e);
					return;
				}
			};
			if (!StringUtils.isEmpty(schemaInfo.getSchema().getName())) {
				SchemaContainer containerByName = project.getSchemaContainerRoot().findByName(schemaInfo.getSchema().getName());
				if (containerByName != null) {
					if (requestUser.hasPermission(containerByName, READ_PERM)) {
						containerFoundHandler.handle(Future.succeededFuture(containerByName));
					} else {
						rc.fail(new InvalidPermissionException(i18n.get(rc, "error_missing_perm", containerByName.getUuid())));
					}
				} else {
					rc.fail(new EntityNotFoundException(i18n.get(rc, "schema_not_found", schemaInfo.getSchema().getName())));
				}
			} else {
				loadObjectByUuid(rc, schemaInfo.getSchema().getUuid(), READ_PERM, project.getSchemaContainerRoot(), rh -> {
					if (hasSucceeded(rc, rh)) {
						SchemaContainer schemaContainer = rh.result();
						containerFoundHandler.handle(Future.succeededFuture(schemaContainer));
					}
				});
			}
		}
	}

	@Override
	public void handleDelete(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			String uuid = rc.request().params().get("uuid");
			Project project = getProject(rc);
			if (project.getBaseNode().getUuid().equals(uuid)) {
				rc.fail(new HttpStatusCodeErrorException(METHOD_NOT_ALLOWED, i18n.get(rc, "node_basenode_not_deletable")));
			} else {
				delete(rc, "uuid", "node_deleted", getProject(rc).getNodeRoot());
			}
		}
	}

	@Override
	public void handleUpdate(RoutingContext rc) {
		NodeUpdateRequest requestModel;
		try {
			requestModel = JsonUtil.readNode(rc.getBodyAsString(), NodeUpdateRequest.class, schemaStorage);
			if (StringUtils.isEmpty(requestModel.getLanguage())) {
				rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "error_language_not_set")));
				return;
			}
			try (Trx tx = new Trx(db)) {
				Project project = getProject(rc);
				loadObject(rc, "uuid", READ_PERM, project.getNodeRoot(), rh -> {
					if (hasSucceeded(rc, rh)) {
						Node node = rh.result();
						try {
							Language language = boot.languageRoot().findByLanguageTag(requestModel.getLanguage());
							if (language == null) {
								rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST,
										i18n.get(rc, "error_language_not_found", requestModel.getLanguage())));
								return;
							}
							try (Trx txUpdate = new Trx(db)) {
								/* TODO handle other fields, etc. */
								node.setPublished(requestModel.isPublished());
								node.setEditor(getUser(rc));
								node.setLastEditedTimestamp(System.currentTimeMillis());
								NodeFieldContainer container = node.getOrCreateFieldContainer(language);
								Schema schema = node.getSchema();
								try {
									container.setFieldFromRest(rc, requestModel.getFields(), schema);
									searchQueue().put(node.getUuid(), Node.TYPE, SearchQueueEntryAction.UPDATE_ACTION);
									vertx.eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, null);
									txUpdate.success();
									transformAndResponde(rc, node);
								} catch (MeshSchemaException e) {
									txUpdate.failure();
									/* TODO i18n */
									rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, e.getMessage()));
								}
							}

						} catch (IOException e) {
							rc.fail(e);
						}
					}
				});
			}
		} catch (Exception e1) {
			rc.fail(e1);
		}
	}

	@Override
	public void handleRead(RoutingContext rc) {
		String uuid = rc.request().params().get("uuid");
		if (StringUtils.isEmpty(uuid)) {
			rc.next();
		} else {
			try (Trx tx = new Trx(db)) {
				Project project = getProject(rc);
				loadTransformAndResponde(rc, "uuid", READ_PERM, project.getNodeRoot());
			}
		}
	}

	@Override
	public void handleReadList(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			Project project = getProject(rc);
			loadTransformAndResponde(rc, project.getNodeRoot(), new NodeListResponse());
		}
	}

	public void handleMove(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			Project project = getProject(rc);
			// Load the node that should be moved
			String uuid = rc.request().params().get("uuid");
			String toUuid = rc.request().params().get("toUuid");
			loadObject(rc, "uuid", UPDATE_PERM, project.getNodeRoot(), sourceNodeHandler -> {
				if (hasSucceeded(rc, sourceNodeHandler)) {
					loadObject(rc, "toUuid", UPDATE_PERM, project.getNodeRoot(), targetNodeHandler -> {
						if (hasSucceeded(rc, targetNodeHandler)) {
							Node sourceNode = sourceNodeHandler.result();
							Node targetNode = targetNodeHandler.result();

							// TODO should we add a guard that terminates this loop when it runs to long?
							// Check whether the target node is part of the subtree of the source node.
							Node parent = targetNode.getParentNode();
							while (parent != null) {
								if (parent.getUuid().equals(sourceNode.getUuid())) {
									fail(rc, "node_move_error_not_allowd_to_move_node_into_one_of_its_children");
									return;
								}
								parent = parent.getParentNode();
							}

							try {
								if (!targetNode.getSchema().isFolder()) {
									fail(rc, "node_move_error_targetnode_is_no_folder");
									return;
								}
							} catch (Exception e) {
								log.error("Could not load schema for target node during move action", e);
								// TODO maybe add better i18n error
								rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "error"), e));
								return;
							}

							if (sourceNode.getUuid().equals(targetNode.getUuid())) {
								fail(rc, "node_move_error_same_nodes");
								return;
							}

							// TODO check whether there is a node in the target node that has the same name. We do this to prevent issues for the webroot api

							// Move the node
							try (Trx txMove = new Trx(db)) {
								sourceNode.setParentNode(targetNode);
								sourceNode.setEditor(getUser(rc));
								sourceNode.setLastEditedTimestamp(System.currentTimeMillis());
								targetNode.setEditor(getUser(rc));
								targetNode.setLastEditedTimestamp(System.currentTimeMillis());
								txMove.success();
							}
							// TODO update the search index
							responde(rc, toJson(new GenericMessageResponse(i18n.get(rc, "node_moved_to", uuid, toUuid))));
						}
					});
				}
			});
		}
	}

	public void handleDownload(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			Project project = getProject(rc);
			loadObject(rc, "uuid", READ_PERM, project.getNodeRoot(), rh -> {
				if (hasSucceeded(rc, rh)) {
					Node node = rh.result();
					node.getBinaryFileBuffer().setHandler(bh -> {
						// TODO set content disposition
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

	public void handleUpload(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			FileSystem fileSystem = Mesh.vertx().fileSystem();
			Project project = getProject(rc);
			MeshUploadOptions uploadOptions = Mesh.mesh().getOptions().getUploadOptions();
			loadObject(rc, "uuid", UPDATE_PERM, project.getNodeRoot(), rh -> {
				if (hasSucceeded(rc, rh)) {
					Node node = rh.result();
					try {
						Schema schema = node.getSchema();
						if (!schema.isBinary()) {
							fail(rc, "node_error_no_binary_node");
						} else {
							Set<FileUpload> fileUploads = rc.fileUploads();
							if (fileUploads.isEmpty()) {
								fail(rc, "node_error_no_binarydata_found");
							} else if (fileUploads.size() > 1) {
								fail(rc, "node_error_more_than_one_binarydata_included");
							} else {
								FileUpload ul = fileUploads.iterator().next();
								long byteLimit = uploadOptions.getByteLimit();
								if (ul.size() > byteLimit) {
									String humanReadableFileSize = org.apache.commons.io.FileUtils.byteCountToDisplaySize(ul.size());
									String humanReadableUploadLimit = org.apache.commons.io.FileUtils.byteCountToDisplaySize(byteLimit);
									fail(rc, "node_error_uploadlimit_reached", humanReadableFileSize, humanReadableUploadLimit);
								} else {
									String contentType = ul.contentType();
									String fileName = ul.fileName();
									try (Trx txUpdate = new Trx(db)) {
										node.setBinaryFileName(fileName);
										node.setBinaryFileSize(ul.size());
										node.setBinaryContentType(contentType);

										Handler<AsyncResult<File>> targetFolderChecked = tfc -> {

											if (tfc.succeeded()) {
												File targetFolder = tfc.result();
												String targetPath = new File(targetFolder, node.getUuid() + ".bin").getAbsolutePath();
												if (log.isDebugEnabled()) {
													log.debug("Moving file from {" + ul.uploadedFileName() + "} to {" + targetPath + "}");
												}

												fileSystem.move(ul.uploadedFileName(), targetPath, mh -> {
													if (mh.succeeded()) {
														txUpdate.success();
														responde(rc, toJson(new GenericMessageResponse(
																i18n.get(rc, "node_binary_field_updated", node.getUuid()))));
													} else {
														log.error("Failed to move file to {" + targetPath + "}", mh.cause());
														fail(rc, "node_error_upload_failed");
													}
												});
											} else {
												fail(rc, "node_error_upload_failed");
											}
										};

										FileUtils.generateSha512Sum(ul.uploadedFileName(), hash -> {
											if (hash.succeeded()) {
												node.setBinarySHA512Sum(hash.result());
												System.out.println("FILE: " + ul.uploadedFileName());

												File folder = new File(uploadOptions.getDirectory(), node.getSegmentedPath());
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
																	log.error("Failed to create target folder {" + folder.getAbsolutePath() + "}",
																			mkh.cause());
																	fail(rc, "node_error_upload_failed");
																}
															});
														} else {
															targetFolderChecked.handle(Future.succeededFuture(folder));
														}
													} else {
														log.error(
																"Could not check whether target directory {" + folder.getAbsolutePath() + "} exists.",
																deh.cause());
														fail(rc, "node_error_upload_failed");
													}
												});

												// node.setBinaryImageDPI(dpi);
												// node.setBinaryImageHeight(heigth);
												// node.setBinaryImageWidth(width);
											} else {
												fail(rc, "node_error_hashing_failed");
											}
										});
									}
								}
							}
						}
					} catch (Exception e) {
						log.error("Could not load schema for node {" + node.getUuid() + "}");
						rc.fail(e);
					}

				}
			});
		}
	}

	public void handleReadChildren(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			MeshAuthUser requestUser = getUser(rc);
			Project project = getProject(rc);
			loadObject(rc, "uuid", READ_PERM, project.getNodeRoot(), rh -> {
				if (hasSucceeded(rc, rh)) {
					Node node = rh.result();
					try {
						Page<? extends Node> page = node.getChildren(requestUser, getSelectedLanguageTags(rc), getPagingInfo(rc));
						transformAndResponde(rc, page, new NodeListResponse());
					} catch (Exception e) {
						rc.fail(e);
					}
				}
			});
		}
	}

	public void readTags(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			Project project = getProject(rc);
			loadObject(rc, "uuid", READ_PERM, project.getNodeRoot(), rh -> {
				if (hasSucceeded(rc, rh)) {
					Node node = rh.result();
					try {
						Page<? extends Tag> tagPage = node.getTags(rc);
						transformAndResponde(rc, tagPage, new TagListResponse());
					} catch (Exception e) {
						rc.fail(e);
					}
				}
			});
		}
	}

	public void handleAddTag(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			Project project = getProject(rc);
			if (project == null) {
				rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, "Project not found"));
				// TODO i18n error
			} else {
				loadObject(rc, "uuid", UPDATE_PERM, project.getNodeRoot(), rh -> {
					if (hasSucceeded(rc, rh)) {
						Node node = rh.result();
						loadObject(rc, "tagUuid", READ_PERM, project.getTagRoot(), th -> {
							if (hasSucceeded(rc, th)) {
								Tag tag = th.result();
								try (Trx txAdd = new Trx(db)) {
									node.addTag(tag);
									txAdd.success();
								}
								transformAndResponde(rc, node);
							}
						});
					}
				});
			}
		}
	}

	public void handleRemoveTag(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			Project project = getProject(rc);
			loadObject(rc, "uuid", UPDATE_PERM, project.getNodeRoot(), rh -> {
				loadObject(rc, "tagUuid", READ_PERM, project.getTagRoot(), srh -> {
					if (hasSucceeded(rc, srh) && hasSucceeded(rc, rh)) {
						Node node = rh.result();
						Tag tag = srh.result();
						try (Trx txRemove = new Trx(db)) {
							node.removeTag(tag);
							txRemove.success();
						}
						transformAndResponde(rc, node);
					}
				});
			});
		}
	}
}
