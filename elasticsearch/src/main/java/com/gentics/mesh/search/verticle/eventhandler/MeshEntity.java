package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.MeshEventModel;
import com.gentics.mesh.search.index.Transformer;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class MeshEntity<T> {
	private final Transformer<T> transformer;
	private final MeshEvent createEvent;
	private final MeshEvent updateEvent;
	private final MeshEvent deleteEvent;
	private final EventVertexMapper<T> eventVertexMapper;

	public MeshEntity(Transformer<T> transformer, MeshEvent createEvent, MeshEvent updateEvent, MeshEvent deleteEvent, EventVertexMapper<T> eventVertexMapper) {
		this.transformer = transformer;
		this.createEvent = createEvent;
		this.updateEvent = updateEvent;
		this.deleteEvent = deleteEvent;
		this.eventVertexMapper = eventVertexMapper;
	}

	public Transformer<T> getTransformer() {
		return transformer;
	}

	public MeshEvent getCreateEvent() {
		return createEvent;
	}

	public MeshEvent getUpdateEvent() {
		return updateEvent;
	}

	public MeshEvent getDeleteEvent() {
		return deleteEvent;
	}

	public List<MeshEvent> allEvents() {
		return Arrays.asList(createEvent, updateEvent, deleteEvent);
	}

	public JsonObject transform(T element) {
		return transformer.toDocument(element);
	}

	public Optional<T> getElement(MeshEventModel event) {
		return eventVertexMapper.apply(event);
	}

	public Optional<JsonObject> getDocument(MeshEventModel event) {
		return getElement(event).map(transformer::toDocument);
	}
}
