package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.User;

/**
 * Aggregation vertex for Releases
 */
public interface ReleaseRoot extends RootVertex<Release> {
	/**
	 * Get the project of this release root
	 * @return
	 */
	Project getProject();

	/**
	 * Create a new release and make it the latest
	 * The new release will be the initial release, if it is the first created
	 *
	 * @param name release name
	 * @param creator creator
	 * @return new Release
	 */
	Release create(String name, User creator);

	/**
	 * Get the initial release of this root
	 *
	 * @return
	 */
	Release getInitialRelease();

	/**
	 * Get the latest release of this root
	 *
	 * @return
	 */
	Release getLatestRelease();
}
