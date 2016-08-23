package com.gentics.mesh.core.data.fieldhandler.microschema;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.fieldhandler.AbstractComparatorListSchemaTest;
import com.gentics.mesh.core.data.schema.handler.AbstractFieldSchemaContainerComparator;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.rest.schema.Microschema;

public class MicroschemaComparatorListSchemaTest extends AbstractComparatorListSchemaTest<Microschema> {

	protected MicroschemaComparator comparator;

	@Override
	public AbstractFieldSchemaContainerComparator<Microschema> getComparator() {
		return comparator;
	}

	@Override
	public Microschema createContainer() {
		Microschema microschema = FieldUtil.createMinimalValidMicroschema();
		microschema.addField(FieldUtil.createStringFieldSchema("fieldA"));
		microschema.addField(FieldUtil.createStringFieldSchema("fieldB"));
		return microschema;
	}

}
