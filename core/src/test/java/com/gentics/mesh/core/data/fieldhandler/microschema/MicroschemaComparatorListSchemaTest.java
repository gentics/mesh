package com.gentics.mesh.core.data.fieldhandler.microschema;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.fieldhandler.AbstractComparatorListSchemaTest;
import com.gentics.mesh.core.data.schema.handler.AbstractFieldSchemaContainerComparator;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.util.FieldUtil;

public class MicroschemaComparatorListSchemaTest extends AbstractComparatorListSchemaTest<Microschema> {

	@Autowired
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
