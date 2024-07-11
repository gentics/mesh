package com.gentics.mesh.core.data.branch;

import com.gentics.mesh.core.data.schema.SchemaVersion;

/**
 * Assignment between branch and schema version.
 */
public interface BranchSchemaVersion extends BranchVersionAssignment {

	/**
	 * Return the schema version.
	 * 
	 * @return
	 */
	SchemaVersion getSchemaContainerVersion();

}
