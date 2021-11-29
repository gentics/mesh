package com.gentics.mesh.search.verticle.eventhandler.node;

import static com.gentics.mesh.core.rest.MeshEvent.NODE_TAGGED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UNTAGGED;
import static com.gentics.mesh.search.verticle.entity.MeshEntities.findElementByUuidStream;
import static com.gentics.mesh.search.verticle.eventhandler.Util.requireType;
import static com.gentics.mesh.search.verticle.eventhandler.Util.toFlowable;

import java.util.Arrays;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.node.NodeTaggedEventModel;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.entity.MeshEntities;
import com.gentics.mesh.search.verticle.eventhandler.EventHandler;
import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;

import io.reactivex.Flowable;

/**
 * Handler for node tagging events which will be processed into {@link SearchRequest} for Elasticsearch synchronization.
 */
@Singleton
public class NodeTagEventHandler implements EventHandler {

	private final MeshHelper helper;
	private final MeshEntities entities;

	@Inject
	public NodeTagEventHandler(MeshHelper helper, MeshEntities entities) {
		this.helper = helper;
		this.entities = entities;
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Arrays.asList(NODE_TAGGED, NODE_UNTAGGED);
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent messageEvent) {
		return Flowable.defer(() -> {
			NodeTaggedEventModel model = requireType(NodeTaggedEventModel.class, messageEvent.message);
			return helper.getDb().transactional(tx -> {
				// ProjectDaoWrapper projectDao = tx.projectDao();
				return findElementByUuidStream(helper.getBoot().projectDao(), model.getProject().getUuid())
					.flatMap(project -> findElementByUuidStream(helper.getBoot().branchDao(), project, model.getBranch().getUuid())
						.flatMap(branch -> entities.generateNodeRequests(model.getNode().getUuid(), project, branch)))
					.collect(toFlowable());
			}).runInNewTx();
		});
	}
}
