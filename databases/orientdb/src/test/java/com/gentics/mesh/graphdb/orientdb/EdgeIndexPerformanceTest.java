package com.gentics.mesh.graphdb.orientdb;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.orientechnologies.orient.core.index.OCompositeKey;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientEdgeType;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;

public class EdgeIndexPerformanceTest {

	private static OrientGraphFactory factory = new OrientGraphFactory("memory:tinkerpop");

	private final static int nDocuments = 2000;
	private final static int nChecks = 4000;

	private static List<OrientVertex> items;
	private static OrientVertex root;

	@BeforeClass
	public static void setupDatabase() {
		setupTypesAndIndices(factory);

		root = createRoot(factory);
		items = createData(root, factory, nDocuments);
	}

	private static void setupTypesAndIndices(OrientGraphFactory factory2) {
		OrientGraph g = factory.getTx();
		try {
			// g.setUseClassForEdgeLabel(true);
			g.setUseLightweightEdges(false);
			g.setUseVertexFieldsForEdgeLabels(false);
		} finally {
			g.shutdown();
		}

		try {
			g = factory.getTx();

			OrientEdgeType e = g.createEdgeType("HAS_ITEM");
			e.createProperty("in", OType.LINK);
			e.createProperty("out", OType.LINK);
			e.createIndex("e.has_item", OClass.INDEX_TYPE.UNIQUE_HASH_INDEX, "out", "in");

			OrientVertexType v = g.createVertexType("root", "V");
			v.createProperty("name", OType.STRING);

			v = g.createVertexType("item", "V");
			v.createProperty("name", OType.STRING);
			v.createIndex("item", OClass.INDEX_TYPE.UNIQUE_HASH_INDEX, "name");

		} finally {
			g.shutdown();
		}

	}

	@Test
	public void testVertexMigration() {
		// Create vertex without type
		OrientGraph g = factory.getTx();
		OrientVertex v;
		try {
			v = g.addVertex(null);
			v.setProperty("name", "extraName");
		} finally {
			g.shutdown();
		}

		// Migrate vertex
		OrientBaseGraph tx = factory.getTx();
		try {
			v.moveToClass("item");
		} finally {
			tx.shutdown();
		}

		// Search for vertex
		g = factory.getTx();
		System.out.println("Size: " + g.getVertexType("item").getClassIndex("item").getSize());
		try {
			Iterable<Vertex> vertices = g.getVertices("item", new String[] { "name" }, new Object[] { "extraName" });
			assertTrue(vertices.iterator().hasNext());
		} finally {
			g.shutdown();
		}

	}

