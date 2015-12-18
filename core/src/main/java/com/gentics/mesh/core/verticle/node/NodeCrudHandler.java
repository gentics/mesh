package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.UPDATE_ACTION;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.error;
import static com.gentics.mesh.util.VerticleHelper.processOrFail;
import static com.gentics.mesh.util.VerticleHelper.transformAndRespond;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import org.elasticsearch.common.collect.Tuple;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

@Component
public class NodeCrudHandler extends AbstractCrudHandler<Node> {

	@Override
	public RootVertex<Node> getRootVertex(InternalActionContext ac) {
		return ac.getProject().getNodeRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		deleteElement(ac, () -> getRootVertex(ac), "uuid", "node_deleted");
	}

	public void handelDeleteLanguage(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			getRootVertex(ac).loadObject(ac, "uuid", DELETE_PERM, rh -> {
				if (ac.failOnError(rh)) {
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

	//	@Override
	//	public void handleUpdate(InternalActionContext ac) {
	//		updateElement(ac, "uuid", () -> ac.getProject().getNodeRoot());
	//	}
	//
	//	@Override
	//	public void handleRead(InternalActionContext ac) {
	//		readElement(ac, "uuid", () -> ac.getProject().getNodeRoot());
	//	}
	//
	//	@Override
	//	public void handleReadList(InternalActionContext ac) {
	//		readElementList(ac, () -> ac.getProject().getNodeRoot());
	//	}

	public void handleMove(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			Project project = ac.getProject();
			// Load the node that should be moved
			String uuid = ac.getParameter("uuid");
			String toUuid = ac.getParameter("toUuid");
			project.getNodeRoot().loadObject(ac, "uuid", UPDATE_PERM, sourceNodeHandler -> {
				if (ac.failOnError(sourceNodeHandler)) {
					project.getNodeRoot().loadObject(ac, "toUuid", UPDATE_PERM, targetNodeHandler -> {
						if (ac.failOnError(targetNodeHandler)) {
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

	public void handleReadChildren(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			Project project = ac.getProject();
			project.getNodeRoot().loadObject(ac, "uuid", READ_PERM, rh -> {
				if (ac.failOnError(rh)) {
					Node node = rh.result();
					try {
						Page<? extends Node> page = node.getChildren(ac.getUser(), ac.getSelectedLanguageTags(), ac.getPagingParameter());
						page.transformAndRespond(ac, OK);
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
			project.getNodeRoot().loadObject(ac, "uuid", READ_PERM, rh -> {
				if (ac.failOnError(rh)) {
					Node node = rh.result();
					try {
						Page<? extends Tag> tagPage = node.getTags(ac);
						tagPage.transformAndRespond(ac, OK);
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
				project.getNodeRoot().loadObject(ac, "uuid", UPDATE_PERM, rh -> {
					if (ac.failOnError(rh)) {
						Node node = rh.result();
						project.getTagRoot().loadObject(ac, "tagUuid", READ_PERM, th -> {
							// TODO check whether the tag has already been assigned to the node. In this case we need to do nothing.
							if (ac.failOnError(th)) {
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
											transformAndRespond(ac, ch.result(), OK);
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
			project.getNodeRoot().loadObject(ac, "uuid", UPDATE_PERM, rh -> {
				project.getTagRoot().loadObject(ac, "tagUuid", READ_PERM, srh -> {
					if (ac.failOnError(srh) && ac.failOnError(rh)) {
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
									transformAndRespond(ac, ch.result(), OK);
								} , txAdded.result().v2());
							}
						});
					}
				});
			});
		} , ac.errorHandler());
	}

	public void handelReadBreadcrumb(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			Project project = ac.getProject();
			project.getNodeRoot().loadObject(ac, "uuid", READ_PERM, rh -> {
				if (ac.failOnError(rh)) {
					Node node = rh.result();
					node.transformToBreadcrumb(ac, th -> {
						ac.send(JsonUtil.toJson(th.result()), OK);
					});
				}
			});
		} , ac.errorHandler());
	}

}
