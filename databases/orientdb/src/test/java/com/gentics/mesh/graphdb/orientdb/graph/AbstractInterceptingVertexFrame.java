//package com.gentics.mesh.graphdb.orientdb.graph;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import org.apache.tinkerpop.gremlin.structure.Element;
//import org.apache.tinkerpop.gremlin.structure.Vertex;
//
//import com.gentics.madl.orientdb3.wrapper.element.AbstractWrappedVertex;
//import com.gentics.madl.tx.Tx;
//import com.gentics.madl.wrapper.element.WrappedElement;
//import com.gentics.madl.wrapper.element.WrappedVertex;
//import com.gentics.mesh.graphdb.spi.LegacyDatabase;
//import com.syncleus.ferma.Database;
//import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;
//
///**
// * The intercepting vertex frame is used in order to be able to switch the currently active graph database context within a single vertex instance. This way a
// * framed vertex can be used across multiple transactions without the need to reload the instance from the graph. This is mainly achieved by overriding the
// * {@link #getGraph()} method and using the {@link LegacyDatabase#getThreadLocalGraph()} method instead.
// */
//public class AbstractInterceptingWrappedVertex extends AbstractWrappedVertex {
//
//	private Object id;
//	public ThreadLocal<Element> threadLocalElement = ThreadLocal.withInitial(() -> ((WrappedVertex) getGraph().getVertex(id)).getBaseElement());
//
//	@Override
//	protected void init() {
//		super.init();
//	}
//
//	@Override
//	protected void init(Tx tx, Element element) {
//		super.init(tx, element);
//		this.id = element.id();
//	}
//
//	/**
//	 * Return the properties which are prefixed using the given key.
//	 * 
//	 * @param prefix
//	 * @return
//	 */
//	public Map<String, String> getProperties(String prefix) {
//		Map<String, String> properties = new HashMap<>();
//
//		for (String key : keys()) {
//			if (key.startsWith(prefix)) {
//				properties.put(key, value(key));
//			}
//		}
//		return properties;
//	}
//
//	@SuppressWarnings("unchecked")
//	public Object getId() {
//		return id;
//	}
//
//	public void setLinkInTo(WrappedVertex vertex, String... labels) {
//		// Unlink all edges between both objects with the given label
//		unlinkIn(vertex, labels);
//		// Create a new edge with the given label
//		linkIn(vertex, labels);
//	}
//
//	public void setLinkOutTo(WrappedVertex vertex, String... labels) {
//		// Unlink all edges between both objects with the given label
//		unlinkOut(vertex, labels);
//		// Create a new edge with the given label
//		linkOut(vertex, labels);
//	}
//
//	public String getUuid() {
//		return value("uuid");
//	}
//
//	public void setUuid(String uuid) {
//		property("uuid", uuid);
//	}
//
//	public Vertex getVertex() {
//		return getElement();
//	}
//
//	public String getFermaType() {
//		return value(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY);
//	}
//
//	@Override
//	public Database getGraph() {
//		return Tx.get().getGraph();
//	}
//
//	@Override
//	public Vertex getElement() {
//		Element vertex = threadLocalElement.get();
//
//		// Unwrap wrapped vertex
//		if (vertex instanceof WrappedElement) {
//			vertex = (Vertex) ((WrappedElement) vertex).getBaseElement();
//		}
//		return (Vertex) vertex;
//	}
//
//}
