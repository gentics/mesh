package com.gentics.mesh.core.repository.action;

import com.gentics.mesh.core.data.model.Content;


public interface PathActions<T extends Content> {

	/**
	 * Finds and loads the object within the given project and path.
	 * 
	 * @param projectName
	 * @param path
	 * @return
	 */
	T findByProject(String projectName, String path);
}
