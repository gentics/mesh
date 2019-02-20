package com.gentics.mesh.search.verticle.entity;

import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.search.index.Transformer;
import com.gentics.mesh.search.index.node.NodeContainerTransformer;
import com.gentics.mesh.search.verticle.eventhandler.EventVertexMapper;
import io.vertx.core.json.JsonObject;

import java.util.Optional;

import static com.gentics.mesh.search.verticle.eventhandler.Util.requireType;

public class NodeMeshEntity extends MeshEntity<NodeGraphFieldContainer> {

	public NodeMeshEntity(Transformer<NodeGraphFieldContainer> transformer, MeshEvent createEvent, MeshEvent updateEvent, MeshEvent deleteEvent, EventVertexMapper<NodeGraphFieldContainer> eventVertexMapper) {
		super(transformer, createEvent, updateEvent, deleteEvent, eventVertexMapper);
	}

	@Override
	public Optional<JsonObject> getDocument(MeshElementEventModel event) {
		NodeMeshEventModel ev = requireType(NodeMeshEventModel.class, event);
		NodeContainerTransformer tf = requireType(NodeContainerTransformer.class, transformer);
		return getElement(event)
			.map(element -> tf.toDocument(
				element,
				ev.getBranchUuid(),
				ContainerType.forVersion(ev.getType())
			));
	}
}
