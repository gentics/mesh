package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.search.index.node.NodeIndexHandler;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.entity.MeshEntities;
import io.reactivex.Flowable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Map;

@Singleton
public class ProjectHandler implements EventHandler {

	private final EventHandler simpleHandler;
	private final NodeIndexHandler nodeIndexHandler;
	private final MeshHelper helper;

	@Inject
	public ProjectHandler(EventHandlerFactory eventHandlerFactory, NodeIndexHandler nodeIndexHandler, MeshHelper helper) {
		simpleHandler = eventHandlerFactory.createSimpleEventHandler(MeshEntities::getProject, Project.composeIndexName());
		this.nodeIndexHandler = nodeIndexHandler;
		this.helper = helper;
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent messageEvent) {
		return Flowable.merge(
			simpleHandler.handle(messageEvent),
			createIndices(messageEvent)
		);
	}

	private Flowable<SearchRequest> createIndices(MessageEvent messageEvent) {
		Map<String, IndexInfo> map = helper.getDb().transactional(tx -> {
			Project project = helper.getBoot().projectRoot().findByUuid(messageEvent.message.getUuid());
			return nodeIndexHandler.getIndices(project).runInExistingTx(tx);
		}).runInNewTx();

		return Flowable.fromIterable(map.values())
			.map(index -> provider -> provider.createIndex(index));
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return simpleHandler.handledEvents();
	}
}
