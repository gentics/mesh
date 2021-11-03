package com.gentics.mesh.core.data.schema;

import java.util.stream.Stream;

import com.gentics.mesh.core.data.Bucket;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.result.Result;

public interface SchemaVersion
		extends GraphFieldSchemaContainerVersion<SchemaResponse, SchemaVersionModel, SchemaReference, HibSchemaVersion, HibSchema>, HibSchemaVersion {

	/**
	 * Returns a result for those {@link HibNodeFieldContainer}'s which can be edited by users. Those are draft and publish versions.
	 *
	 * @param branchUuid Branch Uuid
	 * @return
	 */
	Result<? extends HibNodeFieldContainer> getDraftFieldContainers(String branchUuid);

	/**
	 * Return a stream for {@link HibNodeFieldContainer}'s that use this schema version and are versions for the given branch.
	 *
	 * @param branchUuid
	 *            branch Uuid
	 * @return
	 */
	Stream<? extends HibNodeFieldContainer> getFieldContainers(String branchUuid);

	/**
	 * Return a stream for {@link HibNodeFieldContainer}'s that use this schema version, are versions of the given branch and are listed within the given bucket.
	 * @param branchUuid
	 * @param bucket
	 * @return
	 */
	Stream<? extends HibNodeFieldContainer> getFieldContainers(String branchUuid, Bucket bucket);
}
