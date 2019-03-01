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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

public class FramedEdgeTest {

    private Person p1;
    private Person p2;
    private Knows e1;
    private FramedGraph fg;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        final Graph g = new TinkerGraph();
        fg = new DelegatingFramedGraph(g);
        p1 = fg.addFramedVertex(null, Person.DEFAULT_INITIALIZER);
        p2 = fg.addFramedVertex(null, Person.DEFAULT_INITIALIZER);
        p1.setName("Bryn");
        p2.setName("Julia");
        e1 = p1.addKnows(p2);
        e1.setYears(15);
        
    }

    @Test
    public void testLabel() {
        Assert.assertEquals("knows", e1.getLabel());
    }

    @Test
    public void testInV() {
        Assert.assertEquals(p2, e1.inV().next(Person.class));
    }

    @Test
    public void testOutV() {
        Assert.assertEquals(p1, e1.outV().next(Person.class));
    }

    @Test
    public void testBothV() {
        Assert.assertEquals(p1, e1.bothV().next(Person.class));
    }

    @Test
    public void testInVExplicit() {
        Assert.assertEquals(p2, e1.inV().nextExplicit(Person.class));
    }

    @Test
    public void testOutVExplicit() {
        Assert.assertEquals(p1, e1.outV().nextExplicit(Person.class));
    }

    @Test
    public void testBothVExplicit() {
        Assert.assertEquals(p1, e1.bothV().nextExplicit(Person.class));
    }

    @Test
    public void testNextOrDefaultExplicit() {
        assertNotNull(fg.e().nextOrDefaultExplicit(Knows.class, null));
        fg.e().removeAll();
        assertNull(fg.e().nextOrDefaultExplicit(Knows.class, null));
    }
}
