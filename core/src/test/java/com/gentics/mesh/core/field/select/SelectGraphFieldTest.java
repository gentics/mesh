package com.gentics.mesh.core.field.select;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.field.basic.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.basic.StringGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.nesting.GraphSelectField;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.test.AbstractDBTest;

public class SelectGraphFieldTest extends AbstractDBTest {

	@Test
	public void testStringSelection() {
		try (Trx tx = new Trx(database)) {
			NodeFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);

			GraphSelectField<StringGraphField> field = container.createSelect("dummySelect");
			field.addOption(new StringGraphFieldImpl("test", null));
			assertEquals(1, field.getOptions().size());
		}
	}

	@Test
	@Ignore("Not yet implemented")
	public void testNodeSelection() {
		fail("Not yet implemented");
	}

	@Test
	@Ignore("Not yet implemented")
	public void testNumberSelection() {
		fail("Not yet implemented");
	}

	@Test
	@Ignore("Not yet implemented")
	public void testBooleanSelection() {
		fail("Not yet implemented");
	}
}
