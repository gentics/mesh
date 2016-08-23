package com.gentics.mesh.core.data.fieldhandler.microschema;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.fieldhandler.AbstractComparatorStringTest;
import com.gentics.mesh.core.data.schema.handler.AbstractFieldSchemaContainerComparator;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.rest.schema.Microschema;

public class MicroschemaComparatorStringTest extends AbstractComparatorStringTest<Microschema> {

	protected MicroschemaComparator comparator;

	@Override
	public AbstractFieldSchemaContainerComparator<Microschema> getComparator() {
		return comparator;
	}

	@Override
	public Microschema createContainer() {
		return FieldUtil.createMinimalValidMicroschema();
	}

}
