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

import com.syncleus.ferma.traversals.SideEffectFunction;
import com.syncleus.ferma.traversals.TraversalFunction;
import com.syncleus.ferma.traversals.TraversalFunctions;
import com.syncleus.ferma.traversals.Traversal;
import com.syncleus.ferma.traversals.VertexTraversal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Test;

import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory;
import com.tinkerpop.gremlin.Tokens.T;
import com.tinkerpop.pipes.transform.TransformPipe.Order;
import com.tinkerpop.pipes.util.structures.Row;
import com.tinkerpop.pipes.util.structures.Table;
import com.tinkerpop.pipes.util.structures.Tree;

public class TraversalsTest {

	private final FramedGraph graph = new DelegatingFramedGraph(TinkerGraphFactory.createTinkerGraph());

	@Test
	public void testCopySplit() {
		final List<? extends VertexFrame> fair = graph.v(1).out("knows")
			.copySplit(new TraversalFunction<VertexFrame, Traversal<VertexFrame, ?, ?, ?>>() {

				@Override
				public Traversal<VertexFrame, ?, ?, ?> compute(final VertexFrame argument) {
					return argument.traversal();
				}
			}, new TraversalFunction<VertexFrame, Traversal<VertexFrame, ?, ?, ?>>() {

				@Override
				public Traversal<VertexFrame, ?, ?, ?> compute(final VertexFrame argument) {
					return argument.out();
				}
			}).fairMerge().toList();

		final List<? extends VertexFrame> exhaust = graph.v(1).out("knows")
			.copySplit(new TraversalFunction<VertexFrame, Traversal<VertexFrame, ?, ?, ?>>() {

				@Override
				public Traversal<VertexFrame, ?, ?, ?> compute(final VertexFrame argument) {
					return argument.traversal();
				}
			}, new TraversalFunction<VertexFrame, Traversal<VertexFrame, ?, ?, ?>>() {

				@Override
				public Traversal<VertexFrame, ?, ?, ?> compute(final VertexFrame argument) {
					return argument.out();
				}
			}).exhaustMerge().toList();

		Assert.assertEquals(exhaust.get(0), fair.get(0));
		Assert.assertNotEquals(exhaust.get(1), fair.get(1));
		Assert.assertEquals(exhaust.get(2), fair.get(1));
		Assert.assertEquals(exhaust.get(1), fair.get(2));
		Assert.assertTrue(exhaust.get(0) instanceof TVertex);

	}

	@Test
	public void testMarkBack() {

		Assert.assertEquals(29, graph.v().mark().outE("knows").inV().has("age", T.gt, 30).back().property("age").next());
	}

	@Test
	public void testMarkOptional() {
		final List<VertexFrame> aggregate = new ArrayList<>();
		Assert.assertEquals(6, graph.v().mark().outE("knows").inV().has("age", T.gt, 30).aggregate(aggregate).optional()
			.property("age").count());
		Assert.assertEquals(graph.v(4).next(), aggregate.get(0));
	}

	@Test
	public void testBoth() {
		Assert.assertEquals(3, graph.v(4).both().count());
		Assert.assertEquals(1, graph.v(4).both("knows").count());
		Assert.assertEquals(3, graph.v(4).both("knows", "created").count());
		Assert.assertEquals(1, graph.v(4).both(1, "knows", "created").count());

	}

	@Test
	public void testBothE() {
		Assert.assertEquals(3, graph.v(4).bothE().count());
		Assert.assertEquals(1, graph.v(4).bothE("knows").count());
		Assert.assertEquals(3, graph.v(4).bothE("knows", "created").count());
		Assert.assertEquals(1, graph.v(4).bothE(1, "knows", "created").count());

	}

	@Test
	public void testBothV() {
		Assert.assertEquals(2, graph.e(12).bothV().count());

	}

	@Test
	public void testCap() {
		Assert.assertEquals(3, graph.v().has("lang", "java").in("created").groupCount().cap().size());
	}

	@Test
	public void testId() {
		Assert.assertEquals("1", graph.v().has("name", "marko").id().next());
	}

