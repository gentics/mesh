package com.gentics.mesh.core.utilities;

import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Ignore;

import com.gentics.mesh.test.context.MeshTestSetting;

@Ignore
@MeshTestSetting(testSize = FULL, startServer = true)
public class ValidateSchemaTest extends AbstractValidateSchemaTest {

	public ValidateSchemaTest() {
		super("/utilities/validateSchema", true);
	}

}
