package com.gentics.mesh.core.field.micronode;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.field.micronode.MicronodeFieldHelper.CREATE_EMPTY;
import static com.gentics.mesh.core.field.micronode.MicronodeFieldHelper.FETCH;
import static com.gentics.mesh.core.field.micronode.MicronodeFieldHelper.FILL;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.util.DateUtils.fromISO8601;
import static com.gentics.mesh.util.DateUtils.toISO8601;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.MicroschemaDao;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.data.node.field.list.HibBooleanFieldList;
import com.gentics.mesh.core.data.node.field.list.HibDateFieldList;
import com.gentics.mesh.core.data.node.field.list.HibHtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.HibNodeFieldList;
import com.gentics.mesh.core.data.node.field.list.HibNumberFieldList;
import com.gentics.mesh.core.data.node.field.list.HibStringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.json.MeshJsonException;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.NoConsistencyCheck;
import com.gentics.mesh.util.CoreTestUtils;

import io.vertx.core.json.JsonObject;

/**
 * Test cases for fields of type "micronode"
 */
// TODO: add tests for all types of fields that can be put into a micronode
@MeshTestSetting(testSize = FULL, startServer = false)
public class MicronodeFieldTest extends AbstractFieldTest<MicronodeFieldSchema> {

	private static final String MICRONODE_FIELD = "micronodeField";

	@Override
	protected MicronodeFieldSchema createFieldSchema(boolean isRequired) {
		return createFieldSchema(MICRONODE_FIELD, isRequired);
	}
	protected MicronodeFieldSchema createFieldSchema(String fieldKey, boolean isRequired) {
		MicronodeFieldSchema schema = new MicronodeFieldSchemaImpl();
		schema.setLabel("Some microschema label");
		schema.setName(fieldKey);
		schema.setRequired(isRequired);
		schema.setAllowedMicroSchemas("vcard");
		return schema;
	}

	/**
	 * Create a dummy microschema
	 * 
	 * @return
	 * @throws MeshJsonException
	 */
	protected HibMicroschema createDummyMicroschema() throws MeshJsonException {
		MicroschemaVersionModel dummyMicroschema = new MicroschemaModelImpl();
		dummyMicroschema.setName("dummymicroschema");

		StringFieldSchema stringFieldSchema = new StringFieldSchemaImpl();
		stringFieldSchema.setName("stringfield");
		stringFieldSchema.setLabel("String Field");
		dummyMicroschema.addField(stringFieldSchema);

		return createMicroschema(dummyMicroschema);
	}

	/**
	 * Dummy microschema
	 */
	protected HibMicroschema dummyMicroschema;

	@Before
	public void addDummySchema() throws Exception {
		try (Tx tx = tx()) {
			dummyMicroschema = createDummyMicroschema();
			tx.success();
		}
	}

