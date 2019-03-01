/**
 * Copyright 2004 - 2016 Syncleus, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.syncleus.ferma.typeresolvers;

import com.syncleus.ferma.AbstractEdgeFrame;
import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.ReflectionCache;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.gremlin.Tokens;

import java.util.Set;

/**
 * This type resolver will use the Java class stored in the 'java_class' on
 * the element.
 */
public class PolymorphicTypeResolver implements TypeResolver {
    public final static String TYPE_RESOLUTION_KEY = "ferma_type";

    private final ReflectionCache reflectionCache;
    private final String typeResolutionKey;

    /**
     * Creates a new SimpleTypeResolver with a typing engine that can recognize the specified types. While these types
     * still need to be included in a separate TypedModule they must be created here as well to ensure proper look-ups
     * occur.
     *
     * @since 2.0.0
     */
    public PolymorphicTypeResolver() {
        this.reflectionCache = new ReflectionCache();
	this.typeResolutionKey = TYPE_RESOLUTION_KEY;
    }

    /**
     * Creates a new SimpleTypeResolver with a typing engine that can recognize the specified types. While these types
     * still need to be included in a separate TypedModule they must be created here as well to ensure proper look-ups
     * occur.
     *
     * @since 2.0.0
     */
    public PolymorphicTypeResolver(final String typeResolutionKey) {
        this.reflectionCache = new ReflectionCache();
	this.typeResolutionKey = typeResolutionKey;
    }

    /**
     * Creates a new SimpleTypeResolver with a typing engine that can recognize the specified types. While these types
     * still need to be included in a separate TypedModule they must be created here as well to ensure proper look-ups
     * occur.
     *
     * @param reflectionCache the ReflectionCache used to examine the type hierarchy and do general reflection.
     * @since 2.0.0
     */
    public PolymorphicTypeResolver(final ReflectionCache reflectionCache) {
        this.reflectionCache = reflectionCache;
	this.typeResolutionKey = TYPE_RESOLUTION_KEY;
    }

    /**
     * Creates a new SimpleTypeResolver with a typing engine that can recognize the specified types. While these types
     * still need to be included in a separate TypedModule they must be created here as well to ensure proper look-ups
     * occur.
     *
     * @param reflectionCache the ReflectionCache used to examine the type hierarchy and do general reflection.
     * @since 2.0.0
     */
    public PolymorphicTypeResolver(final ReflectionCache reflectionCache, final String typeResolutionKey) {
        this.reflectionCache = reflectionCache;
	this.typeResolutionKey = typeResolutionKey;
    }

    @Override
    public <T> Class<? extends T> resolve(final Element element, final Class<T> kind) {
        final String nodeClazz = element.getProperty(this.typeResolutionKey);
        if (nodeClazz == null)
            return kind;

        final Class<T> nodeKind = (Class<T>) this.reflectionCache.forName(nodeClazz);

        if (kind.isAssignableFrom(nodeKind) || kind.equals(VertexFrame.class) || kind.equals(EdgeFrame.class) || kind.equals(AbstractVertexFrame.class) || kind.equals(AbstractEdgeFrame.class) || kind.
              equals(Object.class))
            return nodeKind;
        else
            return kind;
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
        element.setProperty(this.typeResolutionKey, kind.getName());
    }
    
    @Override
    public void deinit(final Element element) {
        element.removeProperty(this.typeResolutionKey);
    }
    
    @Override
    public VertexTraversal<?,?,?> hasType(final VertexTraversal<?,?,?> traverser, final Class<?> type) {
        final Set<? extends String> allAllowedValues = this.reflectionCache.getSubTypeNames(type.getName());
        return traverser.has(typeResolutionKey, Tokens.T.in, allAllowedValues);
    }
    
    @Override
    public EdgeTraversal<?,?,?> hasType(final EdgeTraversal<?,?,?> traverser, final Class<?> type) {
        final Set<? extends String> allAllowedValues = this.reflectionCache.getSubTypeNames(type.getName());
        return traverser.has(typeResolutionKey, Tokens.T.in, allAllowedValues);
    }

    @Override
    public VertexTraversal<?, ?, ?> hasNotType(VertexTraversal<?, ?, ?> traverser, Class<?> type) {
        final Set<? extends String> allAllowedValues = this.reflectionCache.getSubTypeNames(type.getName());
        return traverser.has(typeResolutionKey, Tokens.T.notin, allAllowedValues);
    }

    @Override
    public EdgeTraversal<?, ?, ?> hasNotType(EdgeTraversal<?, ?, ?> traverser, Class<?> type) {
        final Set<? extends String> allAllowedValues = this.reflectionCache.getSubTypeNames(type.getName());
        return traverser.has(typeResolutionKey, Tokens.T.notin, allAllowedValues);
    }

}
