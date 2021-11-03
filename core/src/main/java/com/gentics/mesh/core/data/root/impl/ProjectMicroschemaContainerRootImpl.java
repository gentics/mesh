package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_ROOT;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.project.HibProject;

/**
 * Project specific implementation of microschema container root.
 */
public class ProjectMicroschemaContainerRootImpl extends MicroschemaContainerRootImpl {

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(ProjectMicroschemaContainerRootImpl.class, MeshVertexImpl.class);
	}

	/**
	 * Get the project.
	 * 
	 * @return project
	 */
	protected HibProject getProject() {
		return in(HAS_MICROSCHEMA_ROOT).has(ProjectImpl.class).nextOrDefaultExplicit(ProjectImpl.class, null);
	}
}
