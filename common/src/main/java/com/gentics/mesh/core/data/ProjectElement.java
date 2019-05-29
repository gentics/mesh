package com.gentics.mesh.core.data;

import com.gentics.mesh.graphdb.model.MeshElement;

/**
 * A project element is a mesh element that is located within a project. Elements such as tags, nodes, branches are such elements. 
 */
public interface ProjectElement extends MeshElement {
	
	Project getProject();

}
