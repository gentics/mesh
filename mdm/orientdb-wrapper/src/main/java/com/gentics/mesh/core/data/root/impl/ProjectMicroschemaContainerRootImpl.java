package com.gentics.mesh.core.data.root.impl;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;

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
}
