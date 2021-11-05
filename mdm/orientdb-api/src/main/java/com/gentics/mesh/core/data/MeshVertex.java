package com.gentics.mesh.core.data;

import java.util.Set;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.impl.DummyBulkActionContext;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.madl.frame.VertexFrame;
import com.tinkerpop.blueprints.Vertex;

/**
 * A mesh vertex is a mesh element that exposes various graph OGM specific methods. We use the interface abstraction in order to hide certain ferma methods
 * which would otherwise clutter the API.
 */
public interface MeshVertex extends MeshElement, VertexFrame, HibBaseElement {

	String UUID_KEY = "uuid";

	/**
	 * Return the tinkerpop blueprint vertex of this mesh vertex.
	 * 
	 * @return Underlying vertex
	 */
	Vertex getVertex();

	/**
	 * Delete the element. Additional entries will be added to the batch to keep the search index in sync.
	 * 
	 * @param bac
	 *            Deletion context which keeps track of the deletion process
	 */
	void delete(BulkActionContext bac);

	/**
	 * Invoke deletion without any given bulk action context.
	 */
	default void delete() {
		delete(new DummyBulkActionContext());
	}

	/**
	 * Sets the cached uuid for the vertex.
	 * @param uuid
	 */
	void setCachedUuid(String uuid);

	/**
	 * Set the role uuid for the given permission.
	 *
	 * @param permission
	 * @param allowedRoles
	 */
	void setRoleUuidForPerm(InternalPermission permission, Set<String> allowedRoles);

	/**
	 * Return set of role uuids for the given permission that were granted on the element.
	 *
	 * @param permission
	 * @return
	 */
	Set<String> getRoleUuidsForPerm(InternalPermission permission);
}
