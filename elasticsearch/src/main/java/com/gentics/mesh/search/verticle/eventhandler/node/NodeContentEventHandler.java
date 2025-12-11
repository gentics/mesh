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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.search.Compliance;
import com.gentics.mesh.core.data.search.request.BulkRequest;
import com.gentics.mesh.core.data.search.request.CreateDocumentRequest;
import com.gentics.mesh.core.data.search.request.DeleteDocumentRequest;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.db.Transactional;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.event.migration.SchemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.entity.MeshEntities;
import com.gentics.mesh.search.verticle.eventhandler.EventCauseHelper;
import com.gentics.mesh.search.verticle.eventhandler.EventHandler;
import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;

import io.reactivex.Flowable;

/**
 * Handler for node content events which will be processed into {@link SearchRequest} for Elasticsearch synchronization.
 */
@Singleton
public class NodeContentEventHandler implements EventHandler {

	private static final Logger log = LoggerFactory.getLogger(NodeContentEventHandler.class);

	private final MeshHelper helper;
	private final MeshEntities entities;
	private final Compliance compliance;

	@Inject
	public NodeContentEventHandler(MeshHelper helper, MeshEntities entities, Compliance compliance) {
		this.helper = helper;
		this.entities = entities;
		this.compliance = compliance;
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
					HibSchemaVersion schemaVersion = findLatestSchemaVersion(message).runInNewTx();
					if (schemaVersion != null) {
						return Flowable.just(deleteNodes(message, helper.getDb().tx(() -> schemaVersion.getUuid())));
					} else {
						return Flowable.empty();
					}
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
			ContentDao.composeDocumentId(message.getUuid(), message.getLanguageTag()),
			doc, compliance));
	}

	private DeleteDocumentRequest deleteNodes(NodeMeshEventModel message, String schemaVersionUuid) {
		return helper.deleteDocumentRequest(
			getIndexName(message, schemaVersionUuid), ContentDao.composeDocumentId(message.getUuid(), message.getLanguageTag()),
			compliance);
	}

	private String getIndexName(NodeMeshEventModel message, String schemaVersionUuid) {
		return ContentDao.composeIndexName(
			message.getProject().getUuid(),
			message.getBranchUuid(),
			schemaVersionUuid,
			message.getType(),
			getIndexLanguage(message).runInNewTx(), getMicroschemaVersionHash(message, schemaVersionUuid));
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
			.mapInTx(HibBaseElement::getUuid);
	}

	private Transactional<HibSchemaVersion> findLatestSchemaVersion(NodeMeshEventModel message) {
		return helper.getDb().transactional(tx -> {
			HibSchema schema = tx.schemaDao().findByUuid(message.getSchema().getUuid());
			HibProject project = tx.projectDao().findByUuid(message.getProject().getUuid());
			if (project != null) {
				return tx.schemaDao().findLatestVersion(tx.branchDao().findByUuid(project, message.getBranchUuid()), schema);
			} else {
				log.warn("Could not find the project for UUID {" + message.getProject().getUuid() + "}");
				return getSchemaVersion(message.getSchema(), tx);
			}
		});
	}

	private HibSchemaVersion getSchemaVersion(SchemaReference reference, Tx tx) {
		SchemaDao schemaDao = tx.schemaDao();
		HibSchema schema = schemaDao.findByUuid(reference.getUuid());
		return schemaDao.findVersionByRev(schema, reference.getVersion());
	}

	private String getSchemaVersionUuid(SchemaReference reference) {
		return helper.getDb().tx(tx -> {
			return getSchemaVersion(reference, tx).getUuid();
		});
	}

	private String getMicroschemaVersionHash(NodeMeshEventModel message, String schemaVersionUuid) {
		return helper.getDb().tx(tx -> {
			HibSchema schema = tx.schemaDao().findByUuid(message.getSchema().getUuid());
			HibProject project = tx.projectDao().findByUuid(message.getProject().getUuid());
			if (project != null) {
				HibBranch branch = tx.branchDao().findByUuid(project, message.getBranchUuid());
				HibSchemaVersion schemaVersion = tx.schemaDao().findVersionByUuid(schema, schemaVersionUuid);
				return schemaVersion.getMicroschemaVersionHash(branch);
			} else {
				log.warn("Could not find the project for UUID {" + message.getProject().getUuid() + "}");
				return null;
			}
		});
	}
}
