package com.gentics.mesh.parameter;

/**
 * An interface for the branch parameters.
 */
public interface BranchParameters extends ParameterProvider {

	public static final String BRANCH_QUERY_PARAM_KEY = "branch";

	/**
	 * Return the currently configured branch name.
	 * 
	 * @return
	 */
	default String getBranch() {
		return getParameter(BRANCH_QUERY_PARAM_KEY);
	}

	/**
	 * Set the branch by name or UUID.
	 *
	 * @param branch
	 *            name or uuid
	 * @return fluent API
	 */
	default BranchParameters setBranch(String branch) {
		setParameter(BRANCH_QUERY_PARAM_KEY, branch);
		return this;
	}
}
