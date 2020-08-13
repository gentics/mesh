package com.gentics.mesh.core.data.fieldhandler.schema;

import static com.gentics.mesh.test.TestSize.FULL;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.fieldhandler.AbstractComparatorMicronodeTest;
import com.gentics.mesh.core.data.schema.handler.AbstractFieldSchemaContainerComparator;
import com.gentics.mesh.core.data.schema.handler.SchemaComparator;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = false)
public class SchemaComparatorMicronodeTest extends AbstractComparatorMicronodeTest<SchemaModel> {

	@Override
	public AbstractFieldSchemaContainerComparator<SchemaModel> getComparator() {
		return new SchemaComparator();
	}

	@Override
	public SchemaModel createContainer() {
		return FieldUtil.createMinimalValidSchema();
	}

}
