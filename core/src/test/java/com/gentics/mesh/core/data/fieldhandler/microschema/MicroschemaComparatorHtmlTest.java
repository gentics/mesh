package com.gentics.mesh.core.data.fieldhandler.microschema;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.fieldhandler.AbstractComparatorHtmlTest;
import com.gentics.mesh.core.data.schema.handler.AbstractFieldSchemaContainerComparator;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.util.FieldUtil;

public class MicroschemaComparatorHtmlTest extends AbstractComparatorHtmlTest<Microschema> {

	@Autowired
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
