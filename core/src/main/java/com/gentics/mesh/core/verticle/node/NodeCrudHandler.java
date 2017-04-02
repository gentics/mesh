package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;

import org.apache.commons.lang3.math.NumberUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.NotModifiedException;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphdb.spi.TxHandler;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;

import io.netty.handler.codec.http.HttpResponseStatus;
import rx.Single;

/**
 * Main CRUD handler for the Node Endpoint.
 */
public class NodeCrudHandler extends AbstractCrudHandler<Node, NodeResponse> {

	private SearchQueue searchQueue;

	private BootstrapInitializer boot;

	@Inject
	public NodeCrudHandler(Database db, SearchQueue searchQueue, HandlerUtilities utils, BootstrapInitializer boot) {
		super(db, utils);
		this.searchQueue = searchQueue;
		this.boot = boot;
	}

	@Override
	public RootVertex<Node> getRootVertex(InternalActionContext ac) {
		return ac.getProject().getNodeRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		utils.operateNoTx(ac, () -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, DELETE_PERM);
			if (node.getProject().getBaseNode().getUuid().equals(node.getUuid())) {
				throw error(METHOD_NOT_ALLOWED, "node_basenode_not_deletable");
			}

			// Create the batch first since we can't delete the container and access it later in batch creation
			SearchQueueBatch batch = searchQueue.create();
			db.tx(() -> {
				node.deleteFromRelease(ac.getRelease(), batch, false);
				return batch;
			}).processSync();
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

		utils.operateNoTx(ac, () -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, DELETE_PERM);
			Language language = MeshInternal.get().boot().meshRoot().getLanguageRoot().findByLanguageTag(languageTag);
			if (language == null) {
				throw error(NOT_FOUND, "error_language_not_found", languageTag);
			}

			// Create the batch first since we can't delete the container and access it later in batch creation
			SearchQueueBatch batch = searchQueue.create();
			db.tx(() -> {
				node.deleteLanguageContainer(ac.getRelease(), language, batch);
				return batch;
			}).processSync();
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

		utils.operateNoTx(ac, () -> {
			Project project = ac.getProject();

			// Load the node that should be moved
			Node sourceNode = project.getNodeRoot().loadObjectByUuid(ac, uuid, UPDATE_PERM);
			Node targetNode = project.getNodeRoot().loadObjectByUuid(ac, toUuid, UPDATE_PERM);

			SearchQueueBatch batch = searchQueue.create();
			db.tx(() -> {
				sourceNode.moveTo(ac, targetNode, batch);
				return batch;
			}).processSync();
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

		db.operateNoTx(() -> {
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

		utils.operateNoTx(ac, () -> {
			NodeParametersImpl nodeParams = ac.getNodeParameters();
			PagingParametersImpl pagingParams = ac.getPagingParameters();
			VersioningParametersImpl versionParams = ac.getVersioningParameters();
			GraphPermission requiredPermission = "published".equals(ac.getVersioningParameters().getVersion()) ? READ_PUBLISHED_PERM : READ_PERM;
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, requiredPermission);
			Page<? extends Node> page = node.getChildren(ac.getUser(), nodeParams.getLanguageList(), ac.getRelease(node.getProject()).getUuid(),
					ContainerType.forVersion(versionParams.getVersion()), pagingParams);
			// Handle etag
			String etag = page.getETag(ac);
			ac.setEtag(etag, true);
			if (ac.matches(etag, true)) {
				throw new NotModifiedException();
			} else {
				return page.transformToRest(ac, 0).toBlocking().value();
			}
		}, model -> ac.send(model, OK));

	}

	public void handleRead(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		GraphPermission requiredPermission = "published".equals(ac.getVersioningParameters().getVersion()) ? READ_PUBLISHED_PERM : READ_PERM;
		utils.readElement(ac, uuid, () -> getRootVertex(ac), requiredPermission);
	}

	/**
	 * Handle the read node tags request.
	 * 
	 * @param ac
	 * @param uuid
	 *            UUID of the node for which the tags should be loaded
	 */
	public void readTags(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		db.operateNoTx(() -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, READ_PERM);
			try {
				Page<? extends Tag> tagPage = node.getTags(ac.getUser(), ac.getPagingParameters(), ac.getRelease());
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

		db.operateNoTx(() -> {
			Project project = ac.getProject();
			Release release = ac.getRelease();
			Node node = project.getNodeRoot().loadObjectByUuid(ac, uuid, UPDATE_PERM);
			Tag tag = boot.meshRoot().getTagRoot().loadObjectByUuid(ac, tagUuid, READ_PERM);

			// TODO check whether the tag has already been assigned to the node. In this case we need to do nothing.
			SearchQueueBatch batch = searchQueue.create();
			Node updatedNode = db.tx(() -> {
				node.addTag(tag, release);
				batch.store(node, release.getUuid(), PUBLISHED, false);
				batch.store(node, release.getUuid(), DRAFT, false);
				return node;
			});
			return batch.processAsync().andThen(updatedNode.transformToRest(ac, 0));
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

		db.operateNoTx(() -> {
			Project project = ac.getProject();
			Release release = ac.getRelease();
			Node node = project.getNodeRoot().loadObjectByUuid(ac, uuid, UPDATE_PERM);
			Tag tag = boot.meshRoot().getTagRoot().loadObjectByUuid(ac, tagUuid, READ_PERM);

			SearchQueueBatch batch = searchQueue.create();
			db.tx(() -> {
				batch.store(node, release.getUuid(), PUBLISHED, false);
				batch.store(node, release.getUuid(), DRAFT, false);
				node.removeTag(tag, release);
				return node;
			});
			return batch.processAsync().andThen(Single.just(null));
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

		utils.operateNoTx(ac, () -> {
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

		db.operateNoTx(() -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, PUBLISH_PERM);
			SearchQueueBatch batch = searchQueue.create();
			node.publish(ac, batch);
			node.reload();
			return batch.processAsync().andThen(Single.just(node.transformToPublishStatus(ac)));
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

		db.operateNoTx(() -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, PUBLISH_PERM);
			SearchQueueBatch batch = searchQueue.create();
			node.takeOffline(ac, batch);
			return batch.processAsync().andThen(Single.just(null));
		}).subscribe(model -> ac.send(NO_CONTENT), ac::fail);
	}

	/**
	 * Handle getting the publish status for the requested language of the node.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the node for which the status should be loaded
	 * @param languageTag
	 *            Language to check
	 */
	public void handleGetPublishStatus(InternalActionContext ac, String uuid, String languageTag) {
		validateParameter(uuid, "uuid");

		utils.operateNoTx(ac, () -> {
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

		db.operateNoTx(() -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, PUBLISH_PERM);
			SearchQueueBatch batch = searchQueue.create();
			node.publish(ac, batch, languageTag);
			node.reload();
			return batch.processAsync().andThen(Single.just(node.transformToPublishStatus(ac, languageTag)));
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

		db.operateNoTx(() -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, PUBLISH_PERM);
			SearchQueueBatch batch = searchQueue.create();
			node.takeOffline(ac, batch, languageTag);
			return batch.processAsync().andThen(Single.just(null));
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

		utils.operateNoTx(ac, () -> {
			RootVertex<Node> root = handler.call();
			GraphPermission requiredPermission = "published".equals(ac.getVersioningParameters().getVersion()) ? READ_PUBLISHED_PERM : READ_PERM;
			Node node = root.loadObjectByUuid(ac, uuid, requiredPermission);
			return node.transformToRestSync(ac, 0);
		}, model -> {
			HttpResponseStatus code = HttpResponseStatus.valueOf(NumberUtils.toInt(ac.data().getOrDefault("statuscode", "").toString(), OK.code()));
			ac.send(model, code);
		});

	}

	/**
	 * Handle a bulk tag update request.
	 * 
	 * @param ac
	 * @param nodeUuid
	 *            Uuid of the node which should be updated
	 */
	public void handleBulkTagUpdate(InternalActionContext ac, String nodeUuid) {
		validateParameter(nodeUuid, "nodeUuid");

		db.operateNoTx(() -> {
			Project project = ac.getProject();
			Node node = project.getNodeRoot().loadObjectByUuid(ac, nodeUuid, UPDATE_PERM);
			SearchQueueBatch batch = searchQueue.create();
			Page<? extends Tag> tags = node.updateTags(ac, batch);
			return batch.processAsync().andThen(tags.transformToRest(ac, 0));
		}).subscribe(model -> ac.send(model, OK), ac::fail);

	}
}