	@Test
	@Override
	public void testFieldTransformation() throws Exception {
		Calendar date = Calendar.getInstance();
		date.set(2016, 1, 6, 3, 44);
		try (Tx tx = tx()) {
			HibNode node = folder("2015");
			HibNode newOverview = content("news overview");

			ContentDao contentDao = tx.contentDao();
			MicroschemaDao microschemaDao = tx.microschemaDao();

			MicroschemaVersionModel fullMicroschema = new MicroschemaModelImpl();
			fullMicroschema.setName("full");

			// fullMicroschema.addField(new BinaryFieldSchemaImpl().setName("binaryfield").setLabel("Binary Field"));
			fullMicroschema.addField(new BooleanFieldSchemaImpl().setName("booleanfield").setLabel("Boolean Field"));
			fullMicroschema.addField(new DateFieldSchemaImpl().setName("datefield").setLabel("Date Field"));
			fullMicroschema.addField(new HtmlFieldSchemaImpl().setName("htmlfield").setLabel("HTML Field"));
			// fullMicroschema.addField(new ListFieldSchemaImpl().setListType("binary").setName("listfield-binary").setLabel("Binary List Field"));
			fullMicroschema.addField(new ListFieldSchemaImpl().setListType("boolean").setName("listfield-boolean").setLabel("Boolean List Field"));
			fullMicroschema.addField(new ListFieldSchemaImpl().setListType("date").setName("listfield-date").setLabel("Date List Field"));
			fullMicroschema.addField(new ListFieldSchemaImpl().setListType("html").setName("listfield-html").setLabel("Html List Field"));
			fullMicroschema.addField(new ListFieldSchemaImpl().setListType("node").setName("listfield-node").setLabel("Node List Field"));
			fullMicroschema.addField(new ListFieldSchemaImpl().setListType("number").setName("listfield-number").setLabel("Number List Field"));
			fullMicroschema.addField(new ListFieldSchemaImpl().setListType("string").setName("listfield-string").setLabel("String List Field"));
			fullMicroschema.addField(new NodeFieldSchemaImpl().setName("nodefield").setLabel("Node Field"));
			fullMicroschema.addField(new NumberFieldSchemaImpl().setName("numberfield").setLabel("Number Field"));
			fullMicroschema.addField(new StringFieldSchemaImpl().setName("stringfield").setLabel("String Field"));

			HibMicroschema microschema = microschemaDao.create(fullMicroschema, getRequestUser(), createBatch());
			prepareTypedSchema(node, new MicronodeFieldSchemaImpl().setName("micronodefield").setLabel("Micronode Field"), false);
			tx.commit();

			HibNodeFieldContainer container = contentDao.createFieldContainer(node, english(),
					node.getProject().getLatestBranch(), user(),
					contentDao.getLatestDraftFieldContainer(node, english()), true);
			HibMicronodeField micronodeField = container.createMicronode("micronodefield", microschema.getLatestVersion());
			HibMicronode micronode = micronodeField.getMicronode();
			assertNotNull("Micronode must not be null", micronode);
			// micronode.createBinary("binaryfield");
			micronode.createBoolean("booleanfield").setBoolean(true);
			micronode.createDate("datefield").setDate(date.getTimeInMillis());
			micronode.createHTML("htmlfield").setHtml("<b>HTML</b> value");

			HibBooleanFieldList booleanList = micronode.createBooleanList("listfield-boolean");
			booleanList.createBoolean(true);
			booleanList.createBoolean(false);

			HibDateFieldList dateList = micronode.createDateList("listfield-date");
			dateList.createDate(date.getTimeInMillis());
			dateList.createDate(0L);

			HibHtmlFieldList htmlList = micronode.createHTMLList("listfield-html");
			htmlList.createHTML("<b>first</b>");
			htmlList.createHTML("<i>second</i>");

			HibNodeFieldList nodeList = micronode.createNodeList("listfield-node");
			nodeList.createNode(0, node);
			nodeList.createNode(1, newOverview);

			HibNumberFieldList numberList = micronode.createNumberList("listfield-number");
			numberList.createNumber(47);
			numberList.createNumber(11);

			// TODO create list of select fields

			HibStringFieldList stringList = micronode.createStringList("listfield-string");
			stringList.createString("first");
			stringList.createString("second");
			stringList.createString("third");

			micronode.createNode("nodefield", newOverview);
			micronode.createNumber("numberfield").setNumber(4711);
			// micronode.createSelect("selectfield");
			micronode.createString("stringfield").setString("String Value");
			tx.success();
		}

		try (Tx tx = tx()) {
			HibNode node = folder("2015");
			HibNode newOverview = content("news overview");

			String json = getJson(node);
			System.out.println(json);
			JsonObject jsonObject = new JsonObject(json);
			JsonObject fields = jsonObject.getJsonObject("fields");
			assertNotNull("JSON Object must contain fields", fields);
			JsonObject micronodeFieldObject = fields.getJsonObject("micronodefield");
			assertNotNull("JSON Object must contain micronode field", micronodeFieldObject);
			JsonObject micronodeFields = micronodeFieldObject.getJsonObject("fields");
			assertNotNull("Micronode must contain fields", micronodeFields);
			// TODO check binary field
			assertEquals("Boolean Field", Boolean.TRUE, micronodeFields.getBoolean("booleanfield"));
			assertEquals("Date Field", toISO8601(date.getTimeInMillis()), micronodeFields.getString("datefield"));
			assertEquals("HTML Field", "<b>HTML</b> value", micronodeFields.getString("htmlfield"));
			// TODO check binary list field
			assertThat(micronodeFields.getJsonArray("listfield-boolean")).as("Boolean List Field").matches(true, false);
			assertThat(micronodeFields.getJsonArray("listfield-date")).as("Date List Field").matches(toISO8601(date.getTimeInMillis()), toISO8601(0));
			assertThat(micronodeFields.getJsonArray("listfield-html")).as("HTML List Field").matches("<b>first</b>", "<i>second</i>");
			assertThat(micronodeFields.getJsonArray("listfield-node")).as("Node List Field").key("uuid").matches(node.getUuid(), newOverview.getUuid());
			assertThat(micronodeFields.getJsonArray("listfield-number").stream().map(Number.class::cast).map(Number::longValue).collect(Collectors.toList())).as("Number List Field").containsExactly(47L, 11L);
			assertThat(micronodeFields.getJsonArray("listfield-string")).as("String List Field").matches("first", "second", "third");
			assertThat(micronodeFields.getJsonObject("nodefield")).as("Node Field").key("uuid").matches(newOverview.getUuid());
			assertEquals("Number Field", 4711, micronodeFields.getInteger("numberfield").intValue());
			assertEquals("String Field", "String Value", micronodeFields.getString("stringfield"));
		}
	}

