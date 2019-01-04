package com.gentics.mesh.graphdb;

import java.util.Set;

import org.apache.tinkerpop.gremlin.structure.Element;

import com.gentics.madl.type.resolver.TypeResolver;
import com.gentics.madl.wrapper.element.AbstractWrappedEdge;
import com.gentics.madl.wrapper.element.AbstractWrappedVertex;
import com.gentics.madl.wrapper.element.WrappedEdge;
import com.gentics.madl.wrapper.element.WrappedVertex;
import com.orientechnologies.orient.core.sql.parser.OMatchStatement.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;

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
		final String nodeClazz = element.value(this.typeResolutionKey);
		if (nodeClazz == null) {
			return kind;
		}

		final Class<T> nodeKind = (Class<T>) this.reflectionCache.forName(nodeClazz);
		if (nodeKind == null) {
			throw new RuntimeException("Did not find class in cache {" + nodeClazz + "}");
		}
		if (kind.isAssignableFrom(nodeKind) || kind.equals(WrappedVertex.class) || kind.equals(WrappedEdge.class)
			|| kind.equals(AbstractWrappedVertex.class) || kind.equals(AbstractWrappedEdge.class) || kind.equals(Object.class)) {
			return nodeKind;
		} else {
			return kind;
		}
	}

	@Override
	public Class<?> resolve(final Element element) {
		final String typeResolutionName = element.value(this.typeResolutionKey);
		if (typeResolutionName == null)
			return null;

		return this.reflectionCache.forName(typeResolutionName);
	}

	@Override
	public void init(final Element element, final Class<?> kind) {
		element.property(this.typeResolutionKey, kind.getSimpleName());
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
