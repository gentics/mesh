package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;

import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.core.data.IndexableElement;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphdb.spi.FieldType;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.annotations.GraphElement;
import com.syncleus.ferma.ext.AbstractInterceptingVertexFrame;
import com.syncleus.ferma.tx.Tx;
import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;

/**
 * @see MeshVertex
 */
@GraphElement
public class MeshVertexImpl extends AbstractInterceptingVertexFrame implements MeshVertex {

	private String uuid;

	public static void init(Database database) {
		database.addVertexType(MeshVertexImpl.class, null);
		database.addVertexIndex(MeshVertexImpl.class, true, "uuid", FieldType.STRING);
	}

	@Override
	protected void init() {
		super.init();
		setProperty("uuid", UUIDUtil.randomUUID());
	}

	@Override
	protected void init(FramedGraph graph, Element element) {
		super.init(graph, element);
	}

	/**
	 * Add a single link <b>in-bound</b> link to the given vertex. Note that this method will remove all other links to other vertices for the given labels and
	 * only create a single edge between both vertices per label.
	 * 
	 * @param vertex
	 *            Target vertex
	 * @param labels
	 *            Labels to handle
	 */
	public void setSingleLinkInTo(VertexFrame vertex, String... labels) {
		// Unlink all edges with the given label
		unlinkIn(null, labels);
		// Create a new edge with the given label
		linkIn(vertex, labels);
	}

	/**
	 * Add a unique <b>in-bound</b> link to the given vertex for the given set of labels. Note that this method will effectively ensure that only one
	 * <b>in-bound</b> link exists between the two vertices for each label.
	 * 
	 * @param vertex
	 *            Target vertex
	 * @param labels
	 *            Labels to handle
	 */
	public void setUniqueLinkInTo(VertexFrame vertex, String... labels) {
		// Unlink all edges between both objects with the given label
		unlinkIn(vertex, labels);
		// Create a new edge with the given label
		linkIn(vertex, labels);
	}

	/**
	 * Remove all out-bound edges with the given label from the current vertex and create a new new <b>out-bound</b> edge between the current and given vertex
	 * using the specified label. Note that only a single out-bound edge per label will be preserved.
	 * 
	 * @param vertex
	 *            Target vertex
	 * @param labels
	 *            Labels to handle
	 */
	public void setSingleLinkOutTo(VertexFrame vertex, String... labels) {
		// Unlink all edges with the given label
		unlinkOut(null, labels);
		// Create a new edge with the given label
		linkOut(vertex, labels);
	}

	@Override
	public void setUniqueLinkOutTo(VertexFrame vertex, String... labels) {
		// Unlink all edges between both objects with the given label
		unlinkOut(vertex, labels);
		// Create a new edge with the given label
		linkOut(vertex, labels);
	}

	public String getUuid() {
		// Return the locally stored uuid if possible. Otherwise load it from the graph.
		if (uuid == null) {
			this.uuid = getProperty("uuid");
		}
		return uuid;
	}

	public void setUuid(String uuid) {
		setProperty("uuid", uuid);
		this.uuid = uuid;
	}

	public String getFermaType() {
		return getProperty(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY);
	}

	@Override
	public FramedGraph getGraph() {
		return Tx.getActive().getGraph();
	}

	@Override
	public void delete(SearchQueueBatch batch) {
		throw new NotImplementedException("The deletion behaviour for this vertex was not implemented.");
	}

	@Override
	public void applyPermissions(SearchQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant,
		Set<GraphPermission> permissionsToRevoke) {
		role.grantPermissions(this, permissionsToGrant.toArray(new GraphPermission[permissionsToGrant.size()]));
		role.revokePermissions(this, permissionsToRevoke.toArray(new GraphPermission[permissionsToRevoke.size()]));
		if (this instanceof IndexableElement) {
			// Check whether the action affects read permissions. We only need to update the document in the index if the action affects those perms
			boolean grantReads = permissionsToGrant.contains(READ_PERM) || permissionsToGrant.contains(READ_PUBLISHED_PERM);
			boolean revokesRead = permissionsToRevoke.contains(READ_PERM) || permissionsToRevoke.contains(READ_PUBLISHED_PERM);
			if (grantReads || revokesRead) {
				batch.updatePermissions((IndexableElement) this);
			}
		}
	}

//	@Override
//	public Vertex getElement() {
//		// TODO FIXME We should store the element reference in a thread local map that is bound to the transaction. The references should be removed once the
//		// transaction finishes
//		FramedGraph fg = Tx.getActive().getGraph();
//		if (fg == null) {
//			throw new RuntimeException(
//				"Could not find thread local graph. The code is most likely not being executed in the scope of a transaction.");
//		}
//
//		Vertex vertexForId = fg.getVertex(id);
//		if (vertexForId == null) {
//			throw new RuntimeException("No vertex for Id {" + id + "} could be found within the graph");
//		}
//		Element vertex = ((WrappedVertex) vertexForId).getBaseElement();
//
//		// Unwrap wrapped vertex
//		if (vertex instanceof WrappedElement) {
//			vertex = (Vertex) ((WrappedElement) vertex).getBaseElement();
//		}
//		return (Vertex) vertex;
//	}

	@Override
	public String getElementVersion() {
		Vertex vertex = getElement();
		return MeshInternal.get().database().getElementVersion(vertex);
	}

}
