package com.gentics.mesh.graphdb.orientdb;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraphFactory;
import org.apache.tinkerpop.gremlin.orientdb.OrientVertex;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.BeforeClass;
import org.junit.Test;

import com.orientechnologies.orient.core.index.OCompositeKey;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientEdgeType;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;

public class EdgeIndexPerformanceTest {

	private static OrientGraphFactory factory = new OrientGraphFactory("memory:tinkerpop");

	private final static int nDocuments = 2000;
	private final static int nChecks = 4000;

	private static List<Vertex> items;
	private static Vertex root;

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
			g.close();
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
			v.createIndex("item", OClass.INDEX_TYPE.FULLTEXT_HASH_INDEX, "name");

		} finally {
			g.close();
		}

	}

	@Test
	public void testVertexMigration() {
		// Create vertex without type
		OrientGraph g = factory.getTx();
		OrientVertex v;
		try {
			v = g.addVertex(null);
			v.property("name", "extraName");
		} finally {
			g.close();
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
			Iterator<Vertex> vertices = g.vertices("item", new String[] { "name" }, new Object[] { "extraName" });
			assertTrue(vertices.hasNext());
		} finally {
			g.close();
		}

	}

	private static List<Vertex> createData(Vertex root, OrientGraphFactory factory, int count) {
		Graph g = factory.getTx();
		try {
			System.out.println("Creating {" + count + "} items.");
			List<Vertex> items = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				Vertex item = g.addVertex("class:item");
				item.property("name", "item_" + i);
				items.add(item);
				root.addEdge("HAS_ITEM", item);
			}
			return items;
		} finally {
			g.close();
		}
	}

	private static Vertex createRoot(OrientGraphFactory factory) {
		OrientGraph g = factory.getTx();
		try {
			Vertex root = g.addVertex("class:root");
			root.property("name", "root vertex");
			return root;
		} finally {
			g.close();
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
				Iterator<Vertex> vertices = g.vertices("item", new String[] { "name" }, new Object[] { "item_" + randomDocumentId });
				// Iterable<Vertex> vertices = g.getVertices("item.name", "item_" + randomDocumentId);
				assertTrue(vertices.hasNext());
			}
			long dur = System.currentTimeMillis() - start;
			double perCheck = ((double) dur / (double) nChecks);
			System.out.println("[graph.getVertices] Duration per lookup: " + perCheck);
			System.out.println("[graph.getVertices] Duration: " + dur);
		} finally {
			g.close();
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
			g.close();
		}

		double total = 0;
		int nRuns = 20;
		for (int e = 0; e < nRuns; e++) {
			g = factory.getTx();
			try {
				long start = System.currentTimeMillis();
				for (int i = 0; i < nChecks; i++) {
					Vertex randomDocument = items.get((int) (Math.random() * items.size()));
					Iterator<Edge> edges = g.edges("e.has_item", new OCompositeKey(root.id(), randomDocument.id()));
					assertTrue(edges.hasNext());
				}
				long dur = System.currentTimeMillis() - start;
				System.out.println("[graph.getEdges] Duration: " + dur);
				double perCheck = ((double) dur / (double) nChecks);
				total += perCheck;
				System.out.println("[graph.getEdges] Duration per lookup: " + perCheck);
			} finally {
				g.close();
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
				Vertex randomDocument = items.get((int) (Math.random() * items.size()));
				Iterator<Edge> edges = root.edges(Direction.OUT, "HAS_ITEM");
				boolean found = false;
				for (Edge edge : (Iterable<Edge>) () -> edges) {
					if (edge.inVertex().equals(randomDocument)) {
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
			g.close();
		}
	}

	@Test
	public void testEdgeIndexViaRootGetEdges() throws Exception {
		OrientGraph g = factory.getTx();
		try {
			long start = System.currentTimeMillis();
			for (int i = 0; i < nChecks; i++) {
				Vertex randomDocument = items.get((int) (Math.random() * items.size()));
				Iterator<Edge> edges = root.edges(randomDocument, Direction.OUT, "HAS_ITEM");
				assertTrue(edges.hasNext());
			}
			long dur = System.currentTimeMillis() - start;
			System.out.println("[root.getEdges] Duration: " + dur);
			System.out.println("[root.getEdges] Duration per lookup: " + ((double) dur / (double) nChecks));
		} finally {
			g.close();
		}
	}

	@Test
	public void testEdgeIndexViaQuery() throws Exception {
		OrientGraph g = factory.getTx();
		try {
			System.out.println("Checking edge");
			long start = System.currentTimeMillis();
			for (int i = 0; i < nChecks; i++) {
				Vertex randomDocument = items.get((int) (Math.random() * items.size()));

				OCommandSQL cmd = new OCommandSQL("select from index:e.has_item where key=?");
				OCompositeKey key = new OCompositeKey(root.id(), randomDocument.id());

				assertTrue(((Iterable<Vertex>) g.command(cmd).execute(key)).iterator().hasNext());
			}
			long dur = System.currentTimeMillis() - start;
			System.out.println("[query] Duration: " + dur);
			System.out.println("[query] Duration per lookup: " + ((double) dur / (double) nChecks));
		} finally {
			g.close();
		}
	}

}
