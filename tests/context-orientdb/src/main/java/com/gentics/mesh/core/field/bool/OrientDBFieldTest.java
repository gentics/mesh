package com.gentics.mesh.core.field.bool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.field.impl.BooleanGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.DateGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.NumberGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.StringGraphFieldImpl;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = false)
public class OrientDBFieldTest extends AbstractMeshTest {

	@Test
	public void testSimpleBoolean() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainerImpl container = ((GraphDBTx) tx).getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			BooleanGraphFieldImpl field = new BooleanGraphFieldImpl("test", container);
			assertEquals(2, container.getPropertyKeys().size());
			assertNull(container.getProperty("test-boolean"));
			field.setBoolean(new Boolean(true));

			assertEquals("true", container.getProperty("test-boolean"));
			// assertEquals(5, container.getPropertyKeys().size());
			field.setBoolean(new Boolean(false));
			assertEquals("false", container.getProperty("test-boolean"));
			field.setBoolean(null);
			assertNull(container.getProperty("test-boolean"));
			assertNull(field.getBoolean());
		}
	}

	@Test
	public void testSimpleString() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainerImpl container = ((GraphDBTx) tx).getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			StringGraphFieldImpl field = new StringGraphFieldImpl("test", container);
			assertEquals(2, container.getPropertyKeys().size());
			field.setString("dummyString");
			assertEquals("dummyString", field.getString());
		}
	}

	@Test
	public void testSimpleDate() {
		try (Tx tx = tx()) {
			Long nowEpoch = System.currentTimeMillis() / 1000;
			NodeGraphFieldContainerImpl container = ((GraphDBTx) tx).getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			DateGraphFieldImpl field = new DateGraphFieldImpl("test", container);
			assertEquals(2, container.getPropertyKeys().size());
			assertNull(container.getProperty("test-date"));
			field.setDate(nowEpoch);
			assertEquals(nowEpoch, Long.valueOf(container.getProperty("test-date")));
			assertEquals(3, container.getPropertyKeys().size());
			field.setDate(null);
			assertNull(container.getProperty("test-date"));
		}
	}

	@Test
	public void testSimpleNumber() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainerImpl container = ((GraphDBTx) tx).getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			NumberGraphFieldImpl field = new NumberGraphFieldImpl("test", container);
			assertEquals(2, container.getPropertyKeys().size());
			assertNull(container.getProperty("test-number"));
			assertEquals(2, container.getPropertyKeys().size());
			field.setNumber(42);
			assertEquals(42, field.getNumber());
			assertEquals(Integer.valueOf(42), container.getProperty("test-number"));
			assertEquals(3, container.getPropertyKeys().size());
			field.setNumber(null);
			assertNull(field.getNumber());
			assertNull(container.getProperty("test-number"));
		}
	}
}
