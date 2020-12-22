package com.gentics.mesh.core.data.changelog;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;

/**
 * @see ChangeMarkerVertex
 */
public class ChangeMarkerVertexImpl extends MeshVertexImpl implements ChangeMarkerVertex {

	/**
	 * Initialize the vertex type.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(ChangeMarkerVertexImpl.class, MeshVertexImpl.class);
	}

}
