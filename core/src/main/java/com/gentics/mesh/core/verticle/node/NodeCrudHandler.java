package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.verticle.handler.HandlerUtilities.operateNoTx;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;

import org.apache.commons.lang3.math.NumberUtils;
import org.elasticsearch.common.collect.Tuple;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.NotModifiedException;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphdb.spi.TxHandler;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;

import io.netty.handler.codec.http.HttpResponseStatus;
import rx.Single;

public class NodeCrudHandler extends AbstractCrudHandler<Node, NodeResponse> {

	@Inject
	public NodeCrudHandler(Database db) {
		super(db);
	}

	@Override
	public RootVertex<Node> getRootVertex(InternalActionContext ac) {
		return ac.getProject().getNodeRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		operateNoTx(ac, () -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, DELETE_PERM);
			if (node.getProject().getBaseNode().getUuid().equals(node.getUuid())) {
				throw error(METHOD_NOT_ALLOWED, "node_basenode_not_deletable");
			}

			db.tx(() -> {
				// Create the batch first since we can't delete the container and access it later in batch creation
				SearchQueue queue = MeshInternal.get().boot().meshRoot().getSearchQueue();
				SearchQueueBatch batch = queue.createBatch();
				node.deleteFromRelease(ac.getRelease(null), batch);
				return batch;
			}).process().await();
			return null;
		}, m -> ac.send(NO_CONTENT));
	}

	/**
	 * Delete a specific language from the node. Only the affected language fields will be removed.
	 * 
	 * @param ac
	 * @param uuid
	 *            Node to be updated
	 * @param languageTag
	 *            Language tag of the language which should be deleted.
	 */
	public void handleDeleteLanguage(InternalActionContext ac, String uuid, String languageTag) {
		validateParameter(uuid, "uuid");

		operateNoTx(ac, () -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, DELETE_PERM);
			Language language = MeshRoot.getInstance().getLanguageRoot().findByLanguageTag(languageTag);
			if (language == null) {
				throw error(NOT_FOUND, "error_language_not_found", languageTag);
			}

			db.tx(() -> {
				// Create the batch first since we can't delete the container and access it later in batch creation
				SearchQueueBatch batch = MeshInternal.get().boot().meshRoot().getSearchQueue().createBatch();
				node.deleteLanguageContainer(ac.getRelease(null), language, batch);
				return batch;
			}).process().await();
			return null;
		}, m -> ac.send(NO_CONTENT));
	}

	/**
	 * Move a node to another parent node.
	 * 
	 * @param ac
	 * @param uuid
	 *            Node to be moved
	 * @param toUuid
	 *            Target node of the node
	 */
	public void handleMove(InternalActionContext ac, String uuid, String toUuid) {
		validateParameter(uuid, "uuid");
		validateParameter(toUuid, "toUuid");

		operateNoTx(ac, () -> {
			Project project = ac.getProject();
			// Load the node that should be moved

			Node sourceNode = project.getNodeRoot().loadObjectByUuid(ac, uuid, UPDATE_PERM);
			Node targetNode = project.getNodeRoot().loadObjectByUuid(ac, toUuid, UPDATE_PERM);
			db.tx(() -> {
				SearchQueueBatch batch = MeshInternal.get().boot().meshRoot().getSearchQueue().createBatch();
				sourceNode.moveTo(ac, targetNode, batch);
				return batch;
			}).process().await();
			return null;
		}, m -> ac.send(NO_CONTENT));

	}

	/**
	 * Handle the navigation request.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the start node for which the navigation should be generated.
	 */
	public void handleNavigation(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		operateNoTx(() -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, READ_PERM);
			return node.transformToNavigation(ac);
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	/**
	 * Handle a read children of node request.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the node from which the children should be loaded.
	 */
	public void handleReadChildren(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		operateNoTx(ac, () -> {
			NodeParameters nodeParams = ac.getNodeParameters();
			PagingParameters pagingParams = ac.getPagingParameters();
			VersioningParameters versionParams = ac.getVersioningParameters();
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, READ_PERM);
			try {
				PageImpl<? extends Node> page = node.getChildren(ac.getUser(), nodeParams.getLanguageList(),
						ac.getRelease(node.getProject()).getUuid(), ContainerType.forVersion(versionParams.getVersion()), pagingParams);
				// Handle etag
				String etag = page.getETag(ac);
				ac.setEtag(etag, true);
				if (ac.matches(etag, true)) {
					throw new NotModifiedException();
				} else {
					return page.transformToRest(ac, 0).toBlocking().value();
				}
			} catch (Exception e) {
				throw error(INTERNAL_SERVER_ERROR, "Error while loading children of node {" + node.getUuid() + "}");
			}
		}, model -> ac.send(model, OK));

	}

	/**
	 * Handle the read node tags request.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the node for which the tags should be loaded
	 */
	public void readTags(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		operateNoTx(() -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, READ_PERM);
			try {
				PageImpl<? extends Tag> tagPage = node.getTags(ac.getRelease(null), ac.getPagingParameters());
				// Handle etag
				String etag = tagPage.getETag(ac);
				ac.setEtag(etag, true);
				if (ac.matches(etag, true)) {
					return Single.error(new NotModifiedException());
				} else {
					return tagPage.transformToRest(ac, 0);
				}
			} catch (Exception e) {
				throw error(INTERNAL_SERVER_ERROR, "Error while loading tags for node {" + node.getUuid() + "}", e);
			}
		}).subscribe(model -> ac.send((RestModel) model, OK), ac::fail);
	}

	/**
	 * Handle the add tag request.
	 * 
	 * @param ac
	 *            Action context which also contains the release information.
	 * @param uuid
	 *            Uuid of the node to which tags should be added.
	 * @param tagUuid
	 *            Uuid of the tag which should be added to the node.
	 */
	public void handleAddTag(InternalActionContext ac, String uuid, String tagUuid) {
		validateParameter(uuid, "uuid");
		validateParameter(tagUuid, "tagUuid");

		operateNoTx(() -> {
			Project project = ac.getProject();
			Release release = ac.getRelease(null);
			Node node = project.getNodeRoot().loadObjectByUuid(ac, uuid, UPDATE_PERM);
			Tag tag = project.getTagRoot().loadObjectByUuid(ac, tagUuid, READ_PERM);

			// TODO check whether the tag has already been assigned to the node. In this case we need to do nothing.
			Tuple<SearchQueueBatch, Node> tuple = db.tx(() -> {
				SearchQueue queue = MeshInternal.get().boot().meshRoot().getSearchQueue();
				SearchQueueBatch batch = queue.createBatch();
				node.addTag(tag, release);
				node.addIndexBatchEntry(batch, STORE_ACTION);
				return Tuple.tuple(batch, node);
			});
			SearchQueueBatch batch = tuple.v1();
			Node updatedNode = tuple.v2();
			return batch.process().andThen(updatedNode.transformToRest(ac, 0));
		}).subscribe(model -> ac.send(model, OK), ac::fail);

	}

	/**
	 * Remove the specified tag from the node.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the node from which the tag should be removed
	 * @param tagUuid
	 *            Uuid of the tag which should be removed from the tag
	 */
	public void handleRemoveTag(InternalActionContext ac, String uuid, String tagUuid) {
		validateParameter(uuid, "uuid");
		validateParameter(tagUuid, "tagUuid");

		operateNoTx(() -> {
			Project project = ac.getProject();
			Release release = ac.getRelease(null);
			Node node = project.getNodeRoot().loadObjectByUuid(ac, uuid, UPDATE_PERM);
			Tag tag = project.getTagRoot().loadObjectByUuid(ac, tagUuid, READ_PERM);

			SearchQueueBatch sqBatch = db.tx(() -> {
				SearchQueue queue = MeshInternal.get().boot().meshRoot().getSearchQueue();
				SearchQueueBatch batch = queue.createBatch();
				// TODO get release specific containers
				node.addIndexBatchEntry(batch, STORE_ACTION);
				node.removeTag(tag, release);
				return batch;
			});
			return sqBatch.process(ac).andThen(Single.just(null));
		}).subscribe(model -> ac.send(NO_CONTENT), ac::fail);
	}

	/**
	 * Handle getting the publish status for the requested node.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the node which will be queried
	 */
	public void handleGetPublishStatus(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		operateNoTx(ac, () -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, READ_PERM);
			return node.transformToPublishStatus(ac);
		}, model -> ac.send(model, OK));
	}

	/**
	 * Handle publishing a node.
	 * 
	 * @param ac
	 * @param uuid
	 *            UUid of the node which should be published
	 */
	public void handlePublish(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		operateNoTx(() -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, PUBLISH_PERM);
			return node.publish(ac).andThen(Single.defer(() -> {
				return db.noTx(() -> {
					node.reload();
					return Single.just(node.transformToPublishStatus(ac));
				});
			}));
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	/**
	 * Handle taking a node offline.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of node which should be taken offline
	 */
	public void handleTakeOffline(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		operateNoTx(() -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, PUBLISH_PERM);
			return node.takeOffline(ac).andThen(Single.defer(() -> {
				return db.noTx(() -> {
					node.reload();
					// return node.transformToPublishStatus(ac);
					return Single.just(null);
				});
			}));
		}).subscribe(model -> ac.send(NO_CONTENT), ac::fail);
	}

	/**
	 * Handle getting the publish status for the requested language of the node.
	 * 
	 * @param ac
	 * @param uuid
	 * @param languageTag
	 */
	public void handleGetPublishStatus(InternalActionContext ac, String uuid, String languageTag) {
		validateParameter(uuid, "uuid");
		operateNoTx(ac, () -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, READ_PERM);
			return node.transformToPublishStatus(ac, languageTag);
		}, model -> ac.send(model, OK));
	}

	/**
	 * Handle publishing a language of the node.
	 * 
	 * @param ac
	 * @param uuid
	 * @param languageTag
	 */
	public void handlePublish(InternalActionContext ac, String uuid, String languageTag) {
		validateParameter(uuid, "uuid");

		operateNoTx(() -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, PUBLISH_PERM);
			return node.publish(ac, languageTag).andThen(Single.defer(() -> {
				return db.noTx(() -> {
					node.reload();
					return Single.just(node.transformToPublishStatus(ac, languageTag));
				});
			}));
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	/**
	 * Handle taking a language of the node offline.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the node that should be handled
	 * @param languageTag
	 *            Language tag of the language variation which should be taken offline
	 */
	public void handleTakeOffline(InternalActionContext ac, String uuid, String languageTag) {
		validateParameter(uuid, "uuid");

		operateNoTx(() -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, PUBLISH_PERM);
			return node.takeOffline(ac, languageTag).toSingle(() -> {
				return db.noTx(() -> {
					node.reload();
					return Single.just(null);
				});
			}).flatMap(x -> x);
		}).subscribe(model -> ac.send(NO_CONTENT), ac::fail);
	}

	/**
	 * Read a single node and respond with a transformed node.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the node which should be read
	 * @param handler
	 *            Handler which provides the root vertex which will be used to locate the node
	 */
	protected void readElement(InternalActionContext ac, String uuid, TxHandler<RootVertex<Node>> handler) {
		validateParameter(uuid, "uuid");

		operateNoTx(ac, () -> {
			RootVertex<Node> root = handler.call();
			GraphPermission requiredPermission = "published".equals(ac.getVersioningParameters().getVersion()) ? READ_PUBLISHED_PERM : READ_PERM;
			Node node = root.loadObjectByUuid(ac, uuid, requiredPermission);
			return node.transformToRestSync(ac, 0);
		}, model -> {
			HttpResponseStatus code = HttpResponseStatus.valueOf(NumberUtils.toInt(ac.data().getOrDefault("statuscode", "").toString(), OK.code()));
			ac.send(model, code);
		});

	}
}
