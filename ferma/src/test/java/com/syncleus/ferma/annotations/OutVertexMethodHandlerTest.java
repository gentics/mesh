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
package com.syncleus.ferma.annotations;

import com.syncleus.ferma.DelegatingFramedGraph;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.VertexFrame;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class OutVertexMethodHandlerTest {

    private static final Set<Class<?>> TEST_TYPES = new HashSet<>(Arrays.asList(new Class<?>[]{God.class, FatherEdge.class, GodExtended.class, FatherEdgeExtended.class}));

    @Test
    public void testGetSonEdges() {
        final TinkerGraph godGraph = new TinkerGraph();
        GodGraphLoader.load(godGraph);

        final FramedGraph framedGraph = new DelegatingFramedGraph(godGraph, TEST_TYPES);

        final List<? extends God> gods = framedGraph.v().has("name", "jupiter").toList(God.class);

        final God father = gods.iterator().next();
        Assert.assertTrue(father != null);
        final VertexFrame fatherVertex = father;
        Assert.assertEquals(fatherVertex.getProperty("name"), "jupiter");

        final Iterable<? extends FatherEdge> childrenEdges = father.getSonEdges(FatherEdge.class);
        final Iterator<? extends FatherEdge> childEdgeIterator = childrenEdges.iterator();
        Assert.assertTrue(childEdgeIterator.hasNext());
        final FatherEdge childEdge = childEdgeIterator.next();

        final God son = childEdge.getSon();
        Assert.assertTrue(son != null);
        final VertexFrame sonVertex = son;
        Assert.assertEquals(sonVertex.getProperty("name"), "hercules");
    }
}
