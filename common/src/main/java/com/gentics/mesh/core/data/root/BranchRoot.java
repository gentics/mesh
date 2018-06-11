package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.User;

/**
 * Aggregation vertex for Branches.
 */
public interface BranchRoot extends RootVertex<Branch> {

	public static final String TYPE = "branches";

	/**
	 * Get the project of this branch root.
	 * 
	 * @return
	 */
	Project getProject();

	/**
	 * Create a new branch and make it the latest The new branch will be the initial branch, if it is the first created.
	 *
	 * @param name
	 *            branch name
	 * @param creator
	 *            creator
	 * @return new Branch
	 */
	default Branch create(String name, User creator) {
		return create(name, creator, null);
	}

	/**
	 * Create a new branch and make it the latest The new branch will be the initial branch, if it is the first created.
	 *
	 * @param name
	 *            branch name
	 * @param creator
	 *            creator
	 * @param uuid
	 *            Optional uuid
	 * @return new Branch
	 */
	Branch create(String name, User creator, String uuid);

	/**
	 * Get the initial branch of this root.
	 *
	 * @return
	 */
	Branch getInitialBranch();

	/**
	 * Get the latest branch of this root.
	 *
	 * @return
	 */
	Branch getLatestBranch();

	/**
	 * Get the unique index key for names of branches attached to this root.
	 * 
	 * @param name
	 *            branch name
	 * @return unique index key
	 */
	String getUniqueNameKey(String name);
}
