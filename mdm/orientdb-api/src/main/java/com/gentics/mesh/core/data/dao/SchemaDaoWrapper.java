package com.gentics.mesh.core.data.dao;

import java.util.Iterator;
import java.util.stream.Stream;

import com.gentics.mesh.core.data.Bucket;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.root.SchemaRoot;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.result.Result;

/**
 * DAO for schema operation.
 */
public interface SchemaDaoWrapper extends SchemaDao {

	/**
	 * Return a list of all schema container roots to which the schema container was added.
	 *
	 * @return
	 */
	Result<? extends SchemaRoot> getRoots(HibSchema schema);

	/**
	 * Load the contents that use the given schema version for the given branch.
	 * 
	 * @param version
	 * @param branchUuid
	 * @return
	 */
	Iterator<? extends NodeGraphFieldContainer> findDraftFieldContainers(HibSchemaVersion version, String branchUuid);

	/**
	 * Return a stream for {@link NodeGraphFieldContainer}'s that use this schema version and are versions for the given branch.
	 * 
	 * @param version
	 * @param branchUuid
	 *            branch Uuid
	 * @return
	 */
	Stream<? extends NodeGraphFieldContainer> getFieldContainers(HibSchemaVersion version, String branchUuid);

	/**
	 * Return a stream for {@link NodeGraphFieldContainer}'s that use this schema version and are versions for the given branch.
	 * 
	 * @param version
	 * @param branchUuid
	 * @param bucket
	 *            Bucket to limit the selection by
	 * @return
	 */
	Stream<? extends NodeGraphFieldContainer> getFieldContainers(HibSchemaVersion version, String branchUuid, Bucket bucket);
}
