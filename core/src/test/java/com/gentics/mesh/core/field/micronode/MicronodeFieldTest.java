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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.microschema.MicroschemaModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
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
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.test.context.MeshTestSetting;

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
		MicronodeFieldSchema schema = new MicronodeFieldSchemaImpl();
		schema.setLabel("Some microschema label");
		schema.setName(MICRONODE_FIELD);
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
	protected MicroschemaContainer createDummyMicroschema() throws MeshJsonException {
		MicroschemaModel dummyMicroschema = new MicroschemaModelImpl();
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
	protected MicroschemaContainer dummyMicroschema;

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
		Node node = folder("2015");
		Long date = fromISO8601(toISO8601(System.currentTimeMillis()));
		Node newOverview = content("news overview");

		try (Tx tx = tx()) {

			MicroschemaModel fullMicroschema = new MicroschemaModelImpl();
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

			MicroschemaContainer microschemaContainer = boot().microschemaContainerRoot().create(fullMicroschema, getRequestUser(), createBatch());

			SchemaModel schema = node.getSchemaContainer().getLatestVersion().getSchema();
			schema.addField(new MicronodeFieldSchemaImpl().setName("micronodefield").setLabel("Micronode Field"));
			node.getSchemaContainer().getLatestVersion().setSchema(schema);

			NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english());
			MicronodeGraphField micronodeField = container.createMicronode("micronodefield", microschemaContainer.getLatestVersion());
			Micronode micronode = micronodeField.getMicronode();
			assertNotNull("Micronode must not be null", micronode);
			// micronode.createBinary("binaryfield");
			micronode.createBoolean("booleanfield").setBoolean(true);
			micronode.createDate("datefield").setDate(date);
			micronode.createHTML("htmlfield").setHtml("<b>HTML</b> value");

			BooleanGraphFieldList booleanList = micronode.createBooleanList("listfield-boolean");
			booleanList.createBoolean(true);
			booleanList.createBoolean(false);

			DateGraphFieldList dateList = micronode.createDateList("listfield-date");
			dateList.createDate(date);
			dateList.createDate(0L);

			HtmlGraphFieldList htmlList = micronode.createHTMLList("listfield-html");
			htmlList.createHTML("<b>first</b>");
			htmlList.createHTML("<i>second</i>");

			NodeGraphFieldList nodeList = micronode.createNodeList("listfield-node");
			nodeList.createNode("0", node);
			nodeList.createNode("1", newOverview);

			NumberGraphFieldList numberList = micronode.createNumberList("listfield-number");
			numberList.createNumber(47);
			numberList.createNumber(11);

			// TODO create list of select fields

			StringGraphFieldList stringList = micronode.createStringList("listfield-string");
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
			assertEquals("Date Field", toISO8601(date), micronodeFields.getString("datefield"));
			assertEquals("HTML Field", "<b>HTML</b> value", micronodeFields.getString("htmlfield"));
			// TODO check binary list field
			assertThat(micronodeFields.getJsonArray("listfield-boolean")).as("Boolean List Field").matches(true, false);
			assertThat(micronodeFields.getJsonArray("listfield-date")).as("Date List Field").matches(toISO8601(date), toISO8601(0));
			assertThat(micronodeFields.getJsonArray("listfield-html")).as("HTML List Field").matches("<b>first</b>", "<i>second</i>");
			assertThat(micronodeFields.getJsonArray("listfield-node")).as("Node List Field").key("uuid").matches(node.getUuid(),
					newOverview.getUuid());
			assertThat(micronodeFields.getJsonArray("listfield-number")).as("Number List Field").matches(47, 11);
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
			NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);

			MicronodeGraphField field = container.createMicronode("testMicronodeField", dummyMicroschema.getLatestVersion());
			assertNotNull(field);
			Micronode micronode = field.getMicronode();
			assertNotNull(micronode);
			assertTrue("Micronode must have a uuid", StringUtils.isNotEmpty(micronode.getUuid()));

			StringGraphField micronodeStringField = micronode.createString("stringfield");
			assertNotNull(micronodeStringField);
			micronodeStringField.setString("dummyString");

			MicronodeGraphField reloadedField = container.getMicronode("testMicronodeField");
			assertNotNull(reloadedField);
			Micronode reloadedMicronode = reloadedField.getMicronode();
			assertNotNull(reloadedMicronode);
			assertEquals(micronode.getUuid(), reloadedMicronode.getUuid());

			StringGraphField reloadedMicronodeStringField = reloadedMicronode.getString("stringfield");
			assertNotNull(reloadedMicronodeStringField);

			assertEquals(micronodeStringField.getString(), reloadedMicronodeStringField.getString());
		}
	}

	@Test
	public void testMicronodeUpdateFromRest() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);

			MicronodeGraphField field = container.createMicronode("testMicronodeField", dummyMicroschema.getLatestVersion());
			Micronode micronode = field.getMicronode();

			MicroschemaModel schema = micronode.getSchemaContainerVersion().getSchema();
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
	@Override
	public void testClone() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			MicronodeGraphField field = container.createMicronode("testMicronodeField", dummyMicroschema.getLatestVersion());

			NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			field.cloneTo(otherContainer);

			assertThat(otherContainer.getMicronode("testMicronodeField")).as("cloned field").isNotNull();
			assertThat(otherContainer.getMicronode("testMicronodeField").getMicronode()).as("cloned micronode").isNotNull()
					.isEqualToComparingFieldByField(field.getMicronode());
		}
	}

	@Test
	@Override
	public void testFieldUpdate() throws Exception {
		try (Tx tx = tx()) {
			NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);

			MicronodeGraphField field = container.createMicronode("testMicronodeField", dummyMicroschema.getLatestVersion());
			Micronode micronode = field.getMicronode();
			String originalUuid = micronode.getUuid();

			List<? extends MicronodeImpl> existingMicronodes = new TraversalResult(tx.getGraph().v().has(MicronodeImpl.class).frameExplicit(MicronodeImpl.class)).list();
			for (Micronode foundMicronode : existingMicronodes) {
				assertEquals(micronode.getUuid(), foundMicronode.getUuid());
			}

			// update by recreation
			MicronodeGraphField updatedField = container.createMicronode("testMicronodeField", dummyMicroschema.getLatestVersion());
			Micronode updatedMicronode = updatedField.getMicronode();

			assertFalse("Uuid of micronode must be different after update", StringUtils.equalsIgnoreCase(originalUuid, updatedMicronode.getUuid()));

			existingMicronodes = new TraversalResult(tx.getGraph().v().has(MicronodeImpl.class).frameExplicit(MicronodeImpl.class)).list();
			for (MicronodeImpl foundMicronode : existingMicronodes) {
				assertEquals(updatedMicronode.getUuid(), foundMicronode.getUuid());
			}
		}
	}

	@Test
	@Override
	public void testEquals() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			MicronodeGraphField fieldA = container.createMicronode("fieldA", microschemaContainer("vcard").getLatestVersion());
			MicronodeGraphField fieldB = container.createMicronode("fieldB", microschemaContainer("vcard").getLatestVersion());
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
			NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			MicronodeGraphField fieldA = container.createMicronode("fieldA", microschemaContainer("vcard").getLatestVersion());
			assertFalse(fieldA.equals((Field) null));
			assertFalse(fieldA.equals((GraphField) null));
		}
	}

	@Test
	@Override
	public void testEqualsRestField() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			// Create microschema for the micronode
			MicroschemaContainerVersion containerVersion = tx.getGraph().addFramedVertex(MicroschemaContainerVersionImpl.class);
			MicroschemaModel microschema = new MicroschemaModelImpl();
			microschema.setVersion("1.0");
			microschema.addField(FieldUtil.createStringFieldSchema("string"));
			microschema.addField(FieldUtil.createDateFieldSchema("date"));

			// rest null - graph null
			containerVersion.setSchema(microschema);
			MicronodeGraphField fieldA = container.createMicronode(MICRONODE_FIELD, containerVersion);
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
				MicronodeGraphField field = container.getMicronode(MICRONODE_FIELD);
				assertNotNull("The graph field {" + MICRONODE_FIELD + "} could not be found.", field);
				assertEquals("The micronode of the field was not updated.", "vcard_firstname_value",
						field.getMicronode().getString("firstName").getString());
				assertEquals("The micronode of the field was not updated.", "vcard_lastname_value",
						field.getMicronode().getString("lastName").getString());
			});
		}
	}

}
