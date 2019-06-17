package com.gentics.mesh.graphdb;

import java.util.Set;

import com.syncleus.ferma.AbstractEdgeFrame;
import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.syncleus.ferma.typeresolvers.TypeResolver;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.gremlin.Tokens;

/**
 * This type resolver will use the Java class stored in the 'java_class' on the element.
 */
public class MeshTypeResolver implements TypeResolver {
	public final static String TYPE_RESOLUTION_KEY = "ferma_type";

	private final SimpleReflectionCache reflectionCache;
	private final String typeResolutionKey;

	public MeshTypeResolver(String... basePaths) {
		this.reflectionCache = new SimpleReflectionCache(basePaths);
		this.typeResolutionKey = TYPE_RESOLUTION_KEY;
	}

	@Override
	public <T> Class<? extends T> resolve(final Element element, final Class<T> kind) {
		final String nodeClazz = element.getProperty(this.typeResolutionKey);
		if (nodeClazz == null) {
			return kind;
		}

		final Class<T> nodeKind = (Class<T>) this.reflectionCache.forName(nodeClazz);
		if (nodeKind == null) {
			throw new RuntimeException("Did not find class in cache {" + nodeClazz + "}");
		}
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

		return this.reflectionCache.forName(typeResolutionName);
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
		final Set<? extends String> allAllowedValues = this.reflectionCache.getSubTypeNames(type.getSimpleName());
		return traverser.has(typeResolutionKey, Tokens.T.in, allAllowedValues);
	}

	@Override
	public EdgeTraversal<?, ?, ?> hasType(final EdgeTraversal<?, ?, ?> traverser, final Class<?> type) {
		final Set<? extends String> allAllowedValues = this.reflectionCache.getSubTypeNames(type.getSimpleName());
		return traverser.has(typeResolutionKey, Tokens.T.in, allAllowedValues);
	}

	@Override
	public VertexTraversal<?, ?, ?> hasNotType(VertexTraversal<?, ?, ?> traverser, Class<?> type) {
		final Set<? extends String> allAllowedValues = this.reflectionCache.getSubTypeNames(type.getSimpleName());
		return traverser.has(typeResolutionKey, Tokens.T.notin, allAllowedValues);
	}

	@Override
	public EdgeTraversal<?, ?, ?> hasNotType(EdgeTraversal<?, ?, ?> traverser, Class<?> type) {
		final Set<? extends String> allAllowedValues = this.reflectionCache.getSubTypeNames(type.getSimpleName());
		return traverser.has(typeResolutionKey, Tokens.T.notin, allAllowedValues);
	}

}
