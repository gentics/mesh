package com.gentics.mesh.core.schema;

import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.test.TestSize.FULL;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.S3BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.S3BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.test.MeshTestSetting;

/**
 * Test cases for dummy updates on string list (or string array) values
 */
@RunWith(Parameterized.class)
@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class SchemaDummyStringListUpdateTest extends AbstractSchemaDummyUpdateTest<List<String>> {
	protected final static String STRINGFIELD_NAME = "stringfield";

	protected final static String BINARYFIELD_NAME = "binaryfield";

	protected final static String LISTFIELD_NAME = "listfield";

	protected final static String MICRONODEFIELD_NAME = "micronodefield";

	protected final static String NODEFIELD_NAME = "nodefield";

	protected final static String S3BINARYFIELD_NAME = "s3binaryfield";

	/**
	 * Convert the string list into a string array. If the value is null, the result will also be null
	 * @param value original value
	 * @return string array or null
	 */
	protected static String[] convert(List<String> value) {
		if (value == null) {
			return null;
		} else if (value.isEmpty()) {
			return new String[0];
		} else {
			return value.toArray(new String[value.size()]);
		}
	}

	/**
	 * Test cases
	 * @return test cases
	 */
	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> parameters() {
		return Arrays.asList(new Object[][] {
			{
				"urlFields",
				(BiConsumer<SchemaModel, List<String>>) (schema, value) -> {
					schema.setUrlFields(value);
				},
				STRINGFIELD_NAME
			},
			{
				"%s.allowedValues".formatted(STRINGFIELD_NAME),
				(BiConsumer<SchemaModel, List<String>>) (schema, value) -> {
					schema.getField(STRINGFIELD_NAME, StringFieldSchema.class).setAllowedValues(convert(value));
				},
				"bla"
			},
			{
				"%s.allowedMimeTypes".formatted(BINARYFIELD_NAME),
				(BiConsumer<SchemaModel, List<String>>) (schema, value) -> {
					schema.getField(BINARYFIELD_NAME, BinaryFieldSchema.class).setAllowedMimeTypes(convert(value));
				},
				"text/html"
			},
			{
				"%s.allowedSchemas".formatted(LISTFIELD_NAME),
				(BiConsumer<SchemaModel, List<String>>) (schema, value) -> {
					schema.getField(LISTFIELD_NAME, ListFieldSchema.class).setAllowedSchemas(convert(value));
				},
				"folder"
			},
			{
				"%s.allowedSchemas".formatted(NODEFIELD_NAME),
				(BiConsumer<SchemaModel, List<String>>) (schema, value) -> {
					schema.getField(NODEFIELD_NAME, NodeFieldSchema.class).setAllowedSchemas(convert(value));
				},
				"folder"
			},
			{
				"%s.allowedMimeTypes".formatted(S3BINARYFIELD_NAME),
				(BiConsumer<SchemaModel, List<String>>) (schema, value) -> {
					schema.getField(S3BINARYFIELD_NAME, S3BinaryFieldSchema.class).setAllowedMimeTypes(convert(value));
				},
				"text/html"
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
		doUpdateTest(Arrays.asList(validValue), null, false);
	}

	@Test
	public void testUpdateNotEmptyToEmpty() {
		doUpdateTest(Arrays.asList(validValue), Collections.emptyList(), true);
	}

	@Test
	public void testUpdateNullToEmpty() {
		doUpdateTest(null, Collections.emptyList(), false);
	}

	@Test
	public void testUpdateEmptyToNull() {
		doUpdateTest(Collections.emptyList(), null, false);
	}

	@Test
	public void testDiffNotEmptyToNull() {
		doDiffTest(Arrays.asList(validValue), null, false);
	}

	@Test
	public void testDiffNotEmptyToEmpty() {
		doDiffTest(Arrays.asList(validValue), Collections.emptyList(), true);
	}

	@Test
	public void testDiffNullToEmpty() {
		doDiffTest(null, Collections.emptyList(), false);
	}

	@Test
	public void testDiffEmptyToNull() {
		doDiffTest(Collections.emptyList(), null, false);
	}

	@Override
	protected List<FieldSchema> fields() {
		return Arrays.asList(
			new StringFieldSchemaImpl().setName(STRINGFIELD_NAME),
			new BinaryFieldSchemaImpl().setName(BINARYFIELD_NAME),
			new ListFieldSchemaImpl().setListType("node").setName(LISTFIELD_NAME),
			new NodeFieldSchemaImpl().setName(NODEFIELD_NAME),
			new S3BinaryFieldSchemaImpl().setName(S3BINARYFIELD_NAME)
		);
	}
}
