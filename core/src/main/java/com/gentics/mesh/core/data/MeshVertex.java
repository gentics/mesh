package com.gentics.mesh.core.data;

import java.util.Set;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.tinkerpop.blueprints.Vertex;

/**
 * A mesh vertex is a mesh element that exposes various graph OGM specific methods. We use the interface abstraction in order to hide certain ferma methods
 * which would otherwise clutter the API.
 */
public interface MeshVertex extends MeshElement {

	/**
	 * Return the tinkerpop blueprint vertex of this mesh vertex.
	 * 
	 * @return Underlying vertex
	 */
	Vertex getVertex();

	/**
	 * Delete the element. Additional entries will be added to the batch to keep the search index in sync.
	 * 
	 * @param batch
	 *            Batch to be updated with new entries
	 */
	void delete(SearchQueueBatch batch);

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
	 * Return the implementation for this element which exposes various ferma methods.
	 * 
	 * @return
	 */
	MeshVertexImpl getImpl();

}
