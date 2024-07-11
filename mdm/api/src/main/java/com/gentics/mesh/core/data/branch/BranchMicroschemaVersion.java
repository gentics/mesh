package com.gentics.mesh.core.data.branch;

import com.gentics.mesh.core.data.schema.MicroschemaVersion;

/**
 * Assignment between branch and microschema version.
 */
public interface BranchMicroschemaVersion extends BranchVersionAssignment {

	/**
	 * Set the microschema version.
	 * 
	 * @return
	 */
	MicroschemaVersion getMicroschemaContainerVersion();

}
