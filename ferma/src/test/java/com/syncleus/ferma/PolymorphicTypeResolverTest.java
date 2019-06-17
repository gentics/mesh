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
package com.syncleus.ferma;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

public class PolymorphicTypeResolverTest {

    private static final Set<Class<?>> TEST_TYPES = new HashSet<>(Arrays.asList(new Class<?>[]{Person.class, Programmer.class}));
    private static final String CUSTOM_TYPE_KEY = "some_custom_type_key";

    @Test
    public void testChangeType() {
        final TinkerGraph godGraph = new TinkerGraph();
        final FramedGraph framedGraph = new DelegatingFramedGraph(godGraph, TEST_TYPES);

        //add a single node to the graph, a programmer.
        framedGraph.addFramedVertex(Programmer.class);

        //make sure the newly added node is actually a programmer
        final Person programmer = framedGraph.v().next(Person.class);
        Assert.assertTrue(programmer instanceof Programmer);

        //change the type resolution to person
        programmer.setTypeResolution(Person.class);

        //make sure the newly added node is actually a programmer
        final Person person = framedGraph.v().next(Person.class);
        Assert.assertFalse(person instanceof Programmer);
    }
    
    @Test
    public void testCustomTypeKey() {
        final Graph g = new TinkerGraph();
        final ReflectionCache cache = new ReflectionCache(TEST_TYPES);
        final FramedGraph fg = new DelegatingFramedGraph(g, new PolymorphicTypeResolver(cache, CUSTOM_TYPE_KEY));

        final Person p1 = fg.addFramedVertex(null, Programmer.DEFAULT_INITIALIZER);
        p1.setName("Bryn");

        final Person p2 = fg.addFramedVertex(null, Person.DEFAULT_INITIALIZER);
        p2.setName("Julia");

        final Person bryn = fg.v().has("name", "Bryn").next(Person.class);
        final Person julia = fg.v().has("name", "Julia").next(Person.class);

        Assert.assertEquals(Programmer.class, bryn.getClass());
        Assert.assertEquals(Person.class, julia.getClass());
        
        Assert.assertNotNull(bryn.getElement().getProperty(CUSTOM_TYPE_KEY));
        Assert.assertNull(bryn.getElement().getProperty(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY));
        Assert.assertNotNull(julia.getElement().getProperty(CUSTOM_TYPE_KEY));
        Assert.assertNull(julia.getElement().getProperty(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY));
    }
    
    @Test
    public void testCustomTypeKeyByClass() {
        final Graph g = new TinkerGraph();
        final ReflectionCache cache = new ReflectionCache(TEST_TYPES);
        final FramedGraph fg = new DelegatingFramedGraph(g, new PolymorphicTypeResolver(cache, CUSTOM_TYPE_KEY));

        final Person p1 = fg.addFramedVertex(Programmer.class);
        p1.setName("Bryn");

        final Person p2 = fg.addFramedVertex(Person.class);
        p2.setName("Julia");

        final Person bryn = fg.v().has("name", "Bryn").next(Person.class);
        final Person julia = fg.v().has("name", "Julia").next(Person.class);

        Assert.assertEquals(Programmer.class, bryn.getClass());
        Assert.assertEquals(Person.class, julia.getClass());
        
        Assert.assertNotNull(bryn.getElement().getProperty(CUSTOM_TYPE_KEY));
        Assert.assertNull(bryn.getElement().getProperty(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY));
        Assert.assertNotNull(julia.getElement().getProperty(CUSTOM_TYPE_KEY));
        Assert.assertNull(julia.getElement().getProperty(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY));
    }
    
    @Test
    public void testCustomTypeKeyExplicit() {
        final Graph g = new TinkerGraph();
        final ReflectionCache cache = new ReflectionCache(TEST_TYPES);
        final FramedGraph fg = new DelegatingFramedGraph(g, new PolymorphicTypeResolver(cache, CUSTOM_TYPE_KEY));

        final Person p1 = fg.addFramedVertexExplicit(Programmer.DEFAULT_INITIALIZER);
        p1.setName("Bryn");

        final Person p2 = fg.addFramedVertexExplicit(Person.DEFAULT_INITIALIZER);
        p2.setName("Julia");

        final Person bryn = fg.v().has("name", "Bryn").next(Person.class);
        final Person julia = fg.v().has("name", "Julia").next(Person.class);

        Assert.assertEquals(Person.class, bryn.getClass());
        Assert.assertEquals(Person.class, julia.getClass());
        
        Assert.assertNull(bryn.getElement().getProperty(CUSTOM_TYPE_KEY));
        Assert.assertNull(bryn.getElement().getProperty(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY));
        Assert.assertNull(julia.getElement().getProperty(CUSTOM_TYPE_KEY));
        Assert.assertNull(julia.getElement().getProperty(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY));
    }

    @Test
    public void testCustomTypeKeyExplicitByClass() {
        final Graph g = new TinkerGraph();
        final ReflectionCache cache = new ReflectionCache(TEST_TYPES);
        final FramedGraph fg = new DelegatingFramedGraph(g, new PolymorphicTypeResolver(cache, CUSTOM_TYPE_KEY));

        final Person p1 = fg.addFramedVertexExplicit(Programmer.class);
        p1.setName("Bryn");

        final Person p2 = fg.addFramedVertexExplicit(Person.class);
        p2.setName("Julia");

        final Person bryn = fg.v().has("name", "Bryn").next(Person.class);
        final Person julia = fg.v().has("name", "Julia").next(Person.class);

        Assert.assertEquals(Person.class, bryn.getClass());
        Assert.assertEquals(Person.class, julia.getClass());
        
        Assert.assertNull(bryn.getElement().getProperty(CUSTOM_TYPE_KEY));
        Assert.assertNull(bryn.getElement().getProperty(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY));
        Assert.assertNull(julia.getElement().getProperty(CUSTOM_TYPE_KEY));
        Assert.assertNull(julia.getElement().getProperty(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY));
    }
}
