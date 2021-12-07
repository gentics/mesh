package com.gentics.mesh.core.field.date;

import static com.gentics.mesh.core.field.date.DateFieldTestHelper.CREATE_EMPTY;
import static com.gentics.mesh.core.field.date.DateFieldTestHelper.FETCH;
import static com.gentics.mesh.core.field.date.DateFieldTestHelper.FILL;
import static com.gentics.mesh.util.DateUtils.fromISO8601;
import static com.gentics.mesh.util.DateUtils.toISO8601;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibDateField;
import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.DateFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.util.CoreTestUtils;
import com.gentics.mesh.util.DateUtils;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = false)
public class DateFieldTest extends AbstractFieldTest<DateFieldSchema> {

	private static final String DATE_FIELD = "dateField";

	@Override
	protected DateFieldSchema createFieldSchema(boolean isRequired) {
		DateFieldSchema dateFieldSchema = new DateFieldSchemaImpl();
		dateFieldSchema.setName(DATE_FIELD);
		dateFieldSchema.setLabel("Some date field");
		dateFieldSchema.setRequired(isRequired);
		return dateFieldSchema;
	}

	@Test
	@Override
	public void testClone() {
		try (Tx tx = tx()) {
			Long nowEpoch = System.currentTimeMillis() / 1000;
			HibNodeFieldContainer container = CoreTestUtils.createContainer();
			HibDateField dateField = container.createDate(DATE_FIELD);
			dateField.setDate(nowEpoch);

			HibNodeFieldContainer otherContainer = CoreTestUtils.createContainer();
			dateField.cloneTo(otherContainer);

			assertThat(otherContainer.getDate(DATE_FIELD)).as("cloned field").isNotNull().isEqualToIgnoringGivenFields(dateField, "parentContainer");
		}
	}

	@Test
	@Override
	public void testFieldUpdate() throws Exception {
		try (Tx tx = tx()) {
			Long nowEpoch = System.currentTimeMillis() / 1000;
			HibNodeFieldContainer container = CoreTestUtils.createContainer();
			HibDateField dateField = container.createDate(DATE_FIELD);
			assertEquals(DATE_FIELD, dateField.getFieldKey());
			dateField.setDate(nowEpoch);
			assertEquals(nowEpoch, Long.valueOf(dateField.getDate()));
			HibStringField bogusField1 = container.getString("bogus");
			assertNull(bogusField1);
			HibDateField reloadedDateField = container.getDate(DATE_FIELD);
			assertNotNull(reloadedDateField);
			assertEquals(DATE_FIELD, reloadedDateField.getFieldKey());
		}
	}

	@Test
	@Override
	public void testFieldTransformation() throws Exception {
		HibNode node = folder("2015");
		long date;
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();

			// Add html field schema to the schema
			SchemaVersionModel schema = node.getSchemaContainer().getLatestVersion().getSchema();
			DateFieldSchema dateFieldSchema = createFieldSchema(true);
			schema.addField(dateFieldSchema);
			node.getSchemaContainer().getLatestVersion().setSchema(schema);

			HibNodeFieldContainer container = contentDao.getLatestDraftFieldContainer(node, english());
			HibDateField field = container.createDate(DATE_FIELD);
			date = fromISO8601(toISO8601(System.currentTimeMillis()));
			field.setDate(date);
			tx.success();
		}

		try (Tx tx = tx()) {
			String json = getJson(node);
			assertNotNull(json);
			NodeResponse response = JsonUtil.readValue(json, NodeResponse.class);
			assertNotNull(response);

			com.gentics.mesh.core.rest.node.field.DateField deserializedNodeField = response.getFields().getDateField("dateField");
			assertNotNull(deserializedNodeField);
			assertEquals(Long.valueOf(date), fromISO8601(deserializedNodeField.getDate()));
		}

	}

	@Test
	@Override
	public void testEquals() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer();
			Long date = System.currentTimeMillis();
			HibDateField fieldA = container.createDate(DATE_FIELD);
			HibDateField fieldB = container.createDate(DATE_FIELD + "_2");
			fieldA.setDate(date);
			fieldB.setDate(date);
			assertTrue("Both fields should be equal to eachother", fieldA.equals(fieldB));
		}
	}

	@Test
	@Override
	public void testEqualsNull() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer();
			HibDateField fieldA = container.createDate(DATE_FIELD);
			HibDateField fieldB = container.createDate(DATE_FIELD + "_2");
			assertTrue("Both fields should be equal to eachother", fieldA.equals(fieldB));
		}
	}

	@Test
	@Override
	public void testEqualsRestField() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer();
			Long date = System.currentTimeMillis();

			// rest null - graph null
			HibDateField fieldA = container.createDate(DATE_FIELD);
			DateFieldImpl restField = new DateFieldImpl();
			assertTrue("Both fields should be equal to eachother since both values are null", fieldA.equals(restField));

			// rest set - graph set - different values
			fieldA.setDate(fromISO8601(toISO8601(date)));
			restField.setDate(DateUtils.toISO8601(date + 1000L));
			assertFalse("Both fields should be different since both values are not equal", fieldA.equals(restField));

			// rest set - graph set - same value
			restField.setDate(toISO8601(date));
			assertTrue("Both fields should be equal since values are equal", fieldA.equals(restField));

			// rest set - graph set - same value different type
			assertFalse("Fields should not be equal since the type does not match.",
					fieldA.equals(new StringFieldImpl().setString(String.valueOf(date))));
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreate() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestTestcase(DATE_FIELD, FETCH, CREATE_EMPTY);
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreateRequired() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestNullOnCreateRequiredTestcase(DATE_FIELD, FETCH);
		}
	}

	@Test
	@Override
	public void testRemoveFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveFieldViaNullTestcase(DATE_FIELD, FETCH, FILL, (node) -> {
				updateContainer(ac, node, DATE_FIELD, null);
			});
		}
	}

	@Test
	@Override
	public void testRemoveRequiredFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveRequiredFieldViaNullTestcase(DATE_FIELD, FETCH, FILL, (container) -> {
				updateContainer(ac, container, DATE_FIELD, null);
			});
		}
	}

	@Test
	@Override
	public void testUpdateFromRestValidSimpleValue() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeUpdateFromRestValidSimpleValueTestcase(DATE_FIELD, FILL, (container) -> {
				DateField field = new DateFieldImpl();
				field.setDate(DateUtils.toISO8601(0L, 0));
				updateContainer(ac, container, DATE_FIELD, field);
			}, (container) -> {
				HibDateField field = container.getDate(DATE_FIELD);
				assertNotNull("The graph field {" + DATE_FIELD + "} could not be found.", field);
				assertEquals("The date of the field was not updated.", 0L, field.getDate().longValue());
			});
		}
	}
}
