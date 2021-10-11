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
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.result.Result;

/**
 * Each schema update is stored within a dedicated schema container version in order to be able to keep track of changes in between different schema container
 * versions.
 */
public interface HibSchemaVersion extends HibFieldSchemaVersionElement<SchemaResponse, SchemaVersionModel, HibSchema, HibSchemaVersion> {

	static final TypeInfo TYPE_INFO = new TypeInfo(SCHEMAVERSION, SCHEMA_CREATED, SCHEMA_UPDATED, SCHEMA_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Get container entity bound to this version.
	 */
	HibSchema getSchemaContainer();

	/**
	 * Bind container entity to this version.
	 */
	void setSchemaContainer(HibSchema container);

	/**
	 * Transform the version to a reference POJO.
	 * 
	 * @return
	 */
	SchemaReference transformToReference();

	/**
	 * Return the element version of the schema. Please note that this is not the schema version. The element version instead reflects the update history of the
	 * element.
	 * 
	 * @return
	 */
	String getElementVersion();

	/**
	 * Return jobs which reference the schema version.
	 * 
	 * @return
	 */
	Iterable<? extends HibJob> referencedJobsViaTo();

	/**
	 * Check the autopurge flag of the version.
	 * 
	 * @return
	 */
	boolean isAutoPurgeEnabled();

	/**
	 * Returns all nodes that the user has read permissions for.
	 *
	 * @param branchUuid Branch uuid
	 * @param user User to check permissions for
	 * @param type Container type
	 * @return
	 */
	Result<? extends HibNode> getNodes(String branchUuid, HibUser user, ContainerType type);
}
