package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.search.index.Transformer;
import io.vertx.core.json.JsonObject;

public class MeshEntity<T extends MeshCoreVertex<? extends RestModel, T>> {
	private final Transformer<T> transformer;
	private final RootVertex<T> rootVertex;
	private final MeshEvent createEvent;
	private final MeshEvent updateEvent;
	private final MeshEvent deleteEvent;

	public MeshEntity(Transformer<T> transformer, RootVertex<T> rootVertex, MeshEvent createEvent, MeshEvent updateEvent, MeshEvent deleteEvent) {
		this.transformer = transformer;
		this.rootVertex = rootVertex;
		this.createEvent = createEvent;
		this.updateEvent = updateEvent;
		this.deleteEvent = deleteEvent;
	}

	public Transformer<T> getTransformer() {
		return transformer;
	}

	public RootVertex<T> getRootVertex() {
		return rootVertex;
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

	public JsonObject transform(T element) {
		return transformer.toDocument(element);
	}
}
