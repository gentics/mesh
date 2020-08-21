package com.gentics.mesh.search.verticle.eventhandler.node;

import static com.gentics.mesh.core.rest.MeshEvent.NODE_CONTENT_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_CONTENT_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_PUBLISHED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UNPUBLISHED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UPDATED;
import static com.gentics.mesh.core.rest.event.EventCauseAction.SCHEMA_MIGRATION;
import static com.gentics.mesh.search.verticle.eventhandler.Util.requireType;
import static com.gentics.mesh.search.verticle.eventhandler.Util.toFlowable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.search.request.BulkRequest;
import com.gentics.mesh.core.data.search.request.CreateDocumentRequest;
import com.gentics.mesh.core.data.search.request.DeleteDocumentRequest;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.event.migration.SchemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ComplianceMode;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.graphdb.spi.Transactional;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.entity.MeshEntities;
import com.gentics.mesh.search.verticle.eventhandler.EventCauseHelper;
import com.gentics.mesh.search.verticle.eventhandler.EventHandler;
import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;

import io.reactivex.Flowable;

@Singleton
public class NodeContentEventHandler implements EventHandler {
	private final MeshHelper helper;
	private final MeshEntities entities;
	private final ComplianceMode complianceMode;

	@Inject
	public NodeContentEventHandler(MeshHelper helper, MeshEntities entities, MeshOptions options) {
		this.helper = helper;
		this.entities = entities;
		this.complianceMode = options.getSearchOptions().getComplianceMode();
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Arrays.asList(NODE_CONTENT_CREATED, NODE_UPDATED, NODE_CONTENT_DELETED, NODE_PUBLISHED, NODE_UNPUBLISHED);
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent messageEvent) {
		return Flowable.defer(() -> {
			MeshEvent event = messageEvent.event;
			NodeMeshEventModel message = requireType(NodeMeshEventModel.class, messageEvent.message);

			switch (event) {
			case NODE_CONTENT_CREATED:
			case NODE_UPDATED:
			case NODE_PUBLISHED:
				EventCauseInfo cause = message.getCause();
				if (cause != null && cause.getAction() == SCHEMA_MIGRATION) {
					return migrationUpdate(message);
				} else {
					return toFlowable(upsertNodes(message));
				}
			case NODE_CONTENT_DELETED:
			case NODE_UNPUBLISHED:
				if (EventCauseHelper.isProjectDeleteCause(message)) {
					return Flowable.empty();
				} else {
					return Flowable.just(deleteNodes(message, getSchemaVersionUuid(message).runInNewTx()));
				}
			default:
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
		return helper.getDb().tx(tx -> {
			return entities.nodeContent.getDocument(message);
		}).map(doc -> helper.createDocumentRequest(
				getIndexName(message, getSchemaVersionUuid(message).runInNewTx()),
				ContentDaoWrapper.composeDocumentId(message.getUuid(), message.getLanguageTag()),
				doc, complianceMode));
	}

	private DeleteDocumentRequest deleteNodes(NodeMeshEventModel message, String schemaVersionUuid) {
		return helper.deleteDocumentRequest(
			getIndexName(message, schemaVersionUuid), ContentDaoWrapper.composeDocumentId(message.getUuid(), message.getLanguageTag()),
			complianceMode);
	}

	private String getIndexName(NodeMeshEventModel message, String schemaVersionUuid) {
		return ContentDaoWrapper.composeIndexName(
			message.getProject().getUuid(),
			message.getBranchUuid(),
			schemaVersionUuid,
			message.getType(),
			getIndexLanguage(message).runInNewTx());
	}

	private Transactional<String> getIndexLanguage(NodeMeshEventModel message) {
		return findLatestSchemaVersion(message)
			.mapInTx(schema -> schema.getSchema().findOverriddenSearchLanguages()
				.anyMatch(lang -> lang.equals(message.getLanguageTag()))
					? message.getLanguageTag()
					: null);
	}

	private Transactional<String> getSchemaVersionUuid(NodeMeshEventModel message) {
		return findLatestSchemaVersion(message)
			.mapInTx(MeshElement::getUuid);
	}

	private Transactional<SchemaVersion> findLatestSchemaVersion(NodeMeshEventModel message) {
		return helper.getDb().transactional(tx -> {
			Schema schema = tx.data().schemaDao().findByUuid(message.getSchema().getUuid());
			HibProject project = tx.data().projectDao().findByUuid(message.getProject().getUuid());
			return tx.data().branchDao().findByUuid(project, message.getBranchUuid())
				.findLatestSchemaVersion(schema);
		});
	}

	private String getSchemaVersionUuid(SchemaReference reference) {
		return helper.getDb().tx(tx -> {
			return tx.data().schemaDao()
				.findByUuid(reference.getUuid())
				.findVersionByRev(reference.getVersion())
				.getUuid();
		});
	}
}
