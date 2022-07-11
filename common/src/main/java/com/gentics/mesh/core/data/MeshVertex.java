package com.gentics.mesh.core.data;

import java.util.Set;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.impl.DummyBulkActionContext;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.madl.frame.VertexFrame;
import com.tinkerpop.blueprints.Vertex;

/**
 * A mesh vertex is a mesh element that exposes various graph OGM specific methods. We use the interface abstraction in order to hide certain ferma methods
 * which would otherwise clutter the API.
 */
public interface MeshVertex extends MeshElement, VertexFrame {

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
	 * Grant the set of permissions and revoke the other set of permissions to this element using the role.
	 * 
	 * @param batch
	 * @param role
	 * @param recursive
	 * @param permissionsToGrant
	 * @param permissionsToRevoke
	 * @return true, iff permissions were effectively changed
	 */
	boolean applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant,
		Set<GraphPermission> permissionsToRevoke);

	/**
	 * Tests if the {@link GraphPermission}s READ_PUBLISHED_PERM and READ_PUBLISHED can be set for this element.
	 * @return
	 */
	default boolean hasPublishPermissions() {
		return false;
	}

	/**
	 * Sets the cached uuid for the vertex.
	 * @param uuid
	 */
	void setCachedUuid(String uuid);

}