	/**
	 * Test creation of field
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateMicronodeField() throws Exception {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));

			HibMicronodeField field = container.createMicronode(MICRONODE_FIELD, dummyMicroschema.getLatestVersion());
			assertNotNull(field);
			HibMicronode micronode = field.getMicronode();
			assertNotNull(micronode);
			assertTrue("Micronode must have a uuid", StringUtils.isNotEmpty(micronode.getUuid()));

			HibStringField micronodeStringField = micronode.createString("stringfield");
			assertNotNull(micronodeStringField);
			micronodeStringField.setString("dummyString");

			HibMicronodeField reloadedField = container.getMicronode(MICRONODE_FIELD);
			assertNotNull(reloadedField);
			HibMicronode reloadedMicronode = reloadedField.getMicronode();
			assertNotNull(reloadedMicronode);
			assertEquals(micronode.getUuid(), reloadedMicronode.getUuid());

			HibStringField reloadedMicronodeStringField = reloadedMicronode.getString("stringfield");
			assertNotNull(reloadedMicronodeStringField);

			assertEquals(micronodeStringField.getString(), reloadedMicronodeStringField.getString());
		}
	}

	@Test
	public void testMicronodeUpdateFromRest() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));

			HibMicronodeField field = container.createMicronode(MICRONODE_FIELD, dummyMicroschema.getLatestVersion());
			HibMicronode micronode = field.getMicronode();

			MicroschemaVersionModel schema = micronode.getSchemaContainerVersion().getSchema();
			schema.addField(FieldUtil.createStringFieldSchema("stringfield"));
			micronode.getSchemaContainerVersion().setSchema(schema);
			InternalActionContext ac = mockActionContext();
			mesh().serverSchemaStorage().clear();

			FieldMap restFields = new FieldMapImpl();
			restFields.put("stringfield", new StringFieldImpl().setString("test"));
			field.getMicronode().updateFieldsFromRest(ac, restFields);

			assertNotNull("The field should have been created.", field.getMicronode().getString("stringfield"));
			assertEquals("The field did not contain the expected value", "test", field.getMicronode().getString("stringfield").getString());
		}
	}

	@Test
	@NoConsistencyCheck
	@Override
	public void testClone() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
			HibMicronodeField field = container.createMicronode(MICRONODE_FIELD, dummyMicroschema.getLatestVersion());

			HibNodeFieldContainer otherContainer = CoreTestUtils.createContainer(createFieldSchema(true));
			field.cloneTo(otherContainer);

			assertThat(otherContainer.getMicronode(MICRONODE_FIELD)).as("cloned field").isNotNull();
			assertThat(otherContainer.getMicronode(MICRONODE_FIELD).getMicronode()).as("cloned micronode").isNotNull()
					.isEqualToComparingFieldByField(field.getMicronode());
		}
	}

	@Test
	@Override
	public void testFieldUpdate() throws Exception {
		try (Tx tx = tx()) {
			CommonTx ctx = tx.unwrap();
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));

			HibMicronodeField field = container.createMicronode(MICRONODE_FIELD, dummyMicroschema.getLatestVersion());
			HibMicronode micronode = field.getMicronode();
			String originalUuid = micronode.getUuid();

			Stream<? extends HibMicronode> existingMicronodes = ctx.contentDao().findAllMicronodes();
			existingMicronodes.forEach(foundMicronode -> assertEquals(micronode.getUuid(), foundMicronode.getUuid()));

			// update by recreation
			HibMicronodeField updatedField = container.createMicronode(MICRONODE_FIELD, dummyMicroschema.getLatestVersion());
			HibMicronode updatedMicronode = updatedField.getMicronode();

			assertFalse("Uuid of micronode must be different after update", StringUtils.equalsIgnoreCase(originalUuid, updatedMicronode.getUuid()));

			existingMicronodes = ctx.contentDao().findAllMicronodes();
			existingMicronodes.forEach(foundMicronode -> assertEquals(updatedMicronode.getUuid(), foundMicronode.getUuid()));
		}
	}

	@Test
	@Override
	public void testEquals() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema("fieldA", true), createFieldSchema("fieldB", true));
			HibMicronodeField fieldA = container.createMicronode("fieldA", microschemaContainer("vcard").getLatestVersion());
			HibMicronodeField fieldB = container.createMicronode("fieldB", microschemaContainer("vcard").getLatestVersion());
			assertTrue("The field should  be equal to itself", fieldA.equals(fieldA));
			fieldA.getMicronode().createString("firstName").setString("someStringValue");
			assertTrue("The field should  be equal to itself", fieldA.equals(fieldA));

			assertFalse("The field should not be equal to a non-string field", fieldA.equals("bogus"));
			assertFalse("The field should not be equal since fieldB has no value", fieldA.equals(fieldB));
			fieldB.getMicronode().createString("firstName").setString("someStringValue");
			assertTrue("Both fields have the same value and should be equal", fieldA.equals(fieldB));
		}
	}

	@Test
	@Override
	public void testEqualsNull() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
			HibMicronodeField fieldA = container.createMicronode(MICRONODE_FIELD, microschemaContainer("vcard").getLatestVersion());
			assertFalse(fieldA.equals((Field) null));
			assertFalse(fieldA.equals((HibMicronodeField) null));
		}
	}

	@Test
	@NoConsistencyCheck
	@Override
	public void testEqualsRestField() {
		try (Tx tx = tx()) {
			CommonTx ctx = tx.unwrap();
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));

			MicroschemaVersionModel microschema = new MicroschemaModelImpl();
			microschema.setVersion("1.0");
			microschema.addField(FieldUtil.createStringFieldSchema("string"));
			microschema.addField(FieldUtil.createDateFieldSchema("date"));

			// Create microschema for the micronode
			HibMicroschemaVersion containerVersion = ctx.microschemaDao().createPersistedVersion(dummyMicroschema, v -> {
				// rest null - graph null
				v.setSchema(microschema);
			});
			tx.commit();
			HibMicronodeField fieldA = container.createMicronode(MICRONODE_FIELD, containerVersion);
			MicronodeResponse restField = new MicronodeResponse();
			assertTrue("Both fields should be equal to eachother since both values are null", fieldA.equals(restField));

			// rest set - graph set - different values
			Long date = fromISO8601(toISO8601(System.currentTimeMillis()));
			fieldA.getMicronode().createString("string").setString("someString");
			fieldA.getMicronode().createDate("date").setDate(date);
			restField.getFields().put("string", FieldUtil.createStringField("someOtherString"));
			restField.getFields().put("date", FieldUtil.createDateField(toISO8601(date)));
			assertFalse("Both fields should be different since both values are not equal", fieldA.equals(restField));

			// rest set - graph set - same value
			restField.getFields().getStringField("string").setString("someString");
			assertTrue("Both fields should be equal since values are equal", fieldA.equals(restField));

			// rest set - graph set - same value different type
			restField.getFields().put("string", FieldUtil.createHtmlField("someString"));
			assertFalse("Fields should not be equal since the type does not match.", fieldA.equals(restField));
			assertFalse("Fields should not be equal since the type does not match.", fieldA.equals(new StringFieldImpl().setString("blub")));
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreate() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestTestcase(MICRONODE_FIELD, FETCH, CREATE_EMPTY);
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreateRequired() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestNullOnCreateRequiredTestcase(MICRONODE_FIELD, FETCH);
		}
	}

	@Test
	@Override
	public void testRemoveFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveFieldViaNullTestcase(MICRONODE_FIELD, FETCH, FILL, (node) -> {
				updateContainer(ac, node, MICRONODE_FIELD, null);
			});
		}
	}

	@Test
	@Override
	public void testRemoveRequiredFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveRequiredFieldViaNullTestcase(MICRONODE_FIELD, FETCH, FILL, (container) -> {
				updateContainer(ac, container, MICRONODE_FIELD, null);
			});
		}
	}

	@Test
	@Override
	public void testUpdateFromRestValidSimpleValue() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeUpdateFromRestValidSimpleValueTestcase(MICRONODE_FIELD, FILL, (container) -> {
				MicronodeResponse field = new MicronodeResponse();
				field.setMicroschema(new MicroschemaReferenceImpl().setName("vcard"));
				field.getFields().put("firstName", FieldUtil.createStringField("vcard_firstname_value"));
				field.getFields().put("lastName", FieldUtil.createStringField("vcard_lastname_value"));
				updateContainer(ac, container, MICRONODE_FIELD, field);
			}, (container) -> {
				HibMicronodeField field = container.getMicronode(MICRONODE_FIELD);
				assertNotNull("The graph field {" + MICRONODE_FIELD + "} could not be found.", field);
				assertEquals("The micronode of the field was not updated.", "vcard_firstname_value",
						field.getMicronode().getString("firstName").getString());
				assertEquals("The micronode of the field was not updated.", "vcard_lastname_value",
						field.getMicronode().getString("lastName").getString());
			});
		}
	}

}
