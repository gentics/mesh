package com.gentics.mesh.core.schema;

import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.test.TestSize.FULL;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.S3BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.S3BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.test.MeshTestSetting;

/**
 * Test cases for dummy updates on string values
 */
@RunWith(Parameterized.class)
@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class SchemaDummyStringUpdateTest extends AbstractSchemaDummyUpdateTest<String> {
	protected final static String STRINGFIELD_NAME = "stringfield";

	protected final static String BINARYFIELD_NAME = "binaryfield";

	protected final static String S3BINARYFIELD_NAME = "s3binaryfield";

	/**
	 * Test cases
	 * @return test cases
	 */
	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> parameters() {
		return Arrays.asList(new Object[][] {
			{
				"description",
				(BiConsumer<SchemaModel, String>) (schema, value) -> {
					schema.setDescription(value);
				},
				"This is the description"
			},
			{
				"displayField",
				(BiConsumer<SchemaModel, String>) (schema, value) -> {
					schema.setDisplayField(value);
				},
				STRINGFIELD_NAME
			},
			{
				"segmentField",
				(BiConsumer<SchemaModel, String>) (schema, value) -> {
					schema.setSegmentField(value);
				},
				STRINGFIELD_NAME
			},
			{
				"%s.label".formatted(STRINGFIELD_NAME),
				(BiConsumer<SchemaModel, String>) (schema, value) -> {
					schema.getField(STRINGFIELD_NAME).setLabel(value);
				},
				"This is the label"
			},
			{
				"%s.checkServiceUrl".formatted(BINARYFIELD_NAME),
				(BiConsumer<SchemaModel, String>) (schema, value) -> {
					schema.getField(BINARYFIELD_NAME, BinaryFieldSchema.class).setCheckServiceUrl(value);
				},
				"http://bla.org/"
			},
			{
				"%s.checkServiceUrl".formatted(S3BINARYFIELD_NAME),
				(BiConsumer<SchemaModel, String>) (schema, value) -> {
					schema.getField(S3BINARYFIELD_NAME, S3BinaryFieldSchema.class).setCheckServiceUrl(value);
				},
				"http://bla.org/"
			}
		});
	}

	/**
	 * Valid non-empty value for the test case
	 */
	@Parameter(2)
	public String validValue;

	@Test
	public void testUpdateNotEmptyToNull() {
		doUpdateTest(validValue, null, false);
	}

	@Test
	public void testUpdateNotEmptyToEmpty() {
		doUpdateTest(validValue, "", true);
	}

	@Test
	public void testUpdateEmptyToNull() {
		doUpdateTest("", null, false);
	}

	@Test
	public void testUpdateNullToEmpty() {
		doUpdateTest(null, "", false);
	}

	@Test
	public void testDiffNotEmptyToNull() {
		doDiffTest(validValue, null, false);
	}

	@Test
	public void testDiffNotEmptyToEmpty() {
		doDiffTest(validValue, "", true);
	}

	@Test
	public void testDiffEmptyToNull() {
		doDiffTest("", null, false);
	}

	@Test
	public void testDiffNullToEmpty() {
		doDiffTest(null, "", false);
	}

	@Override
	protected List<FieldSchema> fields() {
		return Arrays.asList(
			new StringFieldSchemaImpl().setName(STRINGFIELD_NAME),
			new BinaryFieldSchemaImpl().setName(BINARYFIELD_NAME),
			new S3BinaryFieldSchemaImpl().setName(S3BINARYFIELD_NAME)
		);
	}
}
