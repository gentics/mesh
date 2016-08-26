package com.gentics.mesh.core.data.fieldhandler.microschema;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.fieldhandler.AbstractComparatorBooleanTest;
import com.gentics.mesh.core.data.schema.handler.AbstractFieldSchemaContainerComparator;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.rest.schema.Microschema;

public class MicroschemaComparatorBooleanTest extends AbstractComparatorBooleanTest<Microschema> {

	@Override
	public AbstractFieldSchemaContainerComparator<Microschema> getComparator() {
		return new MicroschemaComparator();
	}

	@Override
	public Microschema createContainer() {
		return FieldUtil.createMinimalValidMicroschema();
	}
}
