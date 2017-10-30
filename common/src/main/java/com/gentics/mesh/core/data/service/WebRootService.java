package com.gentics.mesh.core.data.service;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.path.Path;

public interface WebRootService {

	/**
	 * Find the element that corresponds to the given project webroot path.
	 * 
	 * @param ac
	 *            Action context
	 * @param path
	 *            Path string
	 * @return Resolved path object
	 */
	Path findByProjectPath(InternalActionContext ac, String path);

	/**
	 * Find the graph field container within the given release with the given path.
	 * 
	 * @param releaseUuid
	 *            Release to be used to scope the lookup
	 * @param path
	 *            Webroot path to use for the lookup
	 * @param type
	 *            Specific type to be looking for
	 * @return
	 */
	NodeGraphFieldContainer findByPath(String releaseUuid, String path, ContainerType type);

}
