package com.gentics.mesh.core.data.release;

import com.gentics.mesh.core.data.schema.SchemaContainerVersion;

/**
 * Edge between release and schema version.
 */
public interface ReleaseSchemaEdge extends ReleaseVersionEdge {

	/**
	 * Return the corresponding schema container version.
	 * 
	 * @return
	 */
	SchemaContainerVersion getSchemaContainerVersion();

}
