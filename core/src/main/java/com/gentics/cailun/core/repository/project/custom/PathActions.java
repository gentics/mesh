package com.gentics.cailun.core.repository.project.custom;

import com.gentics.cailun.core.rest.model.File;

public interface PathActions<T extends File> {

	/**
	 * Finds and loads the object within the given project and path.
	 * 
	 * @param projectName
	 * @param path
	 * @return
	 */
	T findByProject(String projectName, String path);
}
