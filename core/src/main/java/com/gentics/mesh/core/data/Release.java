package com.gentics.mesh.core.data;

import com.gentics.mesh.core.rest.release.ReleaseResponse;

/**
 * Interface for Release Vertex
 */
public interface Release extends MeshCoreVertex<ReleaseResponse, Release>, NamedElement {
	public static final String TYPE = "release";

	/**
	 * Get whether the release is active
	 * @return true for active release
	 */
	boolean isActive();

	/**
	 * Set whether the release is active
	 * @param active true for active
	 */
	void setActive(boolean active);

	/**
	 * Get the next Release
	 * @return next Release
	 */
	Release getNextRelease();

	/**
	 * Set the next Release
	 * @param release next Release
	 */
	void setNextRelease(Release release);

	/**
	 * Get the previous Release
	 * @return previous Release
	 */
	Release getPreviousRelease();
}
