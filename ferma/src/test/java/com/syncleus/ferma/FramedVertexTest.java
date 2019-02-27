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

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

public class FramedVertexTest {

    private FramedGraph fg;
    private Person p1;
    private Person p2;
    private Knows e1;

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
    public void testOut() {

        Assert.assertEquals(p2, p1.out().next(Person.class));
        Assert.assertEquals(p2, p1.out(1).next(Person.class));
    }

    @Test
    public void testOutExplicit() {

        Assert.assertEquals(p2, p1.out().nextExplicit(Person.class));
        Assert.assertEquals(p2, p1.out(1).nextExplicit(Person.class));
    }

    @Test
    public void testIn() {
        Assert.assertEquals(p1, p2.in().next(Person.class));
        Assert.assertEquals(p1, p2.in(1).next(Person.class));

    }

    @Test
    public void testInExplicit() {
        Assert.assertEquals(p1, p2.in().nextExplicit(Person.class));
        Assert.assertEquals(p1, p2.in(1).nextExplicit(Person.class));

    }

    @Test
    public void testOutE() {
        Assert.assertEquals(e1, p1.outE().next(Knows.class));
        Assert.assertEquals(e1, p1.outE(1).next(Knows.class));
    }

    @Test
    public void testOutEExplicit() {
        Assert.assertEquals(e1, p1.outE().nextExplicit(Knows.class));
        Assert.assertEquals(e1, p1.outE(1).nextExplicit(Knows.class));
    }

    @Test
    public void testInE() {
        Assert.assertEquals(e1, p2.inE().next(Knows.class));
        Assert.assertEquals(e1, p2.inE(1).next(Knows.class));
    }

    @Test
    public void testInEExplicit() {
        Assert.assertEquals(e1, p2.inE().nextExplicit(Knows.class));
        Assert.assertEquals(e1, p2.inE(1).nextExplicit(Knows.class));
    }

    @Test
    public void testBoth() {
        Assert.assertEquals(p2, p1.both().next(Person.class));
        Assert.assertEquals(p2, p1.both(1).next(Person.class));
        Assert.assertEquals(p1, p2.both().next(Person.class));
        Assert.assertEquals(p1, p2.both(1).next(Person.class));
    }

    @Test
    public void testBothExplicit() {
        Assert.assertEquals(p2, p1.both().nextExplicit(Person.class));
        Assert.assertEquals(p2, p1.both(1).nextExplicit(Person.class));
        Assert.assertEquals(p1, p2.both().nextExplicit(Person.class));
        Assert.assertEquals(p1, p2.both(1).nextExplicit(Person.class));
    }

    @Test
    public void testBothE() {
        Assert.assertEquals(e1, p2.bothE().next(Knows.class));
        Assert.assertEquals(e1, p2.bothE(1).next(Knows.class));
    }

    @Test
    public void testBothEExplicit() {
        Assert.assertEquals(e1, p2.bothE().nextExplicit(Knows.class));
        Assert.assertEquals(e1, p2.bothE(1).nextExplicit(Knows.class));
    }

    @Test
    public void testLinkOutSingleLabel() {
        final Person p3 = fg.addFramedVertex(Person.class);
        p3.setName("Tjad");

        final String label = "knows";
        final long expectedEdgeCount = p3.outE(label).count() + 1;
        final long expectedVerticesCount = p3.out(label).retain(Lists.newArrayList(p1)).count() + 1;

        p3.linkOut(p1, label);
        //Make sure a new edge was created
        Assert.assertEquals("knows edge was not created", expectedEdgeCount, p3.outE(label).count());

        //Make sure the edge was created to the correct vertex
        Assert.assertEquals("Incorrect vertex associated", expectedVerticesCount, p3.out(label).retain(Lists.newArrayList(p1)).count());
    }

    @Test
    public void testLinkOutMultiLabel() {

        final String[] newLabels = new String[]{"knows", "friends_with"};

        final Person p3 = fg.addFramedVertex(Person.class);
        p3.setName("Tjad");

        final Map<String, Number> expectedCounts = new HashMap<>();

        for (final String label : newLabels) {
            expectedCounts.put("expected_e_" + label, p3.outE(label).count() + 1);
            expectedCounts.put("expected_v_" + label, p3.out(label).retain(Lists.newArrayList(p1)).count() + 1);
        }

        p3.linkOut(p1, newLabels);


        for (final String label : newLabels) {
            //Make sure a new edge was created
            Assert.assertEquals("knows edge was not created", expectedCounts.get("expected_e_" + label), p3.outE(label).count());

            //Make sure the edge was created to the correct vertex
            Assert.assertEquals("Incorrect vertex associated", expectedCounts.get("expected_v_" + label), p3.out(label).retain(Lists.newArrayList(p1)).count());
        }
    }

