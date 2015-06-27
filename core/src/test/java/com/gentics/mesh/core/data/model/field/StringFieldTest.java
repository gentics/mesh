package com.gentics.mesh.core.data.model.field;

import static org.junit.Assert.*;

import org.junit.Test;

import com.gentics.mesh.core.data.model.impl.MeshNodeFieldContainerImpl;
import com.gentics.mesh.core.data.model.node.field.impl.StringFieldImpl;
import com.gentics.mesh.test.AbstractDBTest;

public class StringFieldTest extends AbstractDBTest {

	@Test
	public void testSimpleString() {
		MeshNodeFieldContainerImpl container = fg.addFramedVertex(MeshNodeFieldContainerImpl.class);
		StringFieldImpl field = new StringFieldImpl("test", container);
		assertEquals(2, container.getPropertyKeys().size());
		field.setFieldLabel("dummyLabel");
		field.setFieldName("dummyName");
		assertEquals(4, container.getPropertyKeys().size());
		field.setString("dummyString");
		assertEquals(5, container.getPropertyKeys().size());
	}
}
