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

//	/**
//	 * Returns all the edges from the current Vertex to another one.
//	 *
//	 * @param target
//	 *            The target vertex
//	 * @param direction
//	 *            the direction of the edges to retrieve
//	 * @param labels
//	 *            the labels of the edges to retrieve
//	 * @return an iterable of incident edges
//	 */
//	Iterable<Edge> getEdges(MeshVertex target, Direction direction, String... labels);

	/**
	 * Grant the set of permissions and revoke the other set of permissions to this element using the role.
	 * 
	 * @param role
	 * @param recursive
	 * @param permissionsToGrant
	 * @param permissionsToRevoke
	 */
	void applyPermissions(Role role, boolean recursive, Set<GraphPermission> permissionsToGrant, Set<GraphPermission> permissionsToRevoke);

	/**
	 * Return the implementation for this element.
	 * 
	 * @return
	 */
	MeshVertexImpl getImpl();

}
