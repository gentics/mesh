package com.gentics.mesh.search.verticle.eventhandler;

import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.core.data.search.request.CreateDocumentRequest;
import com.gentics.mesh.core.data.search.request.DeleteDocumentRequest;
import com.gentics.mesh.core.data.search.request.SearchRequest;

import io.reactivex.Flowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static com.gentics.mesh.search.verticle.eventhandler.Util.toFlowable;

@ParametersAreNonnullByDefault
public class SimpleEventHandler<T extends MeshCoreVertex<? extends RestModel, T>> implements EventHandler {
	private static final Logger log = LoggerFactory.getLogger(SimpleEventHandler.class);

	private final MeshHelper helper;
	private final MeshEntity<T> entity;
	private final String indexName;

	public SimpleEventHandler(MeshHelper helper, MeshEntity<T> entity, String indexName) {
		this.helper = helper;
		this.entity = entity;
		this.indexName = indexName;
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return entity.allEvents();
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent eventModel) {
		MeshEvent event = eventModel.event;
		if (event == entity.getCreateEvent() || event == entity.getUpdateEvent()) {
			return toFlowable(helper.getDb().tx(() -> entity.getDocument(eventModel.message))
				.map(document -> helper.createDocumentRequest(
					indexName, eventModel.message.getUuid(),
					document
				)));
		} else if (event == entity.getDeleteEvent()) {
			return Flowable.just(helper.deleteDocumentRequest(
				indexName, eventModel.message.getUuid()
			));
		} else {
			throw new RuntimeException("Unexpected event " + event.address);
		}
	}
}
