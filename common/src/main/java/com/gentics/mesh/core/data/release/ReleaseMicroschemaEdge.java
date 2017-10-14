package com.gentics.mesh.core.data.release;

import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;

/**
 * Edge between a release and a microschema version.
 */
public interface ReleaseMicroschemaEdge extends ReleaseVersionEdge {

	/**
	 * Return the corresponding microschema container version.
	 * 
	 * @return
	 */
	MicroschemaContainerVersion getMicroschemaContainerVersion();

}
