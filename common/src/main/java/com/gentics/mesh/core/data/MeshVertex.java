package com.gentics.mesh.core.data;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.impl.DummyBulkActionContext;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.syncleus.ferma.VertexFrame;
import com.tinkerpop.blueprints.Vertex;

import java.util.Set;

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
	 * @param context
	 *            Deletion context which keeps track of the deletion process
	 */
	void delete(BulkActionContext context);

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
	 */
	void applyPermissions(SearchQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant,
		Set<GraphPermission> permissionsToRevoke);

	/**
	 * Tests if the {@link GraphPermission}s READ_PUBLISHED_PERM and READ_PUBLISHED can be set for this element.
	 * @return
	 */
	default boolean hasPublishPermissions() {
		return false;
	}

	/**
	 * Add a unique <b>out-bound</b> link to the given vertex for the given set of labels. Note that this method will effectively ensure that only one
	 * <b>out-bound</b> link exists between the two vertices for each label.
	 * 
	 * @param vertex
	 *            Target vertex
	 * @param labels
	 *            Labels to handle
	 */
	void setUniqueLinkOutTo(VertexFrame vertex, String... labels);

}
