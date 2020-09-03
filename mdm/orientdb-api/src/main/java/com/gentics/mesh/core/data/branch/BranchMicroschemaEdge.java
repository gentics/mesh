package com.gentics.mesh.core.data.branch;

import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;

/**
 * Edge between a branch and a microschema version.
 */
public interface BranchMicroschemaEdge extends BranchVersionEdge, HibBranchMicroschemaVersion {

	/**
	 * Return the corresponding microschema container version.
	 * 
	 * @return
	 */
	HibMicroschemaVersion getMicroschemaContainerVersion();

}
