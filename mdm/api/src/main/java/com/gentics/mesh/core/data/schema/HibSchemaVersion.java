package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.result.Result;

public interface HibSchemaVersion extends HibFieldSchemaVersionElement<SchemaResponse, SchemaVersionModel, HibSchema, HibSchemaVersion> {

	// TODO MDM rename method
	HibSchema getSchemaContainer();

	// TODO MDM rename method
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
