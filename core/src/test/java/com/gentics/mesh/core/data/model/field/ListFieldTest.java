package com.gentics.mesh.core.data.model.field;

import org.junit.Test;

import com.gentics.mesh.core.data.model.node.field.ListField;
import com.gentics.mesh.core.data.model.node.field.StringField;
import com.gentics.mesh.test.AbstractDBTest;

public class ListFieldTest extends AbstractDBTest {

	@Test
	public void testSimpleList() {
		ListField<StringField> list = fg.addFramedVertex(ListField.class);
		list.getList().add(new StringField("test", null));
	}

}
