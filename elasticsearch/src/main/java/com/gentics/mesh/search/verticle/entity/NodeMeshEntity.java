package com.gentics.mesh.search.verticle.entity;

import static com.gentics.mesh.search.verticle.eventhandler.Util.requireType;

import java.util.Optional;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.event.role.PermissionChangedEventModelImpl;
import com.gentics.mesh.search.index.Transformer;
import com.gentics.mesh.search.index.node.NodeContainerTransformer;
import com.gentics.mesh.search.verticle.eventhandler.EventVertexMapper;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Dedicated entity definition for nodes. This class provides methods to transform database entities to documents and also to generate the partial update requests.
 */
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
				ev.getType()
			));
	}

	@Override
	public Optional<JsonObject> getPermissionPartial(PermissionChangedEventModelImpl event) {
		log.warn("permission partial for node requested. This should never happen", new Throwable());
		return Optional.empty();
	}
}