    @Test
    public void testLinkInSingleLabel() {
        final Person p3 = fg.addFramedVertex(Person.class);
        p3.setName("Tjad");

        final String label = "knows";
        final long expectedEdgeCount = p3.inE(label).count() + 1;
        final long expectedVerticesCount = p3.in(label).retain(Lists.newArrayList(p1)).count() + 1;

        p3.linkIn(p1, label);
        //Make sure a new edge was created
        Assert.assertEquals("knows edge was not created", expectedEdgeCount, p3.inE(label).count());

        //Make sure the edge was created to the correct vertex
        Assert.assertEquals("Incorrect vertex associated", expectedVerticesCount, p3.in(label).retain(Lists.newArrayList(p1)).count());
    }

    @Test
    public void testLinkInMultiLabel() {

        final String[] newLabels = new String[]{"knows", "friends_with"};

        final Person p3 = fg.addFramedVertex(Person.class);
        p3.setName("Tjad");

        final Map<String, Number> expectedCounts = new HashMap<>();

        for (final String label : newLabels) {
            expectedCounts.put("expected_e_" + label, p3.inE(label).count() + 1);
            expectedCounts.put("expected_v_" + label, p3.in(label).retain(Lists.newArrayList(p1)).count() + 1);
        }

        p3.linkIn(p1, newLabels);


        for (final String label : newLabels) {
            //Make sure a new edge was created
            Assert.assertEquals("knows edge was not created", expectedCounts.get("expected_e_" + label), p3.inE(label).count());

            //Make sure the edge was created to the correct vertex
            Assert.assertEquals("Incorrect vertex associated", expectedCounts.get("expected_v_" + label), p3.in(label).retain(Lists.newArrayList(p1)).count());
        }

    }

    @Test
    public void testLinkBothSingleLabel() {
        final Person p3 = fg.addFramedVertex(Person.class);
        p3.setName("Tjad");

        final String label = "knows";
        final long expectedEdgeCount = p3.bothE(label).count() + 2;
        final long tailCount = p3.in(label).retain(Lists.newArrayList(p1)).count() + 1;
        final long headCount = p3.out(label).retain(Lists.newArrayList(p1)).count() + 1;

        p3.linkBoth(p1, label);
        //Make sure a new edge was created
        Assert.assertEquals("knows edge was not created", expectedEdgeCount, p3.bothE(label).count());

        //Make sure the edge was created to the correct vertex
        Assert.assertEquals("Incorrect in vertex associated", tailCount, p3.in(label).retain(Lists.newArrayList(p1)).count());
        Assert.assertEquals("Incorrect out vertex associated", headCount, p3.out(label).retain(Lists.newArrayList(p1)).count());
    }

    @Test
    public void testLinkBothMultiLabel() {

        final String[] newLabels = new String[]{"knows", "friends_with"};

        final Person p3 = fg.addFramedVertex(Person.class);
        p3.setName("Tjad");

        final Map<String, Number> expectedCounts = new HashMap<>();

        for (final String label : newLabels) {
            expectedCounts.put("expected_e_" + label, p3.bothE(label).count() + 2);
            expectedCounts.put("expected_tail_" + label, p3.in(label).retain(Lists.newArrayList(p1)).count() + 1);
            expectedCounts.put("expected_head_" + label, p3.out(label).retain(Lists.newArrayList(p1)).count() + 1);
        }

        p3.linkBoth(p1, newLabels);


        for (final String label : newLabels) {
            //Make sure a new edge was created
            Assert.assertEquals("knows edge was not created", expectedCounts.get("expected_e_" + label), p3.bothE(label).count());

            //Make sure the edge was created to the correct vertex
            Assert.assertEquals("Incorrect vertex associated", expectedCounts.get("expected_tail_" + label), p3.in(label).retain(Lists.newArrayList(p1)).count());
            Assert.assertEquals("Incorrect vertex associated", expectedCounts.get("expected_head_" + label), p3.out(label).retain(Lists.newArrayList(p1)).count());
        }

    }

