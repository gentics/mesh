package com.gentics.mesh.core.endpoint.node;

import com.gentics.madl.tx.TxAction1;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.endpoint.handler.AbstractCrudHandler;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.NotModifiedException;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.VersioningParameters;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.math.NumberUtils;

import javax.inject.Inject;

import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.event.Assignment.ASSIGNED;
import static com.gentics.mesh.event.Assignment.UNASSIGNED;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

/**
 * Main CRUD handler for the Node Endpoint.
 */
public class NodeCrudHandler extends AbstractCrudHandler<Node, NodeResponse> {

	private BootstrapInitializer boot;

	private static final Logger log = LoggerFactory.getLogger(NodeCrudHandler.class);


	@Inject
	public NodeCrudHandler(Database db, HandlerUtilities utils, BootstrapInitializer boot) {
		super(db, utils);
		this.boot = boot;
	}

	@Override
	public RootVertex<Node> getRootVertex(InternalActionContext ac) {
		return ac.getProject().getNodeRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		utils.syncTx(ac, () -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, DELETE_PERM);
			if (node.getProject().getBaseNode().getUuid().equals(node.getUuid())) {
				throw error(METHOD_NOT_ALLOWED, "node_basenode_not_deletable");
			}
			// Create the batch first since we can't delete the container and access it later in batch creation
			utils.bulkableAction(bac -> {
				node.deleteFromBranch(ac, ac.getBranch(), bac, false);
			});
		}, () -> ac.send(NO_CONTENT));
	}

	/**
	 * Delete a specific language from the node. Only the affected language fields will be removed.
	 * 
	 * @param ac
	 *            Action context
	 * @param uuid
	 *            Node to be updated
	 * @param languageTag
	 *            Language tag of the language which should be deleted.
	 */
	public void handleDeleteLanguage(InternalActionContext ac, String uuid, String languageTag) {
		validateParameter(uuid, "uuid");

		utils.syncTx(ac, () -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, DELETE_PERM);
			Language language = MeshInternal.get().boot().meshRoot().getLanguageRoot().findByLanguageTag(languageTag);
			if (language == null) {
				throw error(NOT_FOUND, "error_language_not_found", languageTag);
			}
			utils.bulkableAction(bac -> {;
				node.deleteLanguageContainer(ac, ac.getBranch(), languageTag, bac, true);
			});
		}, () -> ac.send(NO_CONTENT));
	}

	/**
	 * Move a node to another parent node.
	 * 
	 * @param ac
	 *            Action context
	 * @param uuid
	 *            Node to be moved
	 * @param toUuid
	 *            Target node of the node
	 */
	public void handleMove(InternalActionContext ac, String uuid, String toUuid) {
		validateParameter(uuid, "uuid");
		validateParameter(toUuid, "toUuid");

		utils.syncTx(ac, () -> {
			Project project = ac.getProject();

			// TODO Add support for moving nodes across projects.
			// This is tricky since the branch consistency must be taken care of
			// One option would be to delete all the version within the source project and create the in the target project
			// The needed schema versions would need to be present in the target project branch as well.

			// Load the node that should be moved
			NodeRoot nodeRoot = project.getNodeRoot();
			Node sourceNode = nodeRoot.loadObjectByUuid(ac, uuid, UPDATE_PERM);
			Node targetNode = nodeRoot.loadObjectByUuid(ac, toUuid, UPDATE_PERM);

			utils.eventAction(batch -> {
				sourceNode.moveTo(ac, targetNode, batch);
			});
		}, () -> ac.send(NO_CONTENT));

	}

	/**
	 * Handle the navigation request.
	 * 
	 * @param ac
	 *            Action context
	 * @param uuid
	 *            Uuid of the start node for which the navigation should be generated.
	 */
	public void handleNavigation(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		utils.rxSyncTx(ac, tx -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, READ_PERM);
			return node.transformToNavigation(ac);
		}, model -> ac.send(model, OK));
	}

	/**
	 * Handle a read children of node request.
	 * 
	 * @param ac
	 *            Action context
	 * @param uuid
	 *            Uuid of the node from which the children should be loaded.
	 */
	public void handleReadChildren(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		utils.rxSyncTx(ac, (tx) -> {
			NodeParameters nodeParams = ac.getNodeParameters();
			PagingParameters pagingParams = ac.getPagingParameters();
			VersioningParameters versionParams = ac.getVersioningParameters();
			GraphPermission requiredPermission = "published".equals(ac.getVersioningParameters().getVersion()) ? READ_PUBLISHED_PERM : READ_PERM;
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, requiredPermission);
			TransformablePage<? extends Node> page = node.getChildren(ac, nodeParams.getLanguageList(),
				ac.getBranch(node.getProject()).getUuid(), ContainerType.forVersion(versionParams.getVersion()), pagingParams);

			// Handle etag
			if (ac.getGenericParameters().getETag()) {
				String etag = page.getETag(ac);
				ac.setEtag(etag, true);
				if (ac.matches(etag, true)) {
					throw new NotModifiedException();
				}
			}
			return page.transformToRest(ac, 0);
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
	 *            Action context
	 * @param uuid
	 *            UUID of the node for which the tags should be loaded
	 */
	public void readTags(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		utils.rxSyncTx(ac, tx -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, READ_PERM);
			try {
				TransformablePage<? extends Tag> tagPage = node.getTags(ac.getUser(), ac.getPagingParameters(), ac.getBranch());
				// Handle etag
				if (ac.getGenericParameters().getETag()) {
					String etag = tagPage.getETag(ac);
					ac.setEtag(etag, true);
					if (ac.matches(etag, true)) {
						return Single.error(new NotModifiedException());
					}
				}
				return tagPage.transformToRest(ac, 0);
			} catch (Exception e) {
				throw error(INTERNAL_SERVER_ERROR, "Error while loading tags for node {" + node.getUuid() + "}", e);
			}
		}, model -> ac.send((RestModel) model, OK));
	}

	/**
	 * Handle the add tag request.
	 * 
	 * @param ac
	 *            Action context which also contains the branch information.
	 * @param uuid
	 *            Uuid of the node to which tags should be added.
	 * @param tagUuid
	 *            Uuid of the tag which should be added to the node.
	 */
	public void handleAddTag(InternalActionContext ac, String uuid, String tagUuid) {
		validateParameter(uuid, "uuid");
		validateParameter(tagUuid, "tagUuid");

		utils.rxSyncTx(ac, tx -> {
			Project project = ac.getProject();
			Branch branch = ac.getBranch();
			Node node = project.getNodeRoot().loadObjectByUuid(ac, uuid, UPDATE_PERM);
			Tag tag = boot.meshRoot().getTagRoot().loadObjectByUuid(ac, tagUuid, READ_PERM);

			if (node.hasTag(tag, branch)) {
				if (log.isDebugEnabled()) {
					log.debug("Node {{}} is already tagged with tag {{}}", node.getUuid(), tag.getUuid());
				}
			} else {
				utils.eventAction(batch -> {
					node.addTag(tag, branch);

					batch.add(node.onTagged(tag, branch, ASSIGNED));
				});
			}

			return node.transformToRest(ac, 0);
		}, model -> ac.send(model, OK));

	}

	/**
	 * Remove the specified tag from the node.
	 * 
	 * @param ac
	 *            Action context
	 * @param uuid
	 *            Uuid of the node from which the tag should be removed
	 * @param tagUuid
	 *            Uuid of the tag which should be removed from the tag
	 */
	public void handleRemoveTag(InternalActionContext ac, String uuid, String tagUuid) {
		validateParameter(uuid, "uuid");
		validateParameter(tagUuid, "tagUuid");

		utils.syncTx(ac, () -> {
			Project project = ac.getProject();
			Branch branch = ac.getBranch();
			Node node = project.getNodeRoot().loadObjectByUuid(ac, uuid, UPDATE_PERM);
			Tag tag = boot.meshRoot().getTagRoot().loadObjectByUuid(ac, tagUuid, READ_PERM);

			if (node.hasTag(tag, branch)) {
				utils.eventAction(batch -> {
					node.removeTag(tag, branch);
					batch.add(node.onTagged(tag, branch, UNASSIGNED));
				});
			} else {
				if (log.isDebugEnabled()) {
					log.debug("Node {{}} was not tagged with tag {{}}", node.getUuid(), tag.getUuid());
				}
			}

		}, () -> ac.send(NO_CONTENT));
	}

	/**
	 * Handle getting the publish status for the requested node.
	 * 
	 * @param ac
	 *            Action context
	 * @param uuid
	 *            Uuid of the node which will be queried
	 */
	public void handleGetPublishStatus(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		utils.syncTx(ac, (tx) -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, READ_PERM);
			return node.transformToPublishStatus(ac);
		}, model -> ac.send(model, OK));
	}

	/**
	 * Handle publishing a node.
	 * 
	 * @param ac
	 *            Action context
	 * @param uuid
	 *            UUid of the node which should be published
	 */
	public void handlePublish(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		utils.syncTx(ac, tx -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, PUBLISH_PERM);
			utils.bulkableAction(bac -> {
				node.publish(ac, bac);
			});
			return node.transformToPublishStatus(ac);
		}, model -> ac.send(model, OK));
	}

	/**
	 * Handle taking a node offline.
	 * 
	 * @param ac
	 *            Action context
	 * @param uuid
	 *            Uuid of node which should be taken offline
	 */
	public void handleTakeOffline(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		utils.syncTx(ac, () -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, PUBLISH_PERM);
			utils.bulkableAction(bac -> {
				node.takeOffline(ac, bac);
			});
		}, () -> ac.send(NO_CONTENT));
	}

	/**
	 * Handle getting the publish status for the requested language of the node.
	 * 
	 * @param ac
	 *            Action context
	 * @param uuid
	 *            Uuid of the node for which the status should be loaded
	 * @param languageTag
	 *            Language to check
	 */
	public void handleGetPublishStatus(InternalActionContext ac, String uuid, String languageTag) {
		validateParameter(uuid, "uuid");

		utils.syncTx(ac, tx -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, READ_PERM);
			return node.transformToPublishStatus(ac, languageTag);
		}, model -> ac.send(model, OK));
	}

	/**
	 * Handle publishing a language of the node.
	 * 
	 * @param ac
	 *            Action context
	 * @param uuid
	 *            Uuid of the node which should be published
	 * @param languageTag
	 *            Language of the node
	 */
	public void handlePublish(InternalActionContext ac, String uuid, String languageTag) {
		validateParameter(uuid, "uuid");

		utils.syncTx(ac, tx -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, PUBLISH_PERM);
			utils.bulkableAction(bac -> {
				node.publish(ac, bac, languageTag);
			});
			return node.transformToPublishStatus(ac, languageTag);
		}, model -> ac.send(model, OK));
	}

	/**
	 * Handle taking a language of the node offline.
	 * 
	 * @param ac
	 *            Action context
	 * @param uuid
	 *            Uuid of the node that should be handled
	 * @param languageTag
	 *            Language tag of the language variation which should be taken offline
	 */
	public void handleTakeOffline(InternalActionContext ac, String uuid, String languageTag) {
		validateParameter(uuid, "uuid");

		utils.syncTx(ac, () -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, PUBLISH_PERM);
			utils.bulkableAction(bac -> {
				Branch branch = ac.getBranch(ac.getProject());
				node.takeOffline(ac, bac, branch, languageTag);
			});
		}, () -> ac.send(NO_CONTENT));
	}

	/**
	 * Read a single node and respond with a transformed node.
	 * 
	 * @param ac
	 *            Action context
	 * @param uuid
	 *            Uuid of the node which should be read
	 * @param handler
	 *            Handler which provides the root vertex which will be used to locate the node
	 */
	protected void readElement(InternalActionContext ac, String uuid, TxAction1<RootVertex<Node>> handler) {
		validateParameter(uuid, "uuid");

		utils.syncTx(ac, (tx) -> {
			RootVertex<Node> root = handler.handle();
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
	 *            Action context
	 * @param nodeUuid
	 *            Uuid of the node which should be updated
	 */
	public void handleBulkTagUpdate(InternalActionContext ac, String nodeUuid) {
		validateParameter(nodeUuid, "nodeUuid");

		db.asyncTx(() -> {
			Project project = ac.getProject();
			Node node = project.getNodeRoot().loadObjectByUuid(ac, nodeUuid, UPDATE_PERM);
			TransformablePage<? extends Tag> page = utils.eventAction(batch -> {
				return node.updateTags(ac, batch);
			});

			return page.transformToRest(ac, 0);
		}).subscribe(model -> ac.send(model, OK), ac::fail);

	}

	/**
	 * Handle the list versions request.
	 * 
	 * @param ac
	 * @param uuid
	 */
	public void handleListVersions(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		utils.syncTx(ac, (tx) -> {
			Node node = getRootVertex(ac).loadObjectByUuid(ac, uuid, READ_PERM);
			return node.transformToVersionList(ac);
		}, model -> {
			ac.send(model, OK);
		});
	}
}
