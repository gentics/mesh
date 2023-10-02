package com.gentics.mesh.search.verticle.eventhandler;

import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_BRANCH_ASSIGN;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_MIGRATION_FINISHED;
import static com.gentics.mesh.search.verticle.eventhandler.Util.requireType;
import static com.gentics.mesh.search.verticle.eventhandler.Util.toRequests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.DropIndexRequest;
import com.gentics.mesh.core.data.search.request.ReIndexRequest;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.branch.BranchMicroschemaAssignModel;
import com.gentics.mesh.core.rest.event.migration.MicroschemaMigrationMeshEventModel;
import com.gentics.mesh.search.index.node.NodeIndexHandlerImpl;
import com.gentics.mesh.search.verticle.MessageEvent;

import io.reactivex.Flowable;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Event handler for microschema migrations.
 * <ul>
 * <li>When a microschema is assigned to a branch, the indices for schemas using the microschema are created (with new microschema version hashes)</li>
 * <li>When a microschema migration is finished, the unused indices are dropped</li>
 * </ul>
 */
@Singleton
public class MicroschemaMigrationEventHandler implements EventHandler {
	private static final Logger log = LoggerFactory.getLogger(MicroschemaMigrationEventHandler.class);

	private final NodeIndexHandlerImpl nodeIndexHandler;
	private final MeshHelper helper;

	/**
	 * Create the instance
	 * @param nodeIndexHandler node index handler
	 * @param helper mesh helper
	 */
	@Inject
	public MicroschemaMigrationEventHandler(NodeIndexHandlerImpl nodeIndexHandler, MeshHelper helper) {
		this.nodeIndexHandler = nodeIndexHandler;
		this.helper = helper;
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Arrays.asList(MICROSCHEMA_BRANCH_ASSIGN, MICROSCHEMA_MIGRATION_FINISHED);
	}

	@Override
	public Flowable<? extends SearchRequest> handle(MessageEvent messageEvent) {
		return Flowable.defer(() -> {
			MeshEvent event = messageEvent.event;
			if (event == MICROSCHEMA_BRANCH_ASSIGN) {
				BranchMicroschemaAssignModel model = requireType(BranchMicroschemaAssignModel.class, messageEvent.message);
				return migrationStart(model);
			} else if (event == MICROSCHEMA_MIGRATION_FINISHED) {
				MicroschemaMigrationMeshEventModel model = requireType(MicroschemaMigrationMeshEventModel.class, messageEvent.message);
				return migrationEnd(model);
			} else {
				throw new RuntimeException("Unexpected event " + event.address);
			}
		});
	}

	/**
	 * Handle the start of a migration.
	 * <ol>
	 * <li>For all schemas in the branch, that "use" the microschema (i.e. have at least one micronode field that contains the microschema as "allowed", a request to
	 * create the new index (containing the new microschema version hash in the name) will be returned.</li>
	 * <li>After that, all documents, that do *not* contain a micronode of the microschema will be re-indexed by elasticsearch from the old index to the new index (documents, which contain a micronode of the microschema will be handled by the migration)</li>
	 * </ol>
	 * @param model rest model of the assignment
	 * @return flowable search requests
	 */
	private Flowable<? extends SearchRequest> migrationStart(BranchMicroschemaAssignModel model) {
		// get all schemas "using" the microschema (in the project/branch) and create schema create requests
		Pair<Map<String, Optional<IndexInfo>>, List<Triple<String, String, JsonObject>>> info = helper.getDb().transactional(tx -> {
			HibProject project = tx.projectDao().findByUuid(model.getProject().getUuid());
			HibBranch branch = tx.branchDao().findByUuid(project, model.getBranch().getUuid());
			HibMicroschema microschema = tx.microschemaDao().findByUuid(model.getSchema().getUuid());
			Map<String, Optional<IndexInfo>> indexMap = branch.findAllSchemaVersions().stream().filter(version -> version.usesMicroschema(microschema))
					.map(version -> nodeIndexHandler.getIndices(project, branch, version).runInExistingTx(tx))
					.collect(HashMap::new, HashMap::putAll, HashMap::putAll);

			// also add triples of old indices, new indices and restricting query for the reindex requests
			List<Triple<String, String, JsonObject>> reIndexTriples = new ArrayList<>();
			if (model.getOldSchema() != null) {
				Map<String, String> replacementMap = new HashMap<>();
				replacementMap.put(model.getOldSchema().getName(), model.getOldSchema().getVersionUuid());
				reIndexTriples.addAll(branch.findAllSchemaVersions().stream().filter(version -> version.usesMicroschema(microschema))
						.flatMap(version -> nodeIndexHandler.getReIndexTriples(project, branch, version, microschema, replacementMap)
								.runInExistingTx(tx).stream()).collect(Collectors.toList()));
			}

			return Pair.of(indexMap, reIndexTriples);
		}).runInNewTx();

		return toRequests(info.getLeft()).concatWith(reIndex(info.getRight()));
	}

	/**
	 * Handle the end of a migration by returning a flowable of index drop requests (for all old indices)
	 * @param model rest model of the event
	 * @return flowable of drop index requests
	 */
	private Flowable<DropIndexRequest> migrationEnd(MicroschemaMigrationMeshEventModel model) {
		// create drop index requests for the old schemas
		return helper.getDb().transactional(tx -> {
			HibProject project = tx.projectDao().findByUuid(model.getProject().getUuid());
			HibBranch branch = tx.branchDao().findByUuid(project, model.getBranch().getUuid());
			HibMicroschema microschema = tx.microschemaDao().findByUuid(model.getFromVersion().getUuid());

			// prepare the replacement map, which will replace the microschema version with the old microschema version, because we
			// need to find the names of the old indices (which used the old microschema version)
			Map<String, String> replacementMap = new HashMap<>();
			replacementMap.put(model.getFromVersion().getName(), model.getFromVersion().getVersionUuid());

			Set<String> oldIndexNames = branch.findAllSchemaVersions().stream().filter(version -> version.usesMicroschema(microschema))
					.flatMap(version -> nodeIndexHandler.getIndices(project, branch, version, replacementMap).runInExistingTx(tx).keySet().stream())
					.collect(Collectors.toSet());

			return Flowable.fromIterable(oldIndexNames).map(DropIndexRequest::new);
		}).runInNewTx();
	}

	/**
	 * For every triple in the given list, create a reindex request.
	 * Each triple consists of
	 * <ol>
	 * <li>Old index name (source of the reindex operation)</li>
	 * <li>New index name (destination of the reindex operation)</li>
	 * <li>elasticsearch query for restricting to documents, that do not contain the microschema</li>
	 * </ol>
	 * @param reIndexTriples list of triples
	 * @return flowable of reindex requests
	 */
	private Flowable<ReIndexRequest> reIndex(List<Triple<String, String, JsonObject>> reIndexTriples) {
		return Flowable.fromIterable(reIndexTriples).map(triple -> new ReIndexRequest(triple.getLeft(), triple.getMiddle(), triple.getRight()));
	}
}