    @Test
    public void testUnlinkInWithNull() {
        final String label = "knows";
        final Person p3 = fg.addFramedVertex(Person.class);
        final Person p4 = fg.addFramedVertex(Person.class);
        final Person p5 = fg.addFramedVertex(Person.class);

        p4.addFramedEdge(label, p3, Knows.class);
        p5.addFramedEdge(label, p3, Knows.class);

        Assert.assertTrue("Multiple edges(in) of type " + label + " must exist for vertice", p3.in(label).count() > 1);

        p3.unlinkIn(null, label);

        //Make all edges were removed
        Assert.assertEquals("All " + label + " edges(in) should have been removed", 0, p3.in(label).count());
    }

    @Test
    public void testUnlinkOutWithNull() {
        final String label = "knows";
        final Person p3 = fg.addFramedVertex(Person.class);
        final Person p4 = fg.addFramedVertex(Person.class);
        final Person p5 = fg.addFramedVertex(Person.class);

        p3.addFramedEdge(label, p4, Knows.class);
        p3.addFramedEdge(label, p5, Knows.class);

        Assert.assertTrue("Multiple edges(out) of type " + label + " must exist for vertice", p3.out(label).count() > 1);

        p3.unlinkOut(null, label);

        //Make all edges were removed
        Assert.assertEquals("All " + label + " edges(out) should have been removed", 0, p3.out(label).count());

    }

    @Test
    public void testUnlinkBothWithNull() {
        final String label = "knows";
        final Person p3 = fg.addFramedVertex(Person.class);
        final Person p4 = fg.addFramedVertex(Person.class);
        final Person p5 = fg.addFramedVertex(Person.class);

        p3.addFramedEdge(label, p4, Knows.class);
        p3.addFramedEdge(label, p5, Knows.class);

        Assert.assertTrue("Multiple edges(out) of type " + label + " must exist for vertice", p3.out(label).count() > 1);

        p4.addFramedEdge(label, p3, Knows.class);
        p5.addFramedEdge(label, p3, Knows.class);

        Assert.assertTrue("Multiple edges(in) of type " + label + " must exist for vertice", p3.in(label).count() > 1);

        p3.unlinkBoth(null, label);

        //Make all edges were removed
        Assert.assertEquals("All " + label + " edges should have been removed", 0, p3.both(label).count());

    }

    @Test
    public void testUnlinkIn() {
        final String label = "knows";
        final Person p3 = fg.addFramedVertex(Person.class);
        final Person p4 = fg.addFramedVertex(Person.class);
        final Person p5 = fg.addFramedVertex(Person.class);

        p4.addFramedEdge(label, p3, Knows.class);
        p5.addFramedEdge(label, p3, Knows.class);
        final long allInEdgesCount = p3.in(label).count();
        final Long targetVertex_InEdgeCount = p3.in(label).mark().retain(Lists.newArrayList(p4)).back().count();

        Assert.assertTrue("target vertex requires an in edge for " + label, targetVertex_InEdgeCount > 0);
        Assert.assertTrue("Multiple edges(in) of type " + label + " must exist for vertice", allInEdgesCount - targetVertex_InEdgeCount > 0);

        p3.unlinkIn(p4, label);

        //Make all edges were removed
        Assert.assertEquals("All " + label + " edges(in) should have been removed from given vertex", 0, p3.in(label).retain(Lists.newArrayList(p4)).count());
        Assert.assertTrue("All " + label + " edges(in) should have remained for other vertices", p3.inE(label).count() > 0);
    }

    @Test
    public void testUnlinkOut() {
        final String label = "knows";
        final Person p3 = fg.addFramedVertex(Person.class);
        final Person p4 = fg.addFramedVertex(Person.class);
        final Person p5 = fg.addFramedVertex(Person.class);

        p3.addFramedEdge(label, p4, Knows.class);
        p3.addFramedEdge(label, p5, Knows.class);
        final long allInEdgesCount = p3.out(label).count();
        final Long targetVertex_OutEdgeCount = p3.out(label).mark().retain(Lists.newArrayList(p4)).back().count();

        Assert.assertTrue("target vertex requires an in edge for " + label, targetVertex_OutEdgeCount > 0);
        Assert.assertTrue("Multiple edges(out) of type " + label + " must exist for vertice", allInEdgesCount - targetVertex_OutEdgeCount > 0);

        p3.unlinkOut(p4, label);

        //Make all edges were removed
        Assert.assertEquals("All " + label + " edges(in) should have been removed from given vertex", 0, p3.out(label).retain(Lists.newArrayList(p4)).count());
        Assert.assertTrue("All " + label + " edges(in) should have remained for other vertices", p3.outE(label).count() > 0);
    }

