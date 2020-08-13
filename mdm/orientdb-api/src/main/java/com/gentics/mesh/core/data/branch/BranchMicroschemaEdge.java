package com.gentics.mesh.core.data.branch;

import com.gentics.mesh.core.data.schema.MicroschemaVersion;

/**
 * Edge between a branch and a microschema version.
 */
public interface BranchMicroschemaEdge extends BranchVersionEdge {

	/**
	 * Return the corresponding microschema container version.
	 * 
	 * @return
	 */
	MicroschemaVersion getMicroschemaContainerVersion();

}
