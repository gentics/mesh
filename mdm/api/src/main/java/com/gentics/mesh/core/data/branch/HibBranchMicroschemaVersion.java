package com.gentics.mesh.core.data.branch;

import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;

/**
 * Assignment between branch and microschema version.
 */
public interface HibBranchMicroschemaVersion extends HibBranchVersionAssignment {

	/**
	 * Set the microschema version.
	 * 
	 * @return
	 */
	HibMicroschemaVersion getMicroschemaContainerVersion();

}
