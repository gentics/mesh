package com.gentics.mesh.core.data.model.field;

import org.junit.Test;

import com.gentics.mesh.core.data.model.node.field.impl.ListFieldImpl;
import com.gentics.mesh.core.data.model.node.field.impl.StringFieldImpl;
import com.gentics.mesh.test.AbstractDBTest;

public class ListFieldTest extends AbstractDBTest {

	@Test
	public void testSimpleList() {
		ListFieldImpl<StringFieldImpl> list = fg.addFramedVertex(ListFieldImpl.class);
		list.getList().add(new StringFieldImpl("test", null));
	}

}
