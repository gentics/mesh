package com.gentics.mesh.core.data.branch;

import com.gentics.mesh.core.data.schema.SchemaContainerVersion;

/**
 * Edge between release and schema version.
 */
public interface BranchSchemaEdge extends ReleaseVersionEdge {

	/**
	 * Return the corresponding schema container version.
	 * 
	 * @return
	 */
	SchemaContainerVersion getSchemaContainerVersion();

}
