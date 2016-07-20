package com.gentics.mesh.core.data.fieldhandler.microschema;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.fieldhandler.AbstractComparatorNodeTest;
import com.gentics.mesh.core.data.schema.handler.AbstractFieldSchemaContainerComparator;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.rest.schema.Microschema;

public class MicroschemaComparatorNodeTest extends AbstractComparatorNodeTest<Microschema> {

	@Autowired
	protected MicroschemaComparator comparator;

	@Override
	public AbstractFieldSchemaContainerComparator<Microschema> getComparator() {
		return comparator;
	}

	@Override
	public Microschema createContainer() {
		Microschema microschema = FieldUtil.createMinimalValidMicroschema();
		return microschema;
	}

}
