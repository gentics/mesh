package com.gentics.mesh.database;

import org.junit.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.test.MeshOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

@MeshTestSetting(testSize = TestSize.PROJECT, customOptionChanger = DatabaseLimitsViolatedTest.class)
// Jenkins behaves oddly with this condition set on a whole class
//@EnabledIfSystemProperty(named = DatabaseLimitsViolatedTest.ENV_SQL_LIMITS_MAY_VIOLATE, matches = "true", disabledReason = "The connected database is not prone to the SQL parameters limits violation.")
public class DatabaseLimitsViolatedTest extends DatabaseLimitsTest implements MeshOptionChanger {

	public static final String ENV_SQL_LIMITS_MAY_VIOLATE = "sqlLimitsMayViolate";

	@Override
	@Test(expected = RuntimeException.class)
	@EnabledIfSystemProperty(named = DatabaseLimitsViolatedTest.ENV_SQL_LIMITS_MAY_VIOLATE, matches = "true", disabledReason = "The connected database is not prone to the SQL parameters limits violation.")
	public void testParamsOverflowLimit() {
		super.testParamsOverflowLimit();
	}

	@Override
	@Test(expected = RuntimeException.class)
	@EnabledIfSystemProperty(named = DatabaseLimitsViolatedTest.ENV_SQL_LIMITS_MAY_VIOLATE, matches = "true", disabledReason = "The connected database is not prone to the SQL parameters limits violation.")
	public void testParamsFitLimit() {
		super.testParamsFitLimit();
	}

	@Test
	public void testJustToPleaseCi() {
		System.out.println("DEar CI, please pass this test");
	}

	@Override
	public void change(MeshOptions options) {
		HibernateMeshOptions eOptions = (HibernateMeshOptions) options;
		eOptions.getStorageOptions().setSqlParametersLimit(Short.toString(Short.MAX_VALUE));
	}
}
