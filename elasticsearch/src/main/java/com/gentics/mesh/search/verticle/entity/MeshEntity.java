package com.gentics.mesh.search.verticle.entity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.role.PermissionChangedEventModelImpl;
import com.gentics.mesh.search.index.Transformer;
import com.gentics.mesh.search.verticle.eventhandler.EventVertexMapper;

import io.vertx.core.json.JsonObject;

/**
 * Useful functions for a type of elements in mesh.
 * @param <T>
 */
public abstract class MeshEntity<T> {

	protected final Transformer<T> transformer;
	private final TypeInfo typeInfo;
	private final EventVertexMapper<T> eventVertexMapper;

	public MeshEntity(Transformer<T> transformer, TypeInfo typeInfo, EventVertexMapper<T> eventVertexMapper) {
		this.transformer = transformer;
		this.typeInfo = typeInfo;
		this.eventVertexMapper = eventVertexMapper;
	}

	/**
	 * The elasticsearch transformer for this entity type.
	 * @return
	 */
	public Transformer<T> getTransformer() {
		return transformer;
	}

	/**
	 * The {@link TypeInfo} for this entity type.
	 * @return
	 */
	public TypeInfo getTypeInfo() {
		return typeInfo;
	}

	/**
	 * The event that will be emitted when an element of this type is created.
	 * @return
	 */
	public MeshEvent getCreateEvent() {
		return typeInfo.getOnCreated();
	}

	/**
	 * The event that will be emitted when an element of this type is updated.
	 * @return
	 */
	public MeshEvent getUpdateEvent() {
		return typeInfo.getOnUpdated();
	}

	/**
	 * The event that will be emitted when an element of this type is deleted.
	 * @return
	 */
	public MeshEvent getDeleteEvent() {
		return typeInfo.getOnDeleted();
	}

	/**
	 * A list of the events from
	 * <ul>
	 *     <li>{@link #getCreateEvent()}</li>
	 *     <li>{@link #getUpdateEvent()}</li>
	 *     <li>{@link #getDeleteEvent()}</li>
	 * </ul>
	 * @return
	 */
	public List<MeshEvent> allEvents() {
		return Arrays.asList(getCreateEvent(), getUpdateEvent(), getDeleteEvent());
	}

	/**
	 * Transforms an element to an elasticsearch document.
	 * 
	 * @param element
	 * @return
	 */
	public JsonObject transform(T element) {
		return transformer.toDocument(element);
	}

	/**
	 * Gets the element that is described in an event.
	 * @param event
	 * @return
	 */
	public Optional<T> getElement(MeshElementEventModel event) {
		return eventVertexMapper.apply(event);
	}

	/**
	 * Gets the elasticsearch document described in an event.
	 * @param tx 
	 * @param event
	 * @return
	 */
	public abstract Optional<JsonObject> getDocument(MeshElementEventModel event);

	/**
	 * Gets a partial permission document update for the element described in the given event.
	 * @param event
	 * @return
	 */
	public abstract Optional<JsonObject> getPermissionPartial(PermissionChangedEventModelImpl event);
}
