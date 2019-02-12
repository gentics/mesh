package com.gentics.mesh.core.rest.event;

import com.gentics.mesh.core.rest.project.ProjectReference;

/**
 * Event which contains project information.
 */
public interface ProjectEvent {

	ProjectReference getProject();

	void setProject(ProjectReference project);

}
