package com.gentics.cailun.core.repository.project.custom;

import com.gentics.cailun.core.rest.model.generic.GenericFile;

public interface PathActions<T extends GenericFile> {

	/**
	 * Finds and loads the object within the given project and path.
	 * 
	 * @param projectName
	 * @param path
	 * @return
	 */
	T findByProject(String projectName, String path);
}
