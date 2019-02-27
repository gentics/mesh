package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.request.BulkRequest;
import com.gentics.mesh.core.data.search.request.CreateDocumentRequest;
import com.gentics.mesh.core.data.search.request.DeleteDocumentRequest;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.event.migration.SchemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.entity.MeshEntities;
import io.reactivex.Flowable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import static com.gentics.mesh.core.rest.MeshEvent.NODE_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UPDATED;
import static com.gentics.mesh.core.rest.event.EventCauseAction.SCHEMA_MIGRATION;
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
		return Flowable.defer(() -> {
			MeshEvent event = messageEvent.event;
			NodeMeshEventModel message = requireType(NodeMeshEventModel.class, messageEvent.message);

			if (event == NODE_CREATED || event == NODE_UPDATED) {
				EventCauseInfo cause = message.getCause();
				if (cause != null && cause.getAction() == SCHEMA_MIGRATION) {
					return migrationUpdate(message);
				} else {
					return toFlowable(upsertNodes(message));
				}
			} else if (event == NODE_DELETED) {
				return Flowable.just(deleteNodes(message, getSchemaVersionUuid(message)));
			} else {
				throw new RuntimeException("Unexpected event " + event.address);
			}
		});
	}

	private Flowable<SearchRequest> migrationUpdate(NodeMeshEventModel message) {
		SchemaMigrationMeshEventModel cause = (SchemaMigrationMeshEventModel) message.getCause();
		DeleteDocumentRequest delete = deleteNodes(message, getSchemaVersionUuid(cause.getFromVersion()));
		SearchRequest request = upsertNodes(message)
			// The requests are bulked together to make sure that these request are in the same bulk
			.<SearchRequest>map(req -> new BulkRequest(req, delete))
			.orElse(delete);
		return Flowable.just(request);
	}

	private Optional<CreateDocumentRequest> upsertNodes(NodeMeshEventModel message) {
		return helper.getDb().tx(() -> entities.node.getDocument(message))
			.map(doc -> helper.createDocumentRequest(
				getIndexName(message, getSchemaVersionUuid(message)),
				NodeGraphFieldContainer.composeDocumentId(message.getUuid(), message.getLanguageTag()),
				doc
			));
	}

	private DeleteDocumentRequest deleteNodes(NodeMeshEventModel message, String schemaVersionUuid) {
		return helper.deleteDocumentRequest(
			getIndexName(message, schemaVersionUuid), NodeGraphFieldContainer.composeDocumentId(message.getUuid(), message.getLanguageTag())
		);
	}

	private String getIndexName(NodeMeshEventModel message, String schemaVersionUuid) {
		return NodeGraphFieldContainer.composeIndexName(
			message.getProject().getUuid(),
			message.getBranchUuid(),
			schemaVersionUuid,
			ContainerType.forVersion(message.getType())
		);
	}

	private String getSchemaVersionUuid(NodeMeshEventModel message) {
		return helper.getDb().tx(() -> {
			SchemaContainer schema = boot.schemaContainerRoot().findByUuid(message.getSchema().getUuid());
			return boot.projectRoot().findByUuid(message.getProject().getUuid())
				.getBranchRoot().findByUuid(message.getBranchUuid())
				.findLatestSchemaVersion(schema)
				.getUuid();
		});
	}

	private String getSchemaVersionUuid(SchemaReference reference) {
		return helper.getDb().tx(() -> boot.schemaContainerRoot()
			.findByUuid(reference.getUuid())
			.findVersionByRev(reference.getVersion())
			.getUuid());
	}
}
