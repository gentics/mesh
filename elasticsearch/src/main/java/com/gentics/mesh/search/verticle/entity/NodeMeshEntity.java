package com.gentics.mesh.search.verticle.entity;

import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.event.role.PermissionChangedEventModel;
import com.gentics.mesh.search.index.Transformer;
import com.gentics.mesh.search.index.node.NodeContainerTransformer;
import com.gentics.mesh.search.verticle.eventhandler.EventVertexMapper;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Optional;

import static com.gentics.mesh.search.verticle.eventhandler.Util.requireType;

public class NodeMeshEntity extends MeshEntity<NodeGraphFieldContainer> {

	private static final Logger log = LoggerFactory.getLogger(NodeMeshEntity.class);

	public NodeMeshEntity(Transformer<NodeGraphFieldContainer> transformer, EventVertexMapper<NodeGraphFieldContainer> eventVertexMapper) {
		super(transformer, Node.TYPE_INFO, eventVertexMapper);
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

	@Override
	public Optional<JsonObject> getPermissionPartial(PermissionChangedEventModel event) {
		log.warn("permission partial for node requested. This should never happen", new Throwable());
		return Optional.empty();
	}
}
