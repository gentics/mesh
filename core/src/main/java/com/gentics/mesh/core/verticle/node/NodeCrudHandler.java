package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.UPDATE_ACTION;
import static com.gentics.mesh.core.rest.common.GenericMessageResponse.message;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import org.elasticsearch.common.collect.Tuple;
import org.springframework.stereotype.Component;

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
import com.gentics.mesh.handler.InternalActionContext;

import rx.Observable;

@Component
public class NodeCrudHandler extends AbstractCrudHandler<Node, NodeResponse> {

	@Override
	public RootVertex<Node> getRootVertex(InternalActionContext ac) {
		return ac.getProject().getNodeRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		deleteElement(ac, () -> getRootVertex(ac), "uuid", "node_deleted");
	}

	public void handleDeleteLanguage(InternalActionContext ac) {
		db.asyncNoTrxExperimental(() -> {
			return getRootVertex(ac).loadObject(ac, "uuid", DELETE_PERM).flatMap(node -> {
				//TODO Don't we need a trx here?!
				String languageTag = ac.getParameter("languageTag");
				Language language = MeshRoot.getInstance().getLanguageRoot().findByLanguageTag(languageTag);
				if (language == null) {
					throw error(NOT_FOUND, "error_language_not_found", languageTag);
				}
				Observable<? extends Node> obs = node.deleteLanguageContainer(ac, language);
				return obs.map(updatedNode -> {
					return message(ac, "node_deleted_language", updatedNode.getUuid(), languageTag);
				});

			});
		}).subscribe(model -> ac.respond(model, OK), ac::fail);

	}

	public void handleMove(InternalActionContext ac) {
		db.asyncNoTrxExperimental(() -> {
			Project project = ac.getProject();
			// Load the node that should be moved
			String uuid = ac.getParameter("uuid");
			String toUuid = ac.getParameter("toUuid");

			Observable<Node> obsSourceNode = project.getNodeRoot().loadObject(ac, "uuid", UPDATE_PERM);
			Observable<Node> obsTargetNode = project.getNodeRoot().loadObject(ac, "toUuid", UPDATE_PERM);

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

	public void handleReadChildren(InternalActionContext ac) {
		db.asyncNoTrxExperimental(() -> {
			return getRootVertex(ac).loadObject(ac, "uuid", READ_PERM).map(node -> {
				try {
					PageImpl<? extends Node> page = node.getChildren(ac.getUser(), ac.getSelectedLanguageTags(), ac.getPagingParameter());
					return page.transformToRest(ac);
				} catch (Exception e) {
					throw error(INTERNAL_SERVER_ERROR, "Error while loading children of node {" + node.getUuid() + "}");
				}
			}).flatMap(x -> x);
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	public void readTags(InternalActionContext ac) {
		db.asyncNoTrxExperimental(() -> {
			return getRootVertex(ac).loadObject(ac, "uuid", READ_PERM).map(node -> {
				try {
					PageImpl<? extends Tag> tagPage = node.getTags(ac);
					return tagPage.transformToRest(ac);
				} catch (Exception e) {
					throw error(INTERNAL_SERVER_ERROR, "Error while loading tags for node {" + node.getUuid() + "}", e);
				}
			}).flatMap(x -> x);
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	public void handleAddTag(InternalActionContext ac) {
		db.asyncNoTrxExperimental(() -> {
			Project project = ac.getProject();
			Observable<Node> obsNode = project.getNodeRoot().loadObject(ac, "uuid", UPDATE_PERM);
			Observable<Tag> obsTag = project.getTagRoot().loadObject(ac, "tagUuid", READ_PERM);

			// TODO check whether the tag has already been assigned to the node. In this case we need to do nothing.
			Observable<Observable<NodeResponse>> obs = Observable.zip(obsNode, obsTag, (node, tag) -> {
				Tuple<SearchQueueBatch, Node> tuple = db.trx(() -> {
					node.addTag(tag);
					SearchQueueBatch batch = node.addIndexBatch(UPDATE_ACTION);
					return Tuple.tuple(batch, node);
				});

				SearchQueueBatch batch = tuple.v1();
				Node updatedNode = tuple.v2();
				return batch.process().flatMap(i -> updatedNode.transformToRest(ac));

			});
			return obs.flatMap(x -> x);

		}).subscribe(model -> ac.respond(model, OK), ac::fail);

	}

	public void handleRemoveTag(InternalActionContext ac) {
		db.asyncNoTrxExperimental(() -> {

			Project project = ac.getProject();
			Observable<Node> obsNode = project.getNodeRoot().loadObject(ac, "uuid", UPDATE_PERM);
			Observable<Tag> obsTag = project.getTagRoot().loadObject(ac, "tagUuid", READ_PERM);

			Observable<Observable<NodeResponse>> obs = Observable.zip(obsNode, obsTag, (node, tag) -> {
				Tuple<SearchQueueBatch, Node> tuple = db.trx(() -> {
					SearchQueueBatch batch = node.addIndexBatch(SearchQueueEntryAction.UPDATE_ACTION);
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

	public void handelReadBreadcrumb(InternalActionContext ac) {
		db.asyncNoTrxExperimental(() -> {
			Project project = ac.getProject();
			return project.getNodeRoot().loadObject(ac, "uuid", READ_PERM).flatMap(node -> {
				return node.transformToBreadcrumb(ac);
			});
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

}