    @Test
    public void testUnlinkBoth() {
        final String label = "knows";
        final Person p3 = fg.addFramedVertex(Person.class);
        final Person p4 = fg.addFramedVertex(Person.class);
        final Person p5 = fg.addFramedVertex(Person.class);

        p4.addFramedEdge(label, p3, Knows.class);
        p5.addFramedEdge(label, p3, Knows.class);
        p3.addFramedEdge(label, p4, Knows.class);
        p3.addFramedEdge(label, p5, Knows.class);

        final long allInEdgesCount = p3.both(label).count();
        final Long targetVertex_EdgeCount = p3.both(label).mark().retain(Lists.newArrayList(p4)).back().count();

        Assert.assertTrue("target vertex requires an in/out edge for " + label, targetVertex_EdgeCount > 0);
        Assert.assertTrue("Multiple edges of type " + label + " must exist for vertice", allInEdgesCount - targetVertex_EdgeCount > 0);

        p3.unlinkBoth(p4, label);

        //Make all edges were removed
        Assert.assertEquals("All " + label + " edges(in/out) should have been removed from given vertex", 0, p3.both(label).retain(Lists.newArrayList(p4)).count());
        Assert.assertTrue("All " + label + " edges(in/out) should have remained for other vertices", p3.bothE(label).count() > 0);
    }

    @Test
    public void testSetLinkIn() {
        final String label = "knows";
        final Person p3 = fg.addFramedVertex(Person.class);
        final Person p4 = fg.addFramedVertex(Person.class);
        final Person p5 = fg.addFramedVertex(Person.class);

        p4.addFramedEdge(label, p3, Knows.class);

        Assert.assertTrue("An edge(in) of type " + label + " must exist between vertices", p3.in(label).retain(Lists.newArrayList(p4)).count() > 0);

        p3.setLinkIn(p5, label);

        //Make sure old edge was deleted
        Assert.assertEquals("old " + label + " edge was not removed", 0, p3.in(label).retain(Lists.newArrayList(p4)).count());


        //Make sure a new edge was created (and to correct vertex)
        Assert.assertEquals(label + " edge was not created", 1, p3.in(label).retain(Lists.newArrayList(p5)).count());
    }

    @Test
    public void testSetLinkInNull() {
        final String label = "knows";
        final Person p3 = fg.addFramedVertex(Person.class);
        final Person p4 = fg.addFramedVertex(Person.class);
        p3.setLinkIn(p4, label);
        Assert.assertTrue("An out edge of type " + label + " must exist between vertices", p3.in(label).retain(Lists.newArrayList(p4)).count() > 0);

        // tests if setLinkOut accepts null values, and only unlinks the edges
        p3.setLinkIn((VertexFrame) null, label);
        Assert.assertEquals(label + " edge was not unlinked", 0, p3.in(label).count());
    }

    @Test
    public void testSetLinkOut() {
        final String label = "knows";
        final Person p3 = fg.addFramedVertex(Person.class);
        final Person p4 = fg.addFramedVertex(Person.class);
        final Person p5 = fg.addFramedVertex(Person.class);

        p3.addFramedEdge(label, p4, Knows.class);

        Assert.assertTrue("An out edge of type " + label + " must exist between vertices", p3.out(label).retain(Lists.newArrayList(p4)).count() > 0);

        p3.setLinkOut(p5, label);

        //Make sure old edge was deleted
        Assert.assertEquals("old " + label + " edge was not removed", 0, p3.out(label).retain(Lists.newArrayList(p4)).count());


        //Make sure a new edge was created (and to correct vertex)
        Assert.assertEquals(label + " edge was not created", 1, p3.out(label).retain(Lists.newArrayList(p5)).count());
    }

    @Test
    public void testSetLinkOutNull() {
        final String label = "knows";
        final Person p3 = fg.addFramedVertex(Person.class);
        final Person p4 = fg.addFramedVertex(Person.class);
        p3.setLinkOut(p4, label);
        Assert.assertTrue("An out edge of type " + label + " must exist between vertices", p3.out(label).retain(Lists.newArrayList(p4)).count() > 0);

        // tests if setLinkOut accepts null values, and only unlinks the edges
        p3.setLinkOut((VertexFrame) null, label);
        Assert.assertEquals(label + " edge was not unlinked", 0, p3.out(label).count());
    }

