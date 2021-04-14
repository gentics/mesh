package com.gentics.mesh.core.utilities;

import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Ignore;

import com.gentics.mesh.test.MeshTestSetting;

@Ignore
@MeshTestSetting(testSize = FULL, startServer = true)
public class ValidateMicroSchemaTest extends AbstractValidateSchemaTest {

	public ValidateMicroSchemaTest() {
		super("/utilities/validateMicroschema", false);
	}

}
