package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.migration.SchemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.graphdb.spi.Transactional;
import com.gentics.mesh.search.index.node.NodeIndexHandler;
import com.gentics.mesh.search.verticle.MessageEvent;
import io.reactivex.Flowable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_MIGRATION_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_MIGRATION_START;
import static com.gentics.mesh.search.verticle.eventhandler.Util.requireType;
import static com.gentics.mesh.search.verticle.eventhandler.Util.toRequests;

@Singleton
public class SchemaMigrationHandler implements EventHandler {

	private final NodeIndexHandler nodeIndexHandler;
	private final MeshHelper helper;

	@Inject
	public SchemaMigrationHandler(NodeIndexHandler nodeIndexHandler, MeshHelper helper) {
		this.nodeIndexHandler = nodeIndexHandler;
		this.helper = helper;
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent messageEvent) {
		if (messageEvent.event == SCHEMA_MIGRATION_START) {
			return migrationStart(messageEvent);
		} else if (messageEvent.event == SCHEMA_MIGRATION_FINISHED) {
			// TODO Implement
			throw new RuntimeException("Not implemented");
		} else {
			throw new RuntimeException("Unexpected event " + messageEvent.event.address);
		}
	}

	public Flowable<SearchRequest> migrationStart(MessageEvent messageEvent) {
		SchemaMigrationMeshEventModel model = requireType(SchemaMigrationMeshEventModel.class, messageEvent.message);
		Map<String, IndexInfo> map = helper.getDb().transactional(tx -> {
			Project project = helper.getBoot().projectRoot().findByUuid(model.getProject().getUuid());
			Branch branch = project.getBranchRoot().findByUuid(model.getBranch().getUuid());
			SchemaContainerVersion schema = getNewSchemaVersion(model).runInExistingTx(tx);
			return nodeIndexHandler.getIndices(project, branch, schema).runInExistingTx(tx);
		}).runInNewTx();

		return toRequests(map);
	}

	private Transactional<SchemaContainerVersion> getNewSchemaVersion(SchemaMigrationMeshEventModel model) {
		return helper.getDb().transactional(tx -> {
			SchemaReference schema = model.getToVersion();
			return helper.getBoot().schemaContainerRoot()
				.findByUuid(schema.getUuid())
				.findVersionByRev(schema.getVersion());
		});
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Arrays.asList(SCHEMA_MIGRATION_START, SCHEMA_MIGRATION_FINISHED);
	}
}
