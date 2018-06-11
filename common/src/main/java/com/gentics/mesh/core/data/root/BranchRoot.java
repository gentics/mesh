package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.User;

/**
 * Aggregation vertex for Releases.
 */
public interface BranchRoot extends RootVertex<Branch> {

	public static final String TYPE = "releases";

	/**
	 * Get the project of this release root.
	 * 
	 * @return
	 */
	Project getProject();

	/**
	 * Create a new release and make it the latest The new release will be the initial release, if it is the first created.
	 *
	 * @param name
	 *            release name
	 * @param creator
	 *            creator
	 * @return new Release
	 */
	default Branch create(String name, User creator) {
		return create(name, creator, null);
	}

	/**
	 * Create a new release and make it the latest The new release will be the initial release, if it is the first created.
	 *
	 * @param name
	 *            release name
	 * @param creator
	 *            creator
	 * @param uuid
	 *            Optional uuid
	 * @return new Release
	 */
	Branch create(String name, User creator, String uuid);

	/**
	 * Get the initial release of this root.
	 *
	 * @return
	 */
	Branch getInitialRelease();

	/**
	 * Get the latest release of this root.
	 *
	 * @return
	 */
	Branch getLatestRelease();

	/**
	 * Get the unique index key for names of releases attached to this root.
	 * 
	 * @param name
	 *            release name
	 * @return unique index key
	 */
	String getUniqueNameKey(String name);
}
