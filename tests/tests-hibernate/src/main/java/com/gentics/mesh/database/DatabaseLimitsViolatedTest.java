package com.gentics.mesh.database;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.test.MeshOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.category.FailingTests;

@MeshTestSetting(testSize = TestSize.PROJECT, customOptionChanger = DatabaseLimitsViolatedTest.class)
@Category(FailingTests.class)
public class DatabaseLimitsViolatedTest extends DatabaseLimitsTest implements MeshOptionChanger {

	@Override
	@Test(expected = RuntimeException.class)
	public void testParamsOverflowLimit() {
		super.testParamsOverflowLimit();
	}

	@Override
	@Test(expected = RuntimeException.class)
	public void testParamsFitLimit() {
		super.testParamsFitLimit();
	}

	@Override
	public void change(MeshOptions options) {
		HibernateMeshOptions eOptions = (HibernateMeshOptions) options;
		eOptions.getStorageOptions().setSqlParametersLimit(Short.toString(Short.MAX_VALUE));
	}
}
