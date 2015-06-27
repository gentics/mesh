package com.gentics.mesh.core.data.model.field;

import org.junit.Test;

import com.gentics.mesh.core.data.model.node.field.impl.SelectFieldImpl;
import com.gentics.mesh.test.AbstractDBTest;

public class SelectFieldTest extends AbstractDBTest{

	
	@Test
	public void testSimpleSelect() {
		SelectFieldImpl field = fg.addFramedVertex(SelectFieldImpl.class);
	}
}
