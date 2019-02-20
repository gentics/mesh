package com.gentics.mesh.search.verticle.entity;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.search.index.Transformer;
import com.gentics.mesh.search.verticle.eventhandler.EventVertexMapper;
import io.vertx.core.json.JsonObject;

import java.util.Optional;

public class SimpleMeshEntity<T> extends MeshEntity<T> {

	public SimpleMeshEntity(Transformer<T> transformer, MeshEvent createEvent, MeshEvent updateEvent, MeshEvent deleteEvent, EventVertexMapper<T> eventVertexMapper) {
		super(transformer, createEvent, updateEvent, deleteEvent, eventVertexMapper);
	}

	@Override
	public Optional<JsonObject> getDocument(MeshElementEventModel event) {
		return getElement(event).map(transformer::toDocument);
	}
}
