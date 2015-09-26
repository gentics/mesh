package com.gentics.mesh.core.data;

import java.util.Set;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.tinkerpop.blueprints.Vertex;

public interface MeshVertex extends MeshElement {

	/**
	 * Return the tinkerpop blueprint vertex of this mesh vertex.
	 * 
	 * @return
	 */
	Vertex getVertex();

	/**
	 * Delete the element.
	 */
	void delete();

	void applyPermissions(Role role, boolean recursive, Set<GraphPermission> permissionsToGrant, Set<GraphPermission> permissionsToRevoke);

	/**
	 * Return the implementation for this element.
	 * 
	 * @return
	 */
	MeshVertexImpl getImpl();

//	<T> T load();
}
