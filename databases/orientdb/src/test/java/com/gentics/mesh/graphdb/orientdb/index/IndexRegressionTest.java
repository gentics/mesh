package com.gentics.mesh.graphdb.orientdb.index;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import com.orientechnologies.orient.core.index.OCompositeKey;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedEdge;

public class IndexRegressionTest extends AbstractOrientTest {

	public static final String EDGE_LABEL = "HAS_TEST_EDGE";

	public static final String KEY_1 = "key_1";
	public static final String VALUE_1 = "value1";

	public static final String KEY_2 = "key_2";
	public static final String VALUE_2 = "value2";

	public static final String KEY_3 = "key_3";
	public static final String VALUE_3 = "value3";

	private OrientGraphFactory factory;

	@Before
	public void setupDB() {
		factory = new OrientGraphFactory("memory:tinkerpop" + System.currentTimeMillis()).setupPool(16, 100);
		addTypesAndIndices();
	}

	private void addTypesAndIndices() {
		addEdgeType(factory::getNoTx, EDGE_LABEL, null, type -> {

			type.createProperty("out", OType.LINK);
			type.createProperty(KEY_1, OType.STRING);
			type.createProperty(KEY_2, OType.STRING);
			type.createProperty(KEY_3, OType.STRING);

			String fields[] = { "out", KEY_1, KEY_2, KEY_3 };
			String indexName = ("e." + EDGE_LABEL + "_test").toLowerCase();
			String indexType = INDEX_TYPE.NOTUNIQUE_HASH_INDEX.toString();
			ODocument meta = new ODocument().fields("ignoreNullValues", true);

			OIndex idx = type.createIndex(indexName, indexType, null, meta, fields);
			assertNotNull("Index was not created.", idx);

		});

		addVertexType(factory::getNoTx, "NodeImpl", null, null);
		addVertexType(factory::getNoTx, "ContentImpl", null, null);
	}

	Object vertexId;

	@Test
	public void testEdgeLookup() {
		OrientGraph tx = factory.getTx();
		try {
			Vertex v1 = tx.addVertex("class:NodeImpl");
			Vertex v2 = tx.addVertex("class:ContentImpl");
			vertexId = v1.getId();

			Edge edge1 = v1.addEdge(EDGE_LABEL, v2);
			edge1.setProperty(KEY_1, VALUE_1);
			edge1.setProperty(KEY_2, VALUE_2);
			edge1.setProperty(KEY_3, VALUE_3);

			Edge edge2 = v2.addEdge(EDGE_LABEL, v1);
			edge2.setProperty(KEY_1, VALUE_1);
			edge2.setProperty(KEY_2, VALUE_2);
			edge2.setProperty(KEY_3, VALUE_3);

			edge2.remove();
			edge1.remove();

			assertIndex(tx);

			tx.commit();
		} finally {
			tx.shutdown();
		}
	}

	private void assertIndex(OrientGraph tx) {
		OrientGraph tx2 = null;
		if (tx == null) {
			tx2 = factory.getTx();
		}
		try {
			OCompositeKey compositeKey = new OCompositeKey(vertexId, VALUE_1, VALUE_2, VALUE_3);
			Iterator<Edge> it = tx.getEdges("e." + EDGE_LABEL.toLowerCase() + "_test", compositeKey).iterator();
			boolean foundEdges = false;
			while (it.hasNext()) {
				Edge foundEdge = it.next();
				assertNotNull("The iterator indicated with hasNext a element would exist. But we got null.", foundEdge);
				if (foundEdge instanceof WrappedEdge) {
					WrappedEdge wrapped = (WrappedEdge) foundEdge;
					assertNotNull("The base element inside the wrapper must not be null", wrapped.getBaseElement());
				} else {
					fail("We only expect wrapped edges but got " + foundEdge.getClass().getName());
				}
				foundEdges = true;
			}
			assertFalse("The index should not have returned edges", foundEdges);
		} finally {
			if (tx2 != null) {
				tx2.shutdown();
			}
		}
	}
}