	@Test
	public void testIn() {
		Assert.assertEquals(1, graph.v(4).in().count());
		Assert.assertEquals(3, graph.v(3).in("created").count());
		Assert.assertEquals(2, graph.v(3).in(2, "created").count());
	}

	@Test
	public void testInE() {
		Assert.assertEquals(1, graph.v(4).inE().count());
		Assert.assertEquals(3, graph.v(3).inE("created").count());
		Assert.assertEquals(2, graph.v(3).inE(2, "created").count());
	}

	@Test
	public void testInV() {

		Assert.assertEquals(graph.v(3).next(), graph.e(12).inV().next());
	}

	@Test
	public void testOutV() {
		Assert.assertEquals(graph.v(6).next(), graph.e(12).outV().next());
	}

	@Test
	public void testProperty() {
		Assert.assertEquals("marko", graph.v(1).property("name").next());
	}

	@Test
	public void testLabel() {
		Assert.assertEquals("created", graph.v(6).outE().label().next());
	}

	@Test
	public void testMap() {
		Assert.assertEquals(2, graph.v(1).propertyMap().next().size());
		Assert.assertEquals(6, graph.v().propertyMap("id", "age").count());
	}

	@Test
	public void testMemoize() {
		Assert.assertEquals(2, graph.v().out().as("here").out().memoize("here").property("name").count());
		final Map<?, ?> map = new HashMap<>();
		Assert.assertEquals(2, graph.v().out().as("here").out().memoize("here", map).property("name").count());
		Assert.assertEquals(4, map.size());
	}

	@Test
	public void testOrder() {
		Assert.assertEquals("josh", graph.v().property("name").order().next());
		Assert.assertEquals("vadas", graph.v().property("name").order(Order.DECR).next());

		Assert.assertEquals(graph.v(2).next(), graph.v().order(Comparators.property("name")).out("knows").next());
	}

	@Test
	public void testOut() {
		Assert.assertEquals(3, graph.v(1).out().count());
		Assert.assertEquals(2, graph.v(1).out("knows").count());
		Assert.assertEquals(1, graph.v(1).out(1, "knows").count());
	}

	@Test
	public void testOutE() {
		Assert.assertEquals(3, graph.v(1).outE().count());
		Assert.assertEquals(2, graph.v(1).outE("knows").count());
		Assert.assertEquals(1, graph.v(1).outE(1, "knows").count());
	}

	@Test
	public void testPath() {
		final Path path = graph.v(1).out().path().next();

		Assert.assertEquals(2, path.size());
		Assert.assertTrue(path.get(0, TVertex.class) != null);
		Assert.assertTrue(path.get(1, TVertex.class) != null);
	}

	@Test
	public void testGatherScatter() {
		Assert.assertEquals(6, graph.v().gatherScatter().out().count());

		Assert.assertEquals(3, graph.v().range(0, 2).gatherScatter().out().count());
	}

	@Test
	public void testSelect() {
		final Row<?> row = graph.v(1).as("x").out("knows").as("y").select().next();
		Assert.assertEquals(graph.v(1).next(), row.get(0));
		Assert.assertEquals(graph.v(2).next(), row.get(1));
	}

	@Test
	public void testShuffle() {
		Assert.assertTrue(graph.v(1).out().shuffle().next() instanceof TVertex);
	}

	@Test
	public void testTransform() {
		Assert.assertEquals(new Integer(1), graph.v(1).transform(new TraversalFunction<VertexFrame, Integer>() {

			@Override
			public Integer compute(final VertexFrame argument) {

				return 1;
			}
		}).next());
	}

	@Test
	public void testRange() {
		Assert.assertEquals(graph.v(2).next(), graph.v(1).out().range(1, 1).next());
	}

	@Test
	public void testOr() {
		Assert.assertEquals(3, graph.v().or(new TraversalFunction<VertexFrame, Traversal<?, ?, ?, ?>>() {

			@Override
			public Traversal<?, ?, ?, ?> compute(final VertexFrame argument) {
				return argument.out().has("name");
			}
		}).count());
	}

