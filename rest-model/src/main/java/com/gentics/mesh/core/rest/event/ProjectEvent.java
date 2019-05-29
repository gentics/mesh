package com.gentics.mesh.core.rest.event;

import com.gentics.mesh.core.rest.project.ProjectReference;

/**
 * Event which contains project information.
 */
public interface ProjectEvent extends MeshEventModel {

	/**
	 * Return the project reference.
	 * 
	 * @return
	 */
	ProjectReference getProject();

	/**
	 * Set the project reference.
	 * 
	 * @param project
	 */
	void setProject(ProjectReference project);

}
