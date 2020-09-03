package com.gentics.mesh.core.data.branch;

import com.gentics.mesh.core.data.schema.HibSchemaVersion;

/**
 * Edge between branch and schema version.
 */
public interface BranchSchemaEdge extends BranchVersionEdge, HibBranchSchemaVersion {

	/**
	 * Return the corresponding schema container version.
	 * 
	 * @return
	 */
	HibSchemaVersion getSchemaContainerVersion();

}
