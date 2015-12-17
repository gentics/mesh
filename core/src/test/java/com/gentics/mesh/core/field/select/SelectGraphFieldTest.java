package com.gentics.mesh.core.field.select;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.StringGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.nesting.SelectGraphField;
import com.gentics.mesh.core.field.bool.AbstractBasicDBTest;

public class SelectGraphFieldTest extends AbstractBasicDBTest {

	@Test
	@Ignore("Not yet implemented")
	public void testStringSelection() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);

		SelectGraphField<StringGraphField> field = container.createSelect("dummySelect");
		field.addOption(new StringGraphFieldImpl("test", null));
		assertEquals(1, field.getOptions().size());
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
