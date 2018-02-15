package com.gentics.mesh.core.utilities;

import static com.gentics.mesh.test.TestSize.FULL;

import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class ValidateMicroSchemaTest extends AbstractValidateSchemaTest {

	public ValidateMicroSchemaTest() {
		super("/utilities/validateMicroschema", true);
	}

}
