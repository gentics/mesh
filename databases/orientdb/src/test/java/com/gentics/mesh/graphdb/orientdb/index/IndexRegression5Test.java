package com.gentics.mesh.graphdb.orientdb.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.Before;
import org.junit.Test;

import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

/**
 * This test creates a parent child relationship graph without edges. The edges are substituted by embedded sets of uuids.
 */
public class IndexRegression5Test extends AbstractOrientTest {

	// Types
	public static final String NODE_TYPE = "NodeImpl";
	public static final String CONTENT_TYPE = "ContentImpl";
	public static final String CONTENT_EDGE_LABEL = "HAS_CONTENT";

	// Indices
	public static final String IDX_POSTFIX = "_parents";
	public static final String INDEX_NAME = NODE_TYPE + IDX_POSTFIX;

	// Props
	public static final String PARENTS_KEY = "parent";
	public static final String UUID_KEY = "uuid";
	private OrientGraphFactory factory;

	@Before
	public void setupDB() {
		factory = new OrientGraphFactory("memory:tinkerpop" + System.currentTimeMillis()).setupPool(16, 100);
		addTypesAndIndices();
	}

	private void addTypesAndIndices() {
		addEdgeType(factory::getNoTx, CONTENT_EDGE_LABEL, null, null);
		addVertexType(factory::getNoTx, NODE_TYPE, null, type -> {
			type.createProperty(UUID_KEY, OType.STRING);
			type.createProperty(PARENTS_KEY, OType.EMBEDDEDSET, OType.STRING);

			String indexType = INDEX_TYPE.NOTUNIQUE_HASH_INDEX.toString();
			ODocument meta = new ODocument().fields("ignoreNullValues", true);

			String fields[] = { PARENTS_KEY };
			OIndex idx = type.createIndex(INDEX_NAME, indexType, null, meta, fields);
			assertNotNull("Index was not created.", idx);
		});
		addVertexType(factory::getNoTx, CONTENT_TYPE, null, null);
	}

	@Test
	public void testVertexLookup() {
		addGraph();
		System.out.println();

		// Now iterate tree and delete the elements.
		// During iteration the index on NodeImpl vertices will be utilized.
		OrientGraph tx = factory.getTx();
		try {
			delete(tx, "R0");
			tx.commit();
		} finally {
			tx.shutdown();
		}
	}

	private void delete(OrientGraph tx, String uuid) {
		List<String> children = getChildren(uuid, tx);
		switch (uuid) {
		case "A":
		case "B":
			assertEquals("Each element on level one should have two children", 2, children.size());
			break;
		case "R0":
			assertEquals("The root level element should have 2 children", 2, children.size());
			break;
		default:
			assertEquals("Leaves should not have children", 0, children.size());
		}
		for (String childUuid : children) {
			delete(tx, childUuid);
		}
		System.out.println("Deleting content of child " + uuid);
		Vertex vertex = tx.getVertices(UUID_KEY, uuid).iterator().next();
		System.out.println("Loaded vertex " + uuid + " " + vertex.getId());
		vertex.getEdges(Direction.OUT, CONTENT_EDGE_LABEL).forEach(e -> {
			e.remove();
			System.out.println("Removed edge " + e.getId());
		});

	}

	private Object addGraph() {
		OrientGraph tx = factory.getTx();
		try {
			Vertex root = createNode(tx, null, "R0");

			Vertex A = createNode(tx, root, "A");
			Vertex A_1 = createNode(tx, A, "A_1");
			Vertex A_2 = createNode(tx, A, "A_2");

			Vertex B = createNode(tx, root, "B");
			Vertex B_1 = createNode(tx, B, "B_1");
			Vertex B_2 = createNode(tx, B, "B_2");

			tx.commit();
			return root.getId();
		} finally {
			tx.shutdown();
		}
	}

	private List<String> getChildren(String parentUuid, OrientGraph tx) {
		System.out.println("Index lookup with key: " + parentUuid);
		String key[] = { PARENTS_KEY };
		String value[] = { parentUuid };
		return StreamSupport.stream(tx.getVertices(NODE_TYPE, key, value).spliterator(), false).map(v -> {
			String uuid = v.getProperty(UUID_KEY);
			return uuid;
		}).collect(Collectors.toList());

	}

	private Vertex createNode(OrientGraph tx, Vertex parent, String uuid) {
		Vertex node = tx.addVertex("class:" + NODE_TYPE);
		node.setProperty(UUID_KEY, uuid);
		if (parent != null) {
			String parentUuid = parent.getProperty(UUID_KEY);
			node.setProperty(PARENTS_KEY, Collections.singleton(parentUuid));
			System.out.println("Created node " + uuid + " with parent " + parentUuid);
		}

		// Add a content vertex to the given node vertex
		Vertex content = tx.addVertex("class:" + CONTENT_TYPE);
		node.addEdge(CONTENT_EDGE_LABEL, content);

		return node;
	}

}
