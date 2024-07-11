package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.project.Project;

/**
 * A project element is a mesh element that is located within a project. Elements such as tags, nodes, branches are such elements.
 */
public interface ProjectElement extends Element {

	/**
	 * Return the project of the element.
	 * 
	 * @return
	 */
	Project getProject();
}
