package com.gentics.mesh.core.data.branch;

import com.gentics.mesh.core.data.schema.SchemaContainerVersion;

/**
 * Edge between branch and schema version.
 */
public interface BranchSchemaEdge extends BranchVersionEdge {

	/**
	 * Return the corresponding schema container version.
	 * 
	 * @return
	 */
	SchemaContainerVersion getSchemaContainerVersion();

}
