package com.gentics.mesh.search.verticle.eventhandler;

import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_BRANCH_ASSIGN;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_BRANCH_UNASSIGN;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_MIGRATION_FINISHED;
import static com.gentics.mesh.search.verticle.eventhandler.Util.requireType;
import static com.gentics.mesh.search.verticle.eventhandler.Util.toRequests;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.DropIndexRequest;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.branch.BranchSchemaAssignEventModel;
import com.gentics.mesh.core.rest.event.migration.SchemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.graphdb.spi.Transactional;
import com.gentics.mesh.search.index.node.NodeIndexHandler;
import com.gentics.mesh.search.verticle.MessageEvent;

import io.reactivex.Flowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class SchemaMigrationEventHandler implements EventHandler {
	private static final Logger log = LoggerFactory.getLogger(SchemaMigrationEventHandler.class);

	private final NodeIndexHandler nodeIndexHandler;
	private final MeshHelper helper;

	@Inject
	public SchemaMigrationEventHandler(NodeIndexHandler nodeIndexHandler, MeshHelper helper) {
		this.nodeIndexHandler = nodeIndexHandler;
		this.helper = helper;
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent messageEvent) {
		return Flowable.defer(() -> {
			MeshEvent event = messageEvent.event;
			if (event == SCHEMA_BRANCH_ASSIGN) {
				BranchSchemaAssignEventModel model = requireType(BranchSchemaAssignEventModel.class, messageEvent.message);
				return migrationStart(model);
			} else if (event == SCHEMA_BRANCH_UNASSIGN) {
				BranchSchemaAssignEventModel model = requireType(BranchSchemaAssignEventModel.class, messageEvent.message);
				return migrationEnd(
					model.getProject().getUuid(),
					model.getBranch().getUuid(),
					model.getSchema().getVersionUuid()
				);
			} else if (event == SCHEMA_MIGRATION_FINISHED) {
				SchemaMigrationMeshEventModel model = requireType(SchemaMigrationMeshEventModel.class, messageEvent.message);
				return migrationEnd(
					model.getProject().getUuid(),
					model.getBranch().getUuid(),
					model.getFromVersion().getVersionUuid()
				);
			} else {
				throw new RuntimeException("Unexpected event " + event.address);
			}
		});
	}

	private Flowable<DropIndexRequest> migrationEnd(String projectUuid, String branchUuid, String schemaVersionUuid) {
		return Flowable.just(new DropIndexRequest(NodeGraphFieldContainer.composeIndexPattern(
			projectUuid,
			branchUuid,
			schemaVersionUuid
		)));
	}

	public Flowable<SearchRequest> migrationStart(BranchSchemaAssignEventModel model) {
		Map<String, IndexInfo> map = helper.getDb().transactional(tx -> {
			Project project = Tx.get().data().projectDao().findByUuid(model.getProject().getUuid());
			Branch branch = project.getBranchRoot().findByUuid(model.getBranch().getUuid());
			SchemaContainerVersion schema = getNewSchemaVersion(model).runInExistingTx(tx);
			return nodeIndexHandler.getIndices(project, branch, schema).runInExistingTx(tx);
		}).runInNewTx();

		return toRequests(map);
	}

	private Transactional<SchemaContainerVersion> getNewSchemaVersion(BranchSchemaAssignEventModel model) {
		return helper.getDb().transactional(tx -> {
			SchemaReference schema = model.getSchema();
			SchemaContainer container = helper.getBoot().schemaContainerRoot().findByUuid(schema.getUuid());
			return container.findVersionByUuid(schema.getVersionUuid());
		});
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Arrays.asList(SCHEMA_BRANCH_ASSIGN, SCHEMA_BRANCH_UNASSIGN, SCHEMA_MIGRATION_FINISHED);
	}
}
