package com.gentics.mesh.core.data.branch;

import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;

/**
 * Edge between a branch and a microschema version.
 */
public interface BranchMicroschemaEdge extends BranchVersionEdge {

	/**
	 * Return the corresponding microschema container version.
	 * 
	 * @return
	 */
	MicroschemaContainerVersion getMicroschemaContainerVersion();

}
