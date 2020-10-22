package com.gentics.mesh.graphdb.orientdb.index;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import com.orientechnologies.orient.core.index.OCompositeKey;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

public class IndexRegression3Test extends AbstractOrientTest {

	public static final String EDGE_LABEL = "HAS_TEST_EDGE";
	public static final String IDX_POSTFIX = "_branch_type_lang";
	public static final String INDEX_NAME = "e." + EDGE_LABEL.toLowerCase() + IDX_POSTFIX;

	public static final String TYPE_INITIAL = "I";
	public static final String TYPE_PUBLISHED = "P";
	public static final String TYPE_DRAFT = "D";
	public static final String BRANCH_UUID = "branchUuidValue";
	public static final String LANG_EN = "en";

	public static final String TYPE_KEY = "type";
	public static final String LANGUAGE_KEY = "language";
	public static final String BRANCH_KEY = "branch";

	private OrientGraphFactory factory;

	@Before
	public void setupDB() {
		factory = new OrientGraphFactory("memory:tinkerpop" + System.currentTimeMillis()).setupPool(16, 100);
		addTypesAndIndices();
	}

	private void addTypesAndIndices() {
		addEdgeType(factory::getNoTx, EDGE_LABEL, null, type -> {

			type.createProperty("out", OType.LINK);
			type.createProperty(TYPE_KEY, OType.STRING);
			type.createProperty(LANGUAGE_KEY, OType.STRING);
			type.createProperty(BRANCH_KEY, OType.STRING);

			String indexType = INDEX_TYPE.NOTUNIQUE_HASH_INDEX.toString();
			ODocument meta = new ODocument().fields("ignoreNullValues", true);

			String fieldsA[] = { "out", BRANCH_KEY, TYPE_KEY, LANGUAGE_KEY };
			OIndex idxA = type.createIndex(INDEX_NAME, indexType, null, meta, fieldsA);
			assertNotNull("Index was not created.", idxA);

		});

		addVertexType(factory::getNoTx, "NodeImpl", null, null);
		addVertexType(factory::getNoTx, "ContentImpl", null, null);
	}

	@Test
	public void testEdgeLookup() {
		Object nodeId = addGraph();
		deleteAndLookup(nodeId);
	}

	private Object addGraph() {
		OrientGraph tx = factory.getTx();
		Object id;
		try {
			Vertex node = tx.addVertex("class:NodeImpl");
			Vertex draftContent = tx.addVertex("class:ContentImpl");
			Vertex initialContent = tx.addVertex("class:ContentImpl");
			id = node.getId();

			Edge initialEdge = node.addEdge(EDGE_LABEL, initialContent);
			initialEdge.setProperty(TYPE_KEY, TYPE_INITIAL);
			initialEdge.setProperty(LANGUAGE_KEY, LANG_EN);
			initialEdge.setProperty(BRANCH_KEY, BRANCH_UUID);

			Edge draftEdge = node.addEdge(EDGE_LABEL, draftContent);
			draftEdge.setProperty(TYPE_KEY, TYPE_DRAFT);
			draftEdge.setProperty(LANGUAGE_KEY, LANG_EN);
			draftEdge.setProperty(BRANCH_KEY, BRANCH_UUID);

			tx.commit();
		} finally {
			tx.shutdown();
		}
		return id;
	}

	private void deleteAndLookup(Object nodeId) {
		OrientGraph tx = factory.getTx();
		try {
			assertInitialEdgeLookup(INDEX_NAME, nodeId, tx, true);
			tx.commit();
		} finally {
			tx.shutdown();
		}
	}

	private void assertInitialEdgeLookup(String indexName, Object nodeId, OrientGraph tx, boolean expectEdge) {
		System.out.println("Index lookup of {" + indexName + "} with composite key.");
		OCompositeKey compositeKey = new OCompositeKey(nodeId, BRANCH_UUID, TYPE_INITIAL, LANG_EN);

		Iterator<Edge> it = tx.getEdges(indexName, compositeKey).iterator();
		boolean foundEdges = false;
		while (it.hasNext()) {
			Edge foundEdge = it.next();
			assertNotNull("The iterator indicated with hasNext a element would exist. But we got null.", foundEdge);

			Vertex inV = foundEdge.getVertex(Direction.IN);
			assertNotNull(inV);
			Vertex outV = foundEdge.getVertex(Direction.OUT);
			assertNotNull(outV);
			foundEdges = true;
		}
		if (expectEdge) {
			assertTrue("The index should have returned edges", foundEdges);
		} else {
			assertFalse("The index should not have returned edges", foundEdges);
		}

		// // Also assert direct index lookup
		// OIndexManager manager = tx.getRawGraph().getMetadata().getIndexManager();
		// List<?> entryList = (List<?>) manager.getIndex(indexName).get(compositeKey);
		// if (expectEdge) {
		// assertFalse("The index should have returned a value but got " + entryList, entryList.isEmpty());
		// } else {
		// assertTrue("The index should not have returned a value but got " + entryList, entryList.isEmpty());
		// }

	}
}
