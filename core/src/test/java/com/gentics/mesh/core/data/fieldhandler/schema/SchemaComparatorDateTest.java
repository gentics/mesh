package com.gentics.mesh.core.data.fieldhandler.schema;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.fieldhandler.AbstractComparatorDateTest;
import com.gentics.mesh.core.data.schema.handler.AbstractFieldSchemaContainerComparator;
import com.gentics.mesh.core.data.schema.handler.SchemaComparator;
import com.gentics.mesh.core.rest.schema.Schema;

public class SchemaComparatorDateTest extends AbstractComparatorDateTest<Schema> {

	protected SchemaComparator comparator;

	@Override
	public AbstractFieldSchemaContainerComparator<Schema> getComparator() {
		return comparator;
	}

	@Override
	public Schema createContainer() {
		return FieldUtil.createMinimalValidSchema();
	}

}
