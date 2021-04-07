package com.gentics.mesh.core.data.fieldhandler.microschema;

import static com.gentics.mesh.test.TestSize.FULL;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.fieldhandler.AbstractComparatorStringTest;
import com.gentics.mesh.core.data.schema.handler.AbstractFieldSchemaContainerComparator;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparatorImpl;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.test.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = false)
public class MicroschemaComparatorStringTest extends AbstractComparatorStringTest<MicroschemaModel> {

	@Override
	public AbstractFieldSchemaContainerComparator<MicroschemaModel> getComparator() {
		return new MicroschemaComparatorImpl();
	}

	@Override
	public MicroschemaModel createContainer() {
		return FieldUtil.createMinimalValidMicroschema();
	}

}
