package com.gentics.mesh.search.verticle.entity;

import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.role.PermissionChangedEventModelImpl;
import com.gentics.mesh.search.index.Transformer;
import com.gentics.mesh.search.verticle.eventhandler.EventVertexMapper;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public abstract class MeshEntity<T> {
	protected final Transformer<T> transformer;
	private final TypeInfo typeInfo;
	private final EventVertexMapper<T> eventVertexMapper;

	public MeshEntity(Transformer<T> transformer, TypeInfo typeInfo, EventVertexMapper<T> eventVertexMapper) {
		this.transformer = transformer;
		this.typeInfo = typeInfo;
		this.eventVertexMapper = eventVertexMapper;
	}

	public Transformer<T> getTransformer() {
		return transformer;
	}

	public TypeInfo getTypeInfo() {
		return typeInfo;
	}

	public MeshEvent getCreateEvent() {
		return typeInfo.getOnCreated();
	}

	public MeshEvent getUpdateEvent() {
		return typeInfo.getOnUpdated();
	}

	public MeshEvent getDeleteEvent() {
		return typeInfo.getOnDeleted();
	}

	public List<MeshEvent> allEvents() {
		return Arrays.asList(getCreateEvent(), getUpdateEvent(), getDeleteEvent());
	}

	public JsonObject transform(T element) {
		return transformer.toDocument(element);
	}

	public Optional<T> getElement(MeshElementEventModel event) {
		return eventVertexMapper.apply(event);
	}

	public abstract Optional<JsonObject> getDocument(MeshElementEventModel event);

	public abstract Optional<JsonObject> getPermissionPartial(PermissionChangedEventModelImpl event);
}
