package com.gentics.mesh.search.verticle.eventhandler.node;

import static com.gentics.mesh.core.rest.MeshEvent.NODE_MOVED;
import static com.gentics.mesh.search.verticle.entity.MeshEntities.findElementByUuidStream;
import static com.gentics.mesh.search.verticle.eventhandler.Util.requireType;
import static com.gentics.mesh.search.verticle.eventhandler.Util.toFlowable;

import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.node.NodeMovedEventModel;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.entity.MeshEntities;
import com.gentics.mesh.search.verticle.eventhandler.EventHandler;
import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;

import io.reactivex.Flowable;

/**
 * Handler for node move events which will be processed into {@link SearchRequest} for Elasticsearch synchronization.
 */
@Singleton
public class NodeMoveEventHandler implements EventHandler {
	private final MeshHelper helper;
	private final MeshEntities entities;

	@Inject
	public NodeMoveEventHandler(MeshHelper helper, MeshEntities entities) {
		this.helper = helper;
		this.entities = entities;
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Collections.singletonList(NODE_MOVED);
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent messageEvent) {
		return Flowable.defer(() -> {
			NodeMovedEventModel model = requireType(NodeMovedEventModel.class, messageEvent.message);
			return helper.getDb().transactional(tx -> {
				return findElementByUuidStream(helper.getBoot().projectDao(), model.getProject().getUuid())
					.flatMap(project -> findElementByUuidStream(helper.getBoot().branchDao(), model.getBranchUuid())
						.flatMap(branch -> entities.generateNodeRequests(model.getUuid(), project, branch)))
					.collect(toFlowable());
			}).runInNewTx();
		});
	}
}
