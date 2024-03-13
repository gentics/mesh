package com.gentics.mesh.event.impl;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.EventCauseAction;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.event.EventCauseInfoImpl;
import com.gentics.mesh.core.rest.event.MeshEventModel;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see EventQueueBatch
 */
public class EventQueueBatchImpl implements EventQueueBatch {

	private static final Logger log = LoggerFactory.getLogger(EventQueueBatchImpl.class);

	private String batchId;

	private List<MeshEventModel> bulkEntries = new ArrayList<>();

	private List<Runnable> bulkActions = new ArrayList<>();

	private EventCauseInfo cause;

	private final Vertx vertx;

	private final MeshOptions options;

	@Inject
	public EventQueueBatchImpl(Vertx vertx, MeshOptions options) {
		this.vertx = vertx;
		this.options = options;
	}

	@Override
	public List<MeshEventModel> getEntries() {
		return bulkEntries;
	}

	@Override
	public List<Runnable> getActions() {
		return bulkActions;
	}

	@Override
	public void setCause(ElementType type, String uuid, EventCauseAction action) {
		this.cause = new EventCauseInfoImpl(type, uuid, action);
	}

	@Override
	public void setCause(EventCauseInfo cause) {
		this.cause = cause;
	}

	@Override
	public EventCauseInfo getCause() {
		return cause;
	}

	@Override
	public String getBatchId() {
		return batchId;
	}

	@Override
	public void dispatch() {
		EventBus eventbus = vertx.eventBus();
		// TODO buffer event dispatching?
		getEntries().forEach(entry -> {
			entry.setCause(getCause());
			MeshEvent event = entry.getEvent();
			if (log.isDebugEnabled()) {
				log.debug("Created event sent {}", event);
			}
			String json = JsonUtil.toJson(entry, false);
			if (log.isTraceEnabled()) {
				log.trace("Dispatching event '{}' with payload:\n{}", event, json);
			}
			eventbus.publish(event.getAddress(), new JsonObject(json),
					new DeliveryOptions().addHeader(SENDER_HEADER, options.getNodeName()));
		});
		getEntries().clear();

		getActions().forEach(action -> {
			action.run();
		});
		getActions().clear();
	}

}
