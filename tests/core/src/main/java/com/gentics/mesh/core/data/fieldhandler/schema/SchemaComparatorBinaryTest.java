package com.gentics.mesh.core.data.fieldhandler.schema;

import static com.gentics.mesh.test.TestSize.FULL;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.fieldhandler.AbstractComparatorBinaryTest;
import com.gentics.mesh.core.data.schema.handler.AbstractFieldSchemaContainerComparator;
import com.gentics.mesh.core.data.schema.handler.SchemaComparatorImpl;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = false)
public class SchemaComparatorBinaryTest extends AbstractComparatorBinaryTest<SchemaModel> {

	@Override
	public AbstractFieldSchemaContainerComparator<SchemaModel> getComparator() {
		return new SchemaComparatorImpl();
	}

	@Override
	public SchemaModel createContainer() {
		return FieldUtil.createMinimalValidSchema();
	}
}