	@Test
	public void testDedup() {
		Assert.assertEquals(3, graph.v(1).out().in().dedup().count());
	}

	@Test
	public void testExcept() {
		Assert.assertEquals(5, graph.v().except(graph.v(1)).count());
	}

	@Test
	public void testFilter() {
		Assert.assertEquals(2, graph.v().filter(new TraversalFunction<VertexFrame, Boolean>() {

			@Override
			public Boolean compute(final VertexFrame argument) {
				return argument.getProperty("age") != null && argument.getProperty("age", Integer.class) > 29;
			}
		}).count());
	}

	@Test
	public void testHas() {
		Assert.assertEquals(1, graph.v().has("name", "marko").count());
	}

	@Test
	public void testHasNot() {
		Assert.assertEquals(5, graph.v().hasNot("name", "marko").count());
	}

	@Test
	public void testInterval() {
		Assert.assertEquals(3, graph.e().interval("weight", 0.3f, 0.9f).count());
	}

	@Test
	public void testRetain() {
		Assert.assertEquals(1, graph.v().retain(graph.v(1)).count());
	}

	@Test
	public void testSimplePath() {
		Assert.assertEquals(2, graph.v(1).out().in().simplePath().count());
	}

	@Test
	public void testAggregate() {
		final List<VertexFrame> x = new ArrayList<>();
		Assert.assertEquals(graph.v(3).next(), graph.v(1).out().aggregate(x).next());
		Assert.assertEquals(3, graph.v(1).out().aggregate().cap().size());
		Assert.assertEquals(3, x.size());
		Assert.assertTrue(x.get(0) instanceof TVertex);
	}

	@Test
	public void testGroupCount() {
		final Map<VertexFrame, Long> cap = graph.v().out().groupCount().cap();
		Assert.assertEquals(4, cap.size());
		Assert.assertTrue(cap.keySet().iterator().next() instanceof TVertex);

		Assert.assertEquals(4, graph.v().out().groupCount(new TraversalFunction<VertexFrame, String>() {

			@Override
			public String compute(final VertexFrame argument) {
				return argument.getId();

			}
		}).cap().size());

		Assert.assertEquals(6, graph.v().out().groupCount().divert(new SideEffectFunction<Map<VertexFrame, Long>>() {

			@Override
			public void execute(final Map<VertexFrame, Long> o) {
				Assert.assertEquals(4, o.size());
			}
		}).count());

	}

	@Test
	public void testGroupBy() {
		final Map<VertexFrame, List<VertexFrame>> cap = graph.v()
			.groupBy(TraversalFunctions.<VertexFrame>identity(), new TraversalFunction<VertexFrame, Iterator<VertexFrame>>() {

				@Override
				public Iterator<VertexFrame> compute(final VertexFrame argument) {
					return argument.out();
				}

			}).cap();
		Assert.assertEquals(6, cap.size());
		Assert.assertEquals(3, cap.get(graph.v(1).next()).size());
		Assert.assertTrue(cap.get(graph.v(1).next()).iterator().next() instanceof TVertex);

	}

	@Test
	public void testSideEffect() {
		final List<VertexFrame> collected = new ArrayList<>();
		graph.v(1).sideEffect(new SideEffectFunction<VertexFrame>() {

			@Override
			public void execute(final VertexFrame o) {
				collected.add(o);

			}
		}).iterate();
		Assert.assertEquals(1, collected.size());

	}

	@Test
	public void testStore() {
		final Collection<TVertex> x = new ArrayList<>();
		graph.v(1).store(x).next();
		Assert.assertEquals(graph.v(1).next(), x.iterator().next());
		Assert.assertEquals(graph.v(1).next(), graph.v(1).store().cap().iterator().next());
	}

	@Test
	public void testTable() {
		final Table table = graph.v().as("vertex").mark().property("name").as("name").back().property("age").as("age").table().cap();
		Assert.assertEquals(6, table.size());
		Assert.assertTrue(table.get(0).get(0) instanceof TVertex);
	}

