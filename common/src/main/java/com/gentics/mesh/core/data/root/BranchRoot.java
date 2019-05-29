package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.event.EventQueueBatch;

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
	 * @param batch
	 * @return new Branch
	 */
	default Branch create(String name, User creator, EventQueueBatch batch) {
		return create(name, creator, null, true, getLatestBranch(), batch);
	}

	/**
	 * Create a new branch. The new branch will be the initial branch, if it is the first created.
	 *
	 * @param name
	 *            branch name
	 * @param creator
	 *            creator
	 * @param uuid
	 *            Optional uuid
	 * @param setLatest
	 *            True to make it the latest branch
	 * @param baseBranch
	 *            optional base branch. This can only be null if this is the first branch in the project.
	 * @param batch
	 * @return new Branch
	 */
	Branch create(String name, User creator, String uuid, boolean setLatest, Branch baseBranch, EventQueueBatch batch);

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
	 * Set the branch to be the latest branch
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
	 * Find the referenced branch. Return null, if reference is null or not filled. Throw an error if the referenced branch cannot be found.
	 * 
	 * @param reference
	 *            branch reference
	 * @return referenced branch
	 */
	Branch fromReference(BranchReference reference);
}
