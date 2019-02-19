package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.core.data.search.request.CreateDocumentRequest;
import com.gentics.mesh.core.data.search.request.DeleteDocumentRequest;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import io.reactivex.Flowable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import static com.gentics.mesh.core.rest.MeshEvent.NODE_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UPDATED;
import static com.gentics.mesh.search.verticle.eventhandler.Util.requireType;
import static com.gentics.mesh.search.verticle.eventhandler.Util.toFlowable;

@Singleton
public class NodeHandler implements EventHandler {
	private final MeshHelper helper;
	private final MeshEntities entities;
	private final BootstrapInitializer boot;

	@Inject
	public NodeHandler(MeshHelper helper, MeshEntities entities, BootstrapInitializer boot) {
		this.helper = helper;
		this.entities = entities;
		this.boot = boot;
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Arrays.asList(NODE_CREATED, NODE_UPDATED, NODE_DELETED);
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent messageEvent) {
		MeshEvent event = messageEvent.event;
		NodeMeshEventModel message = requireType(NodeMeshEventModel.class, messageEvent.message);

		if (event == NODE_CREATED || event == NODE_UPDATED) {
			return toFlowable(upsertNodes(message));
		} else if (event == NODE_DELETED) {
			return Flowable.just(deleteNodes(message));
		} else {
			throw new RuntimeException("Unexpected event " + event.address);
		}
	}

	private Optional<SearchRequest> upsertNodes(NodeMeshEventModel message) {
		return helper.getDb().tx(() -> entities.node.getDocument(message))
			.map(doc -> helper.createDocumentRequest(
				getIndexName(message),
				NodeGraphFieldContainer.composeDocumentId(message.getUuid(), message.getLanguageTag()),
				doc
			));
	}

	private SearchRequest deleteNodes(NodeMeshEventModel message) {
		return helper.deleteDocumentRequest(
			getIndexName(message), NodeGraphFieldContainer.composeDocumentId(message.getUuid(), message.getLanguageTag())
		);
	}

	private String getIndexName(NodeMeshEventModel message) {
		return NodeGraphFieldContainer.composeIndexName(
			message.getProject().getUuid(),
			message.getBranchUuid(),
			getSchemaVersionUuid(message),
			ContainerType.forVersion(message.getType())
		);
	}

	private String getSchemaVersionUuid(NodeMeshEventModel message) {
		return helper.getDb().tx(() -> {
			SchemaContainer schema = boot.schemaContainerRoot().findByUuid(message.getUuid());
			return boot.projectRoot().findByUuid(message.getProject().getUuid())
				.getBranchRoot().findByUuid(message.getBranchUuid())
				.findLatestSchemaVersion(schema)
				.getUuid();
		});
	}
}
