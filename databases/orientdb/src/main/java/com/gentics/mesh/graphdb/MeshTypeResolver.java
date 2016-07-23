package com.gentics.mesh.graphdb;

import com.gentics.ferma.orientdb.ElementTypeClassCache;
import com.syncleus.ferma.AbstractEdgeFrame;
import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;
import com.syncleus.ferma.typeresolvers.TypeResolver;
import com.tinkerpop.blueprints.Element;

/**
 * This type resolver will use the Java class stored in the 'java_class' on the element.
 */
public class MeshTypeResolver implements TypeResolver {

	private ElementTypeClassCache elementTypCache;
	private String typeResolutionKey = PolymorphicTypeResolver.TYPE_RESOLUTION_KEY;

	public MeshTypeResolver(String... packagePaths) {
		this.elementTypCache = new ElementTypeClassCache(packagePaths);
	}

	@Override
	public <T> Class<? extends T> resolve(final Element element, final Class<T> kind) {
		final String nodeClazz = element.getProperty(this.typeResolutionKey);
		if (nodeClazz == null) {
			return kind;
		}

		final Class<T> nodeKind = (Class<T>) this.elementTypCache.forName(nodeClazz);
		if (kind.isAssignableFrom(nodeKind) || kind.equals(VertexFrame.class) || kind.equals(EdgeFrame.class)
				|| kind.equals(AbstractVertexFrame.class) || kind.equals(AbstractEdgeFrame.class) || kind.equals(Object.class)) {
			return nodeKind;
		} else {
			return kind;
		}
	}

	@Override
	public Class<?> resolve(final Element element) {
		final String typeResolutionName = element.getProperty(this.typeResolutionKey);
		if (typeResolutionName == null)
			return null;

		return this.elementTypCache.forName(typeResolutionName);
	}

	@Override
	public void init(final Element element, final Class<?> kind) {
		element.setProperty(this.typeResolutionKey, kind.getSimpleName());
	}

	@Override
	public void deinit(final Element element) {
		element.removeProperty(this.typeResolutionKey);
	}

	@Override
	public VertexTraversal<?, ?, ?> hasType(final VertexTraversal<?, ?, ?> traverser, final Class<?> type) {

		return null;
	}

	@Override
	public EdgeTraversal<?, ?, ?> hasType(final EdgeTraversal<?, ?, ?> traverser, final Class<?> type) {
		return null;
	}

	@Override
	public VertexTraversal<?, ?, ?> hasNotType(VertexTraversal<?, ?, ?> traverser, Class<?> type) {
		return null;
	}

	@Override
	public EdgeTraversal<?, ?, ?> hasNotType(EdgeTraversal<?, ?, ?> traverser, Class<?> type) {
		return null;
	}

}
