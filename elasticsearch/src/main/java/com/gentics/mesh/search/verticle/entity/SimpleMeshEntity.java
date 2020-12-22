package com.gentics.mesh.search.verticle.entity;

import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.role.PermissionChangedEventModelImpl;
import com.gentics.mesh.search.index.Transformer;
import com.gentics.mesh.search.verticle.eventhandler.EventVertexMapper;
import io.vertx.core.json.JsonObject;

import java.util.Optional;

/**
 * Entity definition which is used to manage document transformation, update handling, event handling.
 * 
 * @param <T> Database domain class type of the entity
 */
public class SimpleMeshEntity<T> extends MeshEntity<T> {

	public SimpleMeshEntity(Transformer<T> transformer, TypeInfo typeInfo, EventVertexMapper<T> eventVertexMapper) {
		super(transformer, typeInfo, eventVertexMapper);
	}

	@Override
	public Optional<JsonObject> getDocument(MeshElementEventModel event) {
		return getElement(event).map(transformer::toDocument);
	}

	@Override
	public Optional<JsonObject> getPermissionPartial(PermissionChangedEventModelImpl event) {
		return getElement(event).map(element -> transformer.toPermissionPartial((MeshCoreVertex<?>) element));
	}
}
