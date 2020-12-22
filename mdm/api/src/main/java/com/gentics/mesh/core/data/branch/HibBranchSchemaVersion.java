package com.gentics.mesh.core.data.branch;

import com.gentics.mesh.core.data.schema.HibSchemaVersion;

/**
 * Assignment between branch and schema version.
 */
public interface HibBranchSchemaVersion extends HibBranchVersionAssignment {

	/**
	 * Return the schema version.
	 * 
	 * @return
	 */
	HibSchemaVersion getSchemaContainerVersion();

}
