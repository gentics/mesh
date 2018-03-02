package com.gentics.mesh.core.utilities;

import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Ignore;

import com.gentics.mesh.test.context.MeshTestSetting;

@Ignore
@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class ValidateMicroSchemaTest extends AbstractValidateSchemaTest {

	public ValidateMicroSchemaTest() {
		super("/utilities/validateMicroschema", false);
	}

}
