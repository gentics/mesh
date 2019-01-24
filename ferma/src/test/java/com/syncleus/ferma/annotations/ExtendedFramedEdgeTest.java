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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.syncleus.ferma.DelegatingFramedGraph;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.Knows;
import com.syncleus.ferma.Person;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory;

public class ExtendedFramedEdgeTest {

	private static final Set<Class<?>> TEST_TYPES = new HashSet<Class<?>>(
    		Arrays.asList(new Class<?>[]{
    				Person.class,
    				Friend.class,
    				Knows.class
    				}
    		));
	private Friend p1;
    private Friend p2;
    private Knows e1;
    private Knows e2;

    @Before
    public void init() {
    	
        MockitoAnnotations.initMocks(this);
        final Graph g =TinkerGraphFactory.createTinkerGraph();
        final FramedGraph fg = new DelegatingFramedGraph(g, TEST_TYPES);
        p1 = fg.addFramedVertex(Friend.class);
        p2 = fg.addFramedVertex(Friend.class);
        p1.setName("Bryn");
        p2.setName("Julia");
        e1 = p1.addKnows(p2, Knows.DEFAULT_INITIALIZER);
        e1.setYears(15);
        e2 = p1.addKnownBy(p2, Knows.DEFAULT_INITIALIZER);
        e2.setYears(15);
        
    }

    @Test
    public void testLabel() {
        Assert.assertEquals("knows", e1.getLabel());
    }

    @Test
    public void testInV() {
        Assert.assertEquals(p2, e1.inV().next(Friend.class));
    }

    @Test
    public void testOutV() {
        Assert.assertEquals(p1, e1.outV().next(Friend.class));
    }

    @Test
    public void testBothV() {
        Assert.assertEquals(p1, e1.bothV().next(Friend.class));
    }

    @Test
    public void testInVExplicit() {
        Assert.assertEquals(p2, e1.inV().nextExplicit(Friend.class));
    }

    @Test
    public void testOutVExplicit() {
        Assert.assertEquals(p1, e1.outV().nextExplicit(Friend.class));
    }

    @Test
    public void testBothVExplicit() {
        Assert.assertEquals(p1, e1.bothV().nextExplicit(Friend.class));
    }
    
}
