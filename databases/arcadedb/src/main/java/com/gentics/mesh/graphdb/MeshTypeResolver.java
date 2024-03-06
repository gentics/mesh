package com.gentics.mesh.graphdb;

import java.util.Set;
import java.util.function.Predicate;

import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;

import com.gentics.madl.frame.AbstractEdgeFrame;
import com.gentics.madl.frame.AbstractVertexFrame;
import com.gentics.mesh.madl.frame.EdgeFrame;
import com.gentics.mesh.madl.frame.VertexFrame;
import com.syncleus.ferma.typeresolvers.TypeResolver;

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
		final String nodeClazz = element.<String>property(this.typeResolutionKey).orElse(null);
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
		final String typeResolutionName = element.<String>property(this.typeResolutionKey).orElse(null);
		if (typeResolutionName == null) {
			return null;
		}
		return this.reflectionCache.forName(typeResolutionName);
	}

	@Override
	public void init(final Element element, final Class<?> kind) {
		element.property(this.typeResolutionKey, kind.getSimpleName());
	}

	@Override
	public void deinit(final Element element) {
		element.property(this.typeResolutionKey).remove();
	}

	@Override
    public <P extends Element, T extends Element> GraphTraversal<P, T> hasType(final GraphTraversal<P, T> traverser, final Class<?> type) {
        final Set<? extends String> allAllowedValues = this.reflectionCache.getSubTypeNames(type.getName());
        return traverser.has(typeResolutionKey, org.apache.tinkerpop.gremlin.process.traversal.P.within(allAllowedValues));
    }

    @Override
    public <P extends Element, T extends Element> GraphTraversal<P, T> hasNotType(final GraphTraversal<P, T> traverser, final Class<?> type) {
        final Set<? extends String> allAllowedValues = this.reflectionCache.getSubTypeNames(type.getName());
        return traverser.filter(new Predicate<Traverser<T>>() {
            @Override
            public boolean test(final Traverser<T> toCheck) {
                final Property<String> property = toCheck.get().property(typeResolutionKey);
                if( !property.isPresent() )
                    return true;

                final String resolvedType = property.value();
                if( allAllowedValues.contains(resolvedType) )
                    return false;
                else
                    return true;
            }
        });
    }
}