    @Test
    public void testSetLinkBoth() {
        final String label = "knows";
        final Person p3 = fg.addFramedVertex(Person.class);
        final Person p4 = fg.addFramedVertex(Person.class);
        final Person p5 = fg.addFramedVertex(Person.class);

        p4.addFramedEdge(label, p3, Knows.class);
        p3.addFramedEdge(label, p4, Knows.class);

        Assert.assertTrue("An in edge of type " + label + " must exist between vertices", p3.in(label).retain(Lists.newArrayList(p4)).count() > 0);
        Assert.assertTrue("An out edge of type " + label + " must exist between vertices", p3.out(label).retain(Lists.newArrayList(p4)).count() > 0);

        p3.setLinkBoth(p5, label);

        //Make sure old edge was deleted
        Assert.assertEquals("old " + label + " edge in was not removed", 0, p3.in(label).retain(Lists.newArrayList(p4)).count());
        Assert.assertEquals("old " + label + " edge out was not removed", 0, p3.out(label).retain(Lists.newArrayList(p4)).count());


        //Make sure a new edges were created (and to correct vertex)
        Assert.assertEquals(label + " in edge was not created", 1, p3.in(label).retain(Lists.newArrayList(p5)).count());
        Assert.assertEquals(label + " out edge was not created", 1, p3.out(label).retain(Lists.newArrayList(p5)).count());

    }

    @Test
    public void testSetLinkBothNull() {
        final String label = "knows";
        final Person p3 = fg.addFramedVertex(Person.class);
        final Person p4 = fg.addFramedVertex(Person.class);
        p3.setLinkBoth(p4, label);
        Assert.assertTrue("An out edge of type " + label + " must exist between vertices", p3.both(label).retain(Lists.newArrayList(p4)).count() > 0);

        // tests if setLinkOut accepts null values, and only unlinks the edges
        p3.setLinkBoth((VertexFrame) null, label);
        Assert.assertEquals(label + " edge was not unlinked", 0, p3.both(label).count());
    }

    @Test
    public void testSetNewLinkIn() {
        final String label = "knows";
        final Person p3 = fg.addFramedVertex(Person.class);
        final Person p4 = fg.addFramedVertex(Person.class);

        p4.addFramedEdge(label, p3, Knows.class);

        Assert.assertTrue("An out edge of type " + label + " must exist between vertices", p3.in(label).retain(Lists.newArrayList(p4)).count() > 0);

        final Person p5 = p3.setLinkIn(Person.class, label);

        //Make sure old edge was deleted
        Assert.assertEquals("old " + label + " edge was not removed", 0, p3.in(label).retain(Lists.newArrayList(p4)).count());


        //Make sure a new edge was created (and to correct vertex)
        Assert.assertEquals(label + " edge was not created", 1, p3.in(label).retain(Lists.newArrayList(p5)).count());

    }

    @Test
    public void testSetNewLinkOut() {
        final String label = "knows";
        final Person p3 = fg.addFramedVertex(Person.class);
        final Person p4 = fg.addFramedVertex(Person.class);

        p3.addFramedEdge(label, p4, Knows.class);

        Assert.assertTrue("An out edge of type " + label + " must exist between vertices", p3.out(label).retain(Lists.newArrayList(p4)).count() > 0);

        final Person p5 = p3.setLinkOut(Person.class, label);

        //Make sure old edge was deleted
        Assert.assertEquals("old " + label + " edge was not removed", 0, p3.out(label).retain(Lists.newArrayList(p4)).count());


        //Make sure a new edge was created (and to correct vertex)
        Assert.assertEquals(label + " edge was not created", 1, p3.out(label).retain(Lists.newArrayList(p5)).count());

    }

    @Test
    public void testSetNewLinkBoth() {
        final String label = "knows";
        final Person p3 = fg.addFramedVertex(Person.class);
        final Person p4 = fg.addFramedVertex(Person.class);

        p3.addFramedEdge(label, p4, Knows.class);
        p4.addFramedEdge(label, p3, Knows.class);

        Assert.assertTrue("An in edge of type " + label + " must exist between vertices", p3.in(label).retain(Lists.newArrayList(p4)).count() > 0);
        Assert.assertTrue("An out edge of type " + label + " must exist between vertices", p3.out(label).retain(Lists.newArrayList(p4)).count() > 0);

        final Person p5 = p3.setLinkBoth(Person.class, label);

        //Make sure old edge was deleted
        Assert.assertEquals("old " + label + " edge was not removed", 0, p3.both(label).retain(Lists.newArrayList(p4)).count());


        //Make sure a new edge was created (and to correct vertex)
        Assert.assertEquals(label + " edge(out) was not created", 1, p3.out(label).retain(Lists.newArrayList(p5)).count());
        Assert.assertEquals(label + " edge(in) was not created", 1, p3.in(label).retain(Lists.newArrayList(p5)).count());
    }

}
