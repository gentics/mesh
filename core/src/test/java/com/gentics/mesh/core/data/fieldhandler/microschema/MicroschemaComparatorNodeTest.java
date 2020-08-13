package com.gentics.mesh.core.data.fieldhandler.microschema;

import static com.gentics.mesh.test.TestSize.FULL;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.fieldhandler.AbstractComparatorNodeTest;
import com.gentics.mesh.core.data.schema.handler.AbstractFieldSchemaContainerComparator;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = false)
public class MicroschemaComparatorNodeTest extends AbstractComparatorNodeTest<MicroschemaModel> {

	@Override
	public AbstractFieldSchemaContainerComparator<MicroschemaModel> getComparator() {
		return new MicroschemaComparator();
	}

	@Override
	public MicroschemaModel createContainer() {
		MicroschemaModel microschemaModel = FieldUtil.createMinimalValidMicroschema();
		return microschemaModel;
	}

}
