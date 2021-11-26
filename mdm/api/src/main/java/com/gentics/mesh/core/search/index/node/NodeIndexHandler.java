package com.gentics.mesh.core.search.index.node;

import java.util.Map;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.data.search.MoveDocumentEntry;
import com.gentics.mesh.core.data.search.bulk.BulkEntry;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.db.Transactional;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.schema.SchemaModel;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

/**
 * Index handler for node entities.
 */
public interface NodeIndexHandler extends IndexHandler<HibNode> {

	/**
	 * Validate the schema model and the included ES settings against ES by creating a index template.
	 * 
	 * @param schema
	 * @return
	 */
	Completable validate(SchemaModel schema);

	/**
	 * Return the mapping provider for nodes.
	 */
	NodeContainerMappingProvider getMappingProvider();

	/**
	 * Construct the full index settings using the provided schema and language as a source.
	 *
	 * @param schema
	 * @param language
	 * @return
	 */
	JsonObject createIndexSettings(SchemaModel schema, String language);

	/**
	 * Construct the full index settings using the provided schema.
	 *
	 * @param schema
	 * @return
	 */
	default JsonObject createIndexSettings(SchemaModel schema) {
		return createIndexSettings(schema, null);
	}

	/**
	 * Generate an elasticsearch document object from the given container and stores it in the search index.
	 * 
	 * @param container
	 * @param branchUuid
	 * @param type
	 * @return Single with affected index name
	 */
	Single<String> storeContainer(HibNodeFieldContainer container, String branchUuid, ContainerType type);

	/**
	 * Create a bulk entry for a move document queue entry.
	 * 
	 * @param entry
	 * @return
	 */
	Observable<BulkEntry> moveForBulk(MoveDocumentEntry entry);

	/**
	 * Return a map of indices for the given project and branch.
	 * 
	 * This will list all schema version, draft/published and language specific indices for the projct branch arrangement.
	 * 
	 * @param project
	 * @param branch
	 * @return
	 */
	Transactional<Map<String, IndexInfo>> getIndices(HibProject project, HibBranch branch);

	/**
	 * Return a transactional which produces a map that contains all indices that are needed for the given container version, project, branch arrangement.
	 * 
	 * @param project
	 * @param branch
	 * @param containerVersion
	 * @return
	 */
	Transactional<Map<String, IndexInfo>> getIndices(HibProject project, HibBranch branch,
			HibSchemaVersion containerVersion);
}