	private static List<OrientVertex> createData(OrientVertex root, OrientGraphFactory factory, int count) {
		OrientGraph g = factory.getTx();
		try {
			System.out.println("Creating {" + count + "} items.");
			List<OrientVertex> items = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				OrientVertex item = g.addVertex("class:item");
				item.setProperty("name", "item_" + i);
				items.add(item);
				root.addEdge("HAS_ITEM", item);
			}
			return items;
		} finally {
			g.shutdown();
		}
	}

	private static OrientVertex createRoot(OrientGraphFactory factory) {
		OrientGraph g = factory.getTx();
		try {
			OrientVertex root = g.addVertex("class:root");
			root.setProperty("name", "root vertex");
			return root;
		} finally {
			g.shutdown();
		}
	}

	@Test
	public void testVertexIndex() {
		OrientGraph g = factory.getTx();
		System.out.println("Size: " + g.getVertexType("item").getClassIndex("item").getSize());

		try {
			long start = System.currentTimeMillis();
			for (int i = 0; i < nChecks; i++) {
				int randomDocumentId = (int) (Math.random() * nDocuments);
				Iterable<Vertex> vertices = g.getVertices("item", new String[] { "name" }, new Object[] { "item_" + randomDocumentId });
				// Iterable<Vertex> vertices = g.getVertices("item.name", "item_" + randomDocumentId);
				assertTrue(vertices.iterator().hasNext());
			}
			long dur = System.currentTimeMillis() - start;
			double perCheck = ((double) dur / (double) nChecks);
			System.out.println("[graph.getVertices] Duration per lookup: " + perCheck);
			System.out.println("[graph.getVertices] Duration: " + dur);
		} finally {
			g.shutdown();
		}
	}

	@Test
	public void testEdgeIndexViaGraphGetEdges() throws Exception {
		OrientGraph g = factory.getTx();
		try {
			for (OIndex<?> index : g.getRawGraph().getMetadata().getIndexManager().getIndexes()) {

				System.out.println(index.getName() + " size: " + index.getSize());
			}
			// OIndex<?> index = g.getRawGraph().getMetadata().getIndexManager().getIndex("edge.has_item");
			// assertNotNull("Index could not be found", index);
		} finally {
			g.shutdown();
		}

		double total = 0;
		int nRuns = 20;
		for (int e = 0; e < nRuns; e++) {
			g = factory.getTx();
			try {
				long start = System.currentTimeMillis();
				for (int i = 0; i < nChecks; i++) {
					OrientVertex randomDocument = items.get((int) (Math.random() * items.size()));
					Iterable<Edge> edges = g.getEdges("e.has_item", new OCompositeKey(root.getId(), randomDocument.getId()));
					assertTrue(edges.iterator().hasNext());
				}
				long dur = System.currentTimeMillis() - start;
				System.out.println("[graph.getEdges] Duration: " + dur);
				double perCheck = ((double) dur / (double) nChecks);
				total += perCheck;
				System.out.println("[graph.getEdges] Duration per lookup: " + perCheck);
			} finally {
				g.shutdown();
			}
		}
		System.out.println("Average: " + (total / (double) nRuns));
	}

	@Test
	public void testEdgeIndexViaRootGetEdgesWithoutTarget() throws Exception {
		OrientGraph g = factory.getTx();
		try {
			long start = System.currentTimeMillis();
			for (int i = 0; i < nChecks; i++) {
				OrientVertex randomDocument = items.get((int) (Math.random() * items.size()));
				Iterable<Edge> edges = root.getEdges(Direction.OUT, "HAS_ITEM");
				boolean found = false;
				for (Edge edge : edges) {
					if (edge.getVertex(Direction.IN).equals(randomDocument)) {
						found = true;
						break;
					}
				}
				assertTrue(found);
			}
			long dur = System.currentTimeMillis() - start;
			System.out.println("[root.getEdges - iterating] Duration: " + dur);
			System.out.println("[root.getEdges - iterating] Duration per lookup: " + ((double) dur / (double) nChecks));
		} finally {
			g.shutdown();
		}
	}

	@Test
	public void testEdgeIndexViaRootGetEdges() throws Exception {
		OrientGraph g = factory.getTx();
		try {
			long start = System.currentTimeMillis();
			for (int i = 0; i < nChecks; i++) {
				OrientVertex randomDocument = items.get((int) (Math.random() * items.size()));
				Iterable<Edge> edges = root.getEdges(randomDocument, Direction.OUT, "HAS_ITEM");
				assertTrue(edges.iterator().hasNext());
			}
			long dur = System.currentTimeMillis() - start;
			System.out.println("[root.getEdges] Duration: " + dur);
			System.out.println("[root.getEdges] Duration per lookup: " + ((double) dur / (double) nChecks));
		} finally {
			g.shutdown();
		}
	}

	@Test
	public void testEdgeIndexViaQuery() throws Exception {
		OrientGraph g = factory.getTx();
		try {
			System.out.println("Checking edge");
			long start = System.currentTimeMillis();
			for (int i = 0; i < nChecks; i++) {
				OrientVertex randomDocument = items.get((int) (Math.random() * items.size()));

				OCommandSQL cmd = new OCommandSQL("select from index:e.has_item where key=?");
				OCompositeKey key = new OCompositeKey(root.getId(), randomDocument.getId());

				assertTrue(((Iterable<Vertex>) g.command(cmd).execute(key)).iterator().hasNext());
			}
			long dur = System.currentTimeMillis() - start;
			System.out.println("[query] Duration: " + dur);
			System.out.println("[query] Duration per lookup: " + ((double) dur / (double) nChecks));
		} finally {
			g.shutdown();
		}
	}

}
