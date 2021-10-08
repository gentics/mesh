package com.gentics.mesh.core.data.schema;

import static com.gentics.mesh.ElementType.SCHEMAVERSION;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_UPDATED;

import java.util.Iterator;
import java.util.stream.Stream;

import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.Bucket;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;

public interface SchemaVersion
		extends GraphFieldSchemaContainerVersion<SchemaResponse, SchemaVersionModel, SchemaReference, HibSchemaVersion, HibSchema>, HibSchemaVersion {

	static final TypeInfo TYPE_INFO = new TypeInfo(SCHEMAVERSION, SCHEMA_CREATED, SCHEMA_UPDATED, SCHEMA_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Returns an iterator for those {@link HibNodeFieldContainer}'s which can be edited by users. Those are draft and publish versions.
	 *
	 * @param branchUuid Branch Uuid
	 * @return
	 */
	Iterator<? extends NodeGraphFieldContainer> getDraftFieldContainers(String branchUuid);

	/**
	 * Return a stream for {@link HibNodeFieldContainer}'s that use this schema version and are versions for the given branch.
	 *
	 * @param branchUuid
	 *            branch Uuid
	 * @return
	 */
	Stream<? extends NodeGraphFieldContainer> getFieldContainers(String branchUuid);

	/**
	 * Return a stream for {@link HibNodeFieldContainer}'s that use this schema version, are versions of the given branch and are listed within the given bucket.
	 * @param branchUuid
	 * @param bucket
	 * @return
	 */
	Stream<? extends NodeGraphFieldContainer> getFieldContainers(String branchUuid, Bucket bucket);
}
