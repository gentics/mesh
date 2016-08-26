package com.gentics.mesh.core.data.fieldhandler.schema;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.fieldhandler.AbstractComparatorBooleanTest;
import com.gentics.mesh.core.data.schema.handler.AbstractFieldSchemaContainerComparator;
import com.gentics.mesh.core.data.schema.handler.SchemaComparator;
import com.gentics.mesh.core.rest.schema.Schema;

public class SchemaComparatorBooleanTest extends AbstractComparatorBooleanTest<Schema> {

	@Override
	public AbstractFieldSchemaContainerComparator<Schema> getComparator() {
		return new SchemaComparator();
	}

	@Override
	public Schema createContainer() {
		return FieldUtil.createMinimalValidSchema();
	}

}