	@Test
	public void testTree() {
		final Tree<VertexFrame> tree = graph.v(1).out().out().tree().cap();
		Assert.assertEquals(1, tree.get(graph.v(1).next()).size());
	}

	@Test
	public void testLoop() {
		final List<? extends VertexFrame> list = graph.v(1).loop(new TraversalFunction<VertexFrame, VertexTraversal<?, ?, ?>>() {

			@Override
			public VertexTraversal<?, ?, ?> compute(final VertexFrame argument) {
				return argument.out();
			}
		}, 3).toList();
		Assert.assertEquals(2, list.size());
	}

	@Test
	public void testNext() {
		Assert.assertEquals(graph.v(3).next(), graph.v(6).out("created").next());
	}

	@Test(expected = NoSuchElementException.class)
	public void testNextNoSuchElement() {
		graph.v(6).out("knows").next();
	}

	@Test
	public void testNextOrDefault() {
		final TVertex defaultValue = new TVertex();
		Assert.assertEquals(graph.v(3).next(), graph.v(6).out("created").nextOrDefault(defaultValue));
	}

	@Test
	public void testNextOrDefaultNoSuchElement() {
		final TVertex defaultValue = new TVertex();
		Assert.assertEquals(defaultValue, graph.v(6).out("knows").nextOrDefault(defaultValue));
	}

	@Test
	public void testNextOrDefaultWithKind() {
		final Program defaultValue = new Program();
		Assert.assertEquals(graph.v(3).next(Program.class), graph.v(6).out("created").nextOrDefault(Program.class, defaultValue));
	}

	@Test
	public void testNextOrDefaultWithKindExplicit() {
		final Program defaultValue = new Program();
		Assert.assertEquals(graph.v(3).nextExplicit(Program.class), graph.v(6).out("created").nextOrDefaultExplicit(Program.class, defaultValue));
	}

	@Test
	public void testNextOrDefaultWithKindNoSuchElement() {
		final Person defaultValue = new Person();
		Assert.assertEquals(defaultValue, graph.v(6).out("knows").nextOrDefault(Person.class, defaultValue));
	}

	@Test
	public void testNextOrDefaultWithKindNoSuchElementExplicit() {
		final Person defaultValue = new Person();
		Assert.assertEquals(defaultValue, graph.v(6).out("knows").nextOrDefaultExplicit(Person.class, defaultValue));
	}

	@Test
	public void testLimit() {
		Assert.assertEquals(1, graph.v().limit(1).count());
		Assert.assertEquals(2, graph.v().limit(2).count());
	}

	@Test
	public void testToList() {
		Assert.assertTrue(graph.v().limit(1).toList().iterator().next() instanceof TVertex);
	}

	@Test
	public void testToListWithClass() {
		Assert.assertTrue(graph.v().limit(1).toList(Person.class).iterator().next() != null);
	}

	@Test
	public void testToListWithClassExplicit() {
		Assert.assertTrue(graph.v().limit(1).toListExplicit(Person.class).iterator().next() != null);
	}

	@Test
	public void testToSet() {
		Assert.assertTrue(graph.v().limit(1).toSet().iterator().next() instanceof TVertex);
	}

	@Test
	public void testToSetWithClass() {
		Assert.assertTrue(graph.v().limit(1).toSet(Person.class).iterator().next() != null);
	}

	@Test
	public void testToSetWithClassExplicit() {
		Assert.assertTrue(graph.v().limit(1).toSetExplicit(Person.class).iterator().next() != null);
	}

	@Test
	public void testRemove1() {
		final VertexTraversal<?, ?, ?> traversal = graph.v().has("lang", "java");
		traversal.next();
		traversal.remove();
		Assert.assertEquals(5, graph.v().count());
	}

	@Test
	public void testRemove2() {
		final VertexTraversal<?, ?, ?> traversal = graph.v().has("lang", "java").in();
		traversal.next();
		traversal.remove();
		Assert.assertEquals(5, graph.v().count());
	}
}
