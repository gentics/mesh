package com.gentics.mesh.core.schema;

import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.test.TestSize.FULL;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiConsumer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.test.MeshTestSetting;

/**
 * Test cases for dummy updates on boolean values
 */
@RunWith(Parameterized.class)
@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class SchemaDummyBooleanUpdateTest extends AbstractSchemaDummyUpdateTest<Boolean> {
	/**
	 * Test cases
	 * @return test cases
	 */
	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> parameters() {
		return Arrays.asList(new Object[][] {
			{
				"autoPurge",
				(BiConsumer<SchemaModel, Boolean>) (schema, value) -> {
					schema.setAutoPurge(value);
				},
				true
			},
			{
				"container",
				(BiConsumer<SchemaModel, Boolean>) (schema, value) -> {
					schema.setContainer(value);
				},
				false
			},
			{
				"noIndex",
				(BiConsumer<SchemaModel, Boolean>) (schema, value) -> {
					schema.setNoIndex(value);
				},
				false
			}
		});
	}

	/**
	 * Flag to mark "tri-state" booleans, that distinguish between false and null
	 */
	@Parameter(2)
	public boolean triState;

	@Test
	public void testUpdateTrueToNull() {
		doUpdateTest(true, null, triState);
	}

	@Test
	public void testUpdateNullToTrue() {
		doUpdateTest(null, true, true);
	}

	@Test
	public void testUpdateFalseToNull() {
		doUpdateTest(false, null, triState);
	}

	@Test
	public void testUpdateNullToFalse() {
		doUpdateTest(null, false, triState);
	}

	@Test
	public void testDiffTrueToNull() {
		doDiffTest(true, null, triState);
	}

	@Test
	public void testDiffNullToTrue() {
		doDiffTest(null, true, true);
	}

	@Test
	public void testDiffFalseToNull() {
		doDiffTest(false, null, triState);
	}

	@Test
	public void testDiffNullToFalse() {
		doDiffTest(null, false, triState);
	}
}
