package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.graphdb.model.MeshElement;

/**
 * A mesh edge is a mesh element that exposes various graph OGM specific methods. We use the interface abstraction in order to hide certain ferma methods which
 * would otherwise clutter the API.
 */
public interface MeshEdge extends MeshElement {

	/**
	 * Return the underlying ferma object.
	 * 
	 * @return
	 */
	MeshEdgeImpl getImpl();
}
