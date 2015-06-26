package com.gentics.mesh.core.data.model.field;

import static org.junit.Assert.*;

import org.junit.Test;

import com.gentics.mesh.core.data.model.node.MeshNodeFieldContainer;
import com.gentics.mesh.core.data.model.node.field.StringField;
import com.gentics.mesh.test.AbstractDBTest;

public class StringFieldTest extends AbstractDBTest {

	@Test
	public void testSimpleString() {
		MeshNodeFieldContainer container = fg.addFramedVertex(MeshNodeFieldContainer.class);
		StringField field = new StringField("test", container);
		assertEquals(2, container.getPropertyKeys().size());
		field.setFieldLabel("dummyLabel");
		field.setFieldName("dummyName");
		assertEquals(4, container.getPropertyKeys().size());
		field.setString("dummyString");
		assertEquals(5, container.getPropertyKeys().size());
	}
}
