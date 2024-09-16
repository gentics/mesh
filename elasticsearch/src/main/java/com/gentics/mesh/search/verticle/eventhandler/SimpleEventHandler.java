package com.gentics.mesh.search.verticle.eventhandler;

import static com.gentics.mesh.search.verticle.eventhandler.Util.requireType;
import static com.gentics.mesh.search.verticle.eventhandler.Util.toFlowable;

import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.etc.config.search.ComplianceMode;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.entity.MeshEntity;

import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An event handler that uses the events from {@link MeshEntity#allEvents()} and creates/updates/deletes documents according to
 * {@link MeshEntity#transform(Object)}
 * 
 * @param <T>
 */
@ParametersAreNonnullByDefault
public class SimpleEventHandler<T extends HibBaseElement> implements EventHandler {
	private static final Logger log = LoggerFactory.getLogger(SimpleEventHandler.class);

	private final MeshHelper helper;
	private final MeshEntity<T> entity;
	private final String indexName;
	private final ComplianceMode complianceMode;

	public SimpleEventHandler(MeshHelper helper, MeshEntity<T> entity, String indexName, ComplianceMode complianceMode) {
		this.helper = helper;
		this.entity = entity;
		this.indexName = indexName;
		this.complianceMode = complianceMode;
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return entity.allEvents();
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent eventModel) {
		return Flowable.defer(() -> {
			MeshEvent event = eventModel.event;
			MeshElementEventModel model = requireType(MeshElementEventModel.class, eventModel.message);

			if (event == entity.getCreateEvent() || event == entity.getUpdateEvent()) {
				return toFlowable(helper.getDb().tx(tx -> {
					return entity.getDocument(model);
				}).map(document -> helper.createDocumentRequest(
						indexName, model.getUuid(),
						document, complianceMode)));
			} else if (event == entity.getDeleteEvent()) {
				return Flowable.just(helper.deleteDocumentRequest(
					indexName, model.getUuid(), complianceMode));
			} else {
				throw new RuntimeException("Unexpected event " + event.address);
			}
		});
	}
}
