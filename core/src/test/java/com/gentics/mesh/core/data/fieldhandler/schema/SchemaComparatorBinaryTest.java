package com.gentics.mesh.core.data.fieldhandler.schema;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.fieldhandler.AbstractComparatorBinaryTest;
import com.gentics.mesh.core.data.schema.handler.AbstractFieldSchemaContainerComparator;
import com.gentics.mesh.core.data.schema.handler.SchemaComparator;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.util.FieldUtil;

public class SchemaComparatorBinaryTest extends AbstractComparatorBinaryTest<Schema> {

	@Autowired
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
