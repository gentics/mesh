package com.gentics.mesh.core.data.fieldhandler.microschema;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.fieldhandler.AbstractComparatorNodeTest;
import com.gentics.mesh.core.data.schema.handler.AbstractFieldSchemaContainerComparator;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.test.context.MeshTestSetting;
import static com.gentics.mesh.test.TestSize.FULL;

@MeshTestSetting(testSize = FULL, startServer = false)
public class MicroschemaComparatorNodeTest extends AbstractComparatorNodeTest<Microschema> {

	@Override
	public AbstractFieldSchemaContainerComparator<Microschema> getComparator() {
		return new MicroschemaComparator();
	}

	@Override
	public Microschema createContainer() {
		Microschema microschema = FieldUtil.createMinimalValidMicroschema();
		return microschema;
	}

}
