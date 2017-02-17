package com.gentics.mesh.core.data.fieldhandler.schema;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.fieldhandler.AbstractComparatorBinaryTest;
import com.gentics.mesh.core.data.schema.handler.AbstractFieldSchemaContainerComparator;
import com.gentics.mesh.core.data.schema.handler.SchemaComparator;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, useTinyDataset = false, startServer = false)
public class SchemaComparatorBinaryTest extends AbstractComparatorBinaryTest<Schema> {

	@Override
	public AbstractFieldSchemaContainerComparator<Schema> getComparator() {
		return new SchemaComparator();
	}

	@Override
	public Schema createContainer() {
		return FieldUtil.createMinimalValidSchema();
	}
}
