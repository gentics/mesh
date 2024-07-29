package com.gentics.mesh.core.data.service;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.path.Path;

/**
 * The {@link WebRootService} is the starting point for webroot resolving operations.
 */
public interface WebRootService {

	/**
	 * Find the element that corresponds to the given project webroot path.
	 * 
	 * @param ac
	 *            Action context
	 * @param path
	 *            Path string
	 * @param type 
	 *            Selected type of the node content
	 * @return Resolved path object
	 */
	Path findByProjectPath(InternalActionContext ac, String path, ContainerType type);

	/**
	 * Find the graph field container within the given branch with the given path.
	 * 
	 * @param branchUuid
	 *            Branch to be used to scope the lookup
	 * @param path
	 *            Webroot path to use for the lookup
	 * @param type
	 *            Specific type to be looking for
	 * @return
	 */
	HibNodeFieldContainer findByUrlFieldPath(String branchUuid, String path, ContainerType type);

}
