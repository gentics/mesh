package com.gentics.mesh.cache;

import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.path.Path;

/**
 * Path cache for webroot paths.
 */
public interface WebrootPathCache extends MeshCache<String, Path> {

	/**
	 * Store a path in the cache.
	 * 
	 * @param project
	 *            Project for which the path is valid
	 * @param branch
	 *            Used branch
	 * @param type
	 *            Type of the resolved content
	 * @param path
	 *            Webroot path
	 * @param resolvedPath
	 *            Resolved webroot path to be put in the cache
	 */
	void store(HibProject project, HibBranch branch, ContainerType type, String path, Path resolvedPath);

	/**
	 * Check whether the cache has the path already stored.
	 * 
	 * @param project
	 * @param branch
	 * @param type
	 * @param path
	 * @return Path, if the path could be found in the cache. Otherwise null
	 */
	Path getPath(HibProject project, HibBranch branch, ContainerType type, String path);

}
