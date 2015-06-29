package com.gentics.mesh.core.data.model.field;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.data.model.MeshNodeFieldContainer;
import com.gentics.mesh.core.data.model.impl.MeshNodeFieldContainerImpl;
import com.gentics.mesh.core.data.model.node.field.basic.StringField;
import com.gentics.mesh.core.data.model.node.field.impl.basic.StringFieldImpl;
import com.gentics.mesh.core.data.model.node.field.nesting.SelectField;
import com.gentics.mesh.test.AbstractDBTest;

public class SelectFieldTest extends AbstractDBTest {

	@Test
	public void testStringSelection() {
		MeshNodeFieldContainer container = fg.addFramedVertex(MeshNodeFieldContainerImpl.class);

		SelectField<StringField> field = container.createSelect("dummySelect");
		field.addOption(new StringFieldImpl("test", null));
		assertEquals(1, field.getOptions());
	}

	@Test
	public void testNodeSelection() {

	}

	@Test
	public void testNumberSelection() {

	}

	@Test
	public void testBooleanSelection() {

	}
}
