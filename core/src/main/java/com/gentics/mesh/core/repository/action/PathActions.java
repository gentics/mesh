package com.gentics.mesh.core.repository.action;

import com.gentics.mesh.core.data.model.MeshNode;


public interface PathActions<T extends MeshNode> {

	/**
	 * Finds and loads the object within the given project and path.
	 * 
	 * @param projectName
	 * @param path
	 * @return
	 */
	T findByProject(String projectName, String path);
}
