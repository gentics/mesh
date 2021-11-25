package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.rest.branch.BranchResponse;

/**
 * Aggregation vertex for Branches.
 */
public interface BranchRoot extends RootVertex<Branch>, TransformableElementRoot<Branch, BranchResponse> {

	/**
	 * Get the project of this branch root.
	 * 
	 * @return
	 */
	HibProject getProject();

	/**
	 * Get the initial branch of this root.
	 *
	 * @return
	 */
	HibBranch getInitialBranch();

	/**
	 * Get the latest branch of this root.
	 *
	 * @return
	 */
	HibBranch getLatestBranch();

	/**
	 * Set the branch to be the latest in the branch hierarchy.
	 * 
	 * @param branch
	 */
	void setLatestBranch(Branch branch);

	/**
	 * Get the unique index key for names of branches attached to this root.
	 * 
	 * @param name
	 *            branch name
	 * @return unique index key
	 */
	String getUniqueNameKey(String name);

	/**
	 * Set the branch to be initial in the branch hierarchy.
	 * 
	 * @param branch
	 */
	void setInitialBranch(Branch branch);
}
