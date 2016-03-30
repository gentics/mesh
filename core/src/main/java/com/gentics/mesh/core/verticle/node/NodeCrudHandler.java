package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.UPDATE_ACTION;
import static com.gentics.mesh.core.rest.common.GenericMessageResponse.message;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import org.elasticsearch.common.collect.Tuple;
import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;

import rx.Observable;

@Component
public class NodeCrudHandler extends AbstractCrudHandler<Node, NodeResponse> {

	@Override
	public RootVertex<Node> getRootVertex(InternalActionContext ac) {
		return ac.getProject().getNodeRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac, String uuid) {
		HandlerUtilities.deleteElement(ac, () -> getRootVertex(ac), uuid, "node_deleted");
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
		db.asyncNoTrxExperimental(() -> {
			return getRootVertex(ac).loadObjectByUuid(ac, uuid, DELETE_PERM).flatMap(node -> {
				//TODO Don't we need a trx here?!

				Language language = MeshRoot.getInstance().getLanguageRoot().findByLanguageTag(languageTag);
				if (language == null) {
					throw error(NOT_FOUND, "error_language_not_found", languageTag);
				}
				Observable<? extends Node> obs = node.deleteLanguageContainer(ac, language);
				return obs.map(updatedNode -> {
					return message(ac, "node_deleted_language", uuid, languageTag);
				});

			});
		}).subscribe(model -> ac.respond(model, OK), ac::fail);

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

		db.asyncNoTrxExperimental(() -> {
			Project project = ac.getProject();
			// Load the node that should be moved

			Observable<Node> obsSourceNode = project.getNodeRoot().loadObjectByUuid(ac, uuid, UPDATE_PERM);
			Observable<Node> obsTargetNode = project.getNodeRoot().loadObjectByUuid(ac, toUuid, UPDATE_PERM);

			return Observable.zip(obsSourceNode, obsTargetNode, (sourceNode, targetNode) -> {
				// TODO Update SQB
				Observable<Void> obs1 = sourceNode.moveTo(ac, targetNode);
				Observable<GenericMessageResponse> obs2 = obs1.map(er -> {
					return message(ac, "node_moved_to", uuid, toUuid);
				});
				return obs2;
			}).flatMap(x -> x);
		}).subscribe(model -> ac.respond(model, OK), ac::fail);

	}

	public void handleNavigation(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		db.asyncNoTrxExperimental(() -> {
			return getRootVertex(ac).loadObjectByUuid(ac, uuid, READ_PERM).map(node -> {
				return node.transformToNavigation(ac);
			}).flatMap(x -> x);
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	public void handleReadChildren(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		db.asyncNoTrxExperimental(() -> {
			return getRootVertex(ac).loadObjectByUuid(ac, uuid, READ_PERM).map(node -> {
				try {
					PageImpl<? extends Node> page = node.getChildren(ac.getUser(), ac.getSelectedLanguageTags(), ac.getPagingParameter());
					return page.transformToRest(ac);
				} catch (Exception e) {
					throw error(INTERNAL_SERVER_ERROR, "Error while loading children of node {" + node.getUuid() + "}");
				}
			}).flatMap(x -> x);
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	public void readTags(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		db.asyncNoTrxExperimental(() -> {
			return getRootVertex(ac).loadObjectByUuid(ac, uuid, READ_PERM).map(node -> {
				try {
					PageImpl<? extends Tag> tagPage = node.getTags(ac.getPagingParameter());
					return tagPage.transformToRest(ac);
				} catch (Exception e) {
					throw error(INTERNAL_SERVER_ERROR, "Error while loading tags for node {" + node.getUuid() + "}", e);
				}
			}).flatMap(x -> x);
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	public void handleAddTag(InternalActionContext ac, String uuid, String tagUuid) {
		validateParameter(uuid, "uuid");
		validateParameter(tagUuid, "tagUuid");

		db.asyncNoTrxExperimental(() -> {
			Project project = ac.getProject();
			Observable<Node> obsNode = project.getNodeRoot().loadObjectByUuid(ac, uuid, UPDATE_PERM);
			Observable<Tag> obsTag = project.getTagRoot().loadObjectByUuid(ac, tagUuid, READ_PERM);

			// TODO check whether the tag has already been assigned to the node. In this case we need to do nothing.
			Observable<Observable<NodeResponse>> obs = Observable.zip(obsNode, obsTag, (node, tag) -> {
				Tuple<SearchQueueBatch, Node> tuple = db.trx(() -> {
					node.addTag(tag);
					SearchQueueBatch batch = node.createIndexBatch(UPDATE_ACTION);
					return Tuple.tuple(batch, node);
				});

				SearchQueueBatch batch = tuple.v1();
				Node updatedNode = tuple.v2();
				return batch.process().flatMap(i -> updatedNode.transformToRest(ac));

			});
			return obs.flatMap(x -> x);

		}).subscribe(model -> ac.respond(model, OK), ac::fail);

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

		db.asyncNoTrxExperimental(() -> {

			Project project = ac.getProject();
			Observable<Node> obsNode = project.getNodeRoot().loadObjectByUuid(ac, uuid, UPDATE_PERM);
			Observable<Tag> obsTag = project.getTagRoot().loadObjectByUuid(ac, tagUuid, READ_PERM);

			Observable<Observable<NodeResponse>> obs = Observable.zip(obsNode, obsTag, (node, tag) -> {
				Tuple<SearchQueueBatch, Node> tuple = db.trx(() -> {
					SearchQueueBatch batch = node.createIndexBatch(SearchQueueEntryAction.UPDATE_ACTION);
					node.removeTag(tag);
					return Tuple.tuple(batch, node);
				});

				SearchQueueBatch batch = tuple.v1();
				Node updatedNode = tuple.v2();

				return batch.process(ac).flatMap(i -> updatedNode.transformToRest(ac));

			});
			return obs.flatMap(x -> x);

		}).subscribe(model -> ac.respond(model, OK), ac::fail);

	}

}
