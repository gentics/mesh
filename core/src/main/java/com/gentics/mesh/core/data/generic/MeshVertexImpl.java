package com.gentics.mesh.core.data.generic;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.DelegatingFramedGraph;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedElement;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedVertex;

public class MeshVertexImpl extends AbstractVertexFrame implements MeshVertex {

	private Object id;
	public ThreadLocal<Element> threadLocalElement = ThreadLocal.withInitial(() -> ((WrappedVertex) getGraph().getVertex(id)).getBaseElement());

	@Override
	protected void init() {
		super.init();
		setProperty("uuid", UUIDUtil.randomUUID());
	}

	@Override
	protected void init(FramedGraph graph, Element element) {
		super.init(graph, element);
		this.id = element.getId();
	}

	/**
	 * Return the properties which are prefixed using the given key.
	 * 
	 * @param prefix
	 * @return
	 */
	public Map<String, String> getProperties(String prefix) {
		Map<String, String> properties = new HashMap<>();

		for (String key : getPropertyKeys()) {
			if (key.startsWith(prefix)) {
				properties.put(key, getProperty(key));
			}
		}
		return properties;
	}

	@SuppressWarnings("unchecked")
	public Object getId() {
		return id;
	}

//	@Override
//	public void unlinkIn(VertexFrame vertex, String... labels) {
//		for (Edge edge : this.getElement().getEdges(Direction.IN, labels)) {
//			if (edge.getVertex(Direction.IN).getId().equals(vertex.getId())) {
//				edge.remove();
//			}
//		}
//	}
//
//	@Override
//	public void unlinkOut(VertexFrame vertex, String... labels) {
//		for (Edge edge : this.getElement().getEdges(Direction.OUT, labels)) {
//			if (edge.getVertex(Direction.OUT).getId().equals(vertex.getId())) {
//				edge.remove();
//			}
//		}
//	}
//
//	@Override
//	public void setLinkOut(VertexFrame vertex, String... labels) {
//		for (String label : labels) {
//			for (Edge edge : this.getElement().getEdges(Direction.OUT, label)) {
//				edge.remove();
//			}
//			this.getElement().addEdge(label, vertex.getElement());
//		}
//	}
//
//	@Override
//	public void setLinkIn(VertexFrame vertex, String... labels) {
//		for (String label : labels) {
//			for (Edge edge : this.getElement().getEdges(Direction.IN, label)) {
//				edge.remove();
//			}
//			vertex.getElement().addEdge(label, this.getElement());
//		}
//	}
//
//	/**
//	 * Ensure that only one edge per label exists which connects both vertices.
//	 * 
//	 * @param vertex
//	 * @param labels
//	 */
//	public void setLinkOutTo(VertexFrame vertex, String... labels) {
//		Set<String> labelSet = new HashSet<>(Arrays.asList(labels));
//		// Unlink all edges between both objects with the given label that do not target the given vertex
//		for (Edge edge : this.getElement().getEdges(Direction.OUT, labels)) {
//			if (edge.getVertex(Direction.OUT).getId().equals(vertex.getElement().getId())) {
//				labelSet.remove(edge.getLabel());
//			} else {
//				edge.remove();
//			}
//		}
//
//		// Create a new edge for those labels that do not yet have a edge to the vertex 
//		for (String label : labelSet) {
//			this.getElement().addEdge(label, vertex.getElement());
//		}
//	}
//	//
//		public void setLinkInTo(VertexFrame vertex, String... labels) {
//			// Unlink all edges between both objects with the given label
//			unlinkIn(vertex, labels);
//			// Create a new edge with the given label
//			//linkIn(vertex, labels);
//			for (String label : labels) {
//				vertex.getElement().addEdge(label, this.getElement());
//			}
//		}


	public void setLinkInTo(VertexFrame vertex, String... labels) {
		// Unlink all edges between both objects with the given label
		unlinkIn(vertex, labels);
		// Create a new edge with the given label
		linkIn(vertex, labels);
	}

	public void setLinkOutTo(VertexFrame vertex, String... labels) {
		// Unlink all edges between both objects with the given label
		unlinkOut(vertex, labels);
		// Create a new edge with the given label
		linkOut(vertex, labels);
	}

	public String getUuid() {
		return getProperty("uuid");
	}

	public void setUuid(String uuid) {
		setProperty("uuid", uuid);
	}

	public Vertex getVertex() {
		return getElement();
	}

	public String getFermaType() {
		return getProperty(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY);
	}

	@Override
	public MeshVertexImpl getImpl() {
		return this;
	}

	@Override
	public FramedGraph getGraph() {
		return new DelegatingFramedGraph<>(Database.getThreadLocalGraph(), true, false);
	}

	@Override
	public void delete() {
		throw new NotImplementedException("The deletion behaviour for this vertex was not implemented.");
	}

	@Override
	public void applyPermissions(Role role, boolean recursive, Set<GraphPermission> permissionsToGrant, Set<GraphPermission> permissionsToRevoke) {
		role.grantPermissions(this, permissionsToGrant.toArray(new GraphPermission[permissionsToGrant.size()]));
		role.revokePermissions(this, permissionsToRevoke.toArray(new GraphPermission[permissionsToRevoke.size()]));
	}

	@Override
	public Vertex getElement() {
		Element vertex = threadLocalElement.get();

		// Unwrap wrapped vertex
		if (vertex instanceof WrappedElement) {
			vertex = (Vertex) ((WrappedElement) vertex).getBaseElement();
		}
		return (Vertex) vertex;
	}

	@Override
	public void reload() {
		MeshSpringConfiguration.getInstance().database().reload(this);
	}

}
