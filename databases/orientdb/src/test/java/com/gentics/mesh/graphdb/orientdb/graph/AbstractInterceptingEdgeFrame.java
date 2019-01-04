//package com.gentics.mesh.graphdb.orientdb.graph;
//
//import org.apache.tinkerpop.gremlin.structure.Edge;
//import org.apache.tinkerpop.gremlin.structure.Element;
//import org.apache.tinkerpop.gremlin.structure.util.wrapped.WrappedElement;
//
//import com.gentics.madl.db.Database;
//import com.gentics.madl.orientdb3.wrapper.element.AbstractWrappedEdge;
//import com.gentics.madl.tx.Tx;
//import com.gentics.madl.wrapper.element.WrappedEdge;
//import com.gentics.mesh.graphdb.spi.LegacyDatabase;
//import com.syncleus.ferma.Database;
//import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;
//
///**
// * The intercepting edge frame is used in order to be able to switch the currently active graph database context within a single edge instance. This way a
// * framed edge can be used across multiple transactions without the need to reload the instance from the graph. This is mainly achieved by overriding the
// * {@link #getGraph()} method and using the {@link LegacyDatabase#getThreadLocalGraph()} method instead.
// */
//public class AbstractInterceptingWrappedEdge extends AbstractWrappedEdge {
//
//	private Object id;
//	public ThreadLocal<Element> threadLocalElement = ThreadLocal.withInitial(() -> ((WrappedEdge) getGraph().getEdge(id)).getBaseElement());
//
//	@Override
//	protected void init(Database database, Element element) {
//		super.init(database, element);
//		this.id = element.id();
//	}
//
//	public String getFermaType() {
//		return getProperty(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY);
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
//	@Override
//	public Database getGraph() {
//		return Tx.get().getGraph();
//	}
//
//	@Override
//	public Edge getElement() {
//		Element edge = threadLocalElement.get();
//
//		// Unwrap wrapped edge
//		if (edge instanceof WrappedElement) {
//			edge = (Edge) ((WrappedElement) edge).getBaseElement();
//		}
//		return (Edge) edge;
//	}
//
//}
