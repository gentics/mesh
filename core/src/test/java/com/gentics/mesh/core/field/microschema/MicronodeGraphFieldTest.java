package com.gentics.mesh.core.field.microschema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
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
import com.gentics.mesh.core.field.bool.AbstractBasicDBTest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.MeshJsonException;

import io.vertx.core.json.JsonObject;

/**
 * Test cases for fields of type "micronode" TODO: add tests for all types of fields that can be put into a micronode
 */
public class MicronodeGraphFieldTest extends AbstractBasicDBTest {
	/**
	 * Dummy microschema
	 */
	protected MicroschemaContainer dummyMicroschema;

	@Override
	public void setup() throws Exception {
		super.setup();
		dummyMicroschema = createDummyMicroschema();
	}

	//	@Autowired
	//	private ServerSchemaStorage schemaStorage;

	@Test
	public void testMicronodeFieldTransformation() throws Exception {
		Node newOverview = content("news overview");
		Long date = System.currentTimeMillis();

		Microschema fullMicroschema = new MicroschemaModel();
		fullMicroschema.setName("full");

		//		fullMicroschema.addField(new BinaryFieldSchemaImpl().setName("binaryfield").setLabel("Binary Field"));
		fullMicroschema.addField(new BooleanFieldSchemaImpl().setName("booleanfield").setLabel("Boolean Field"));
		fullMicroschema.addField(new DateFieldSchemaImpl().setName("datefield").setLabel("Date Field"));
		fullMicroschema.addField(new HtmlFieldSchemaImpl().setName("htmlfield").setLabel("HTML Field"));
		//		fullMicroschema.addField(new ListFieldSchemaImpl().setListType("binary").setName("listfield-binary").setLabel("Binary List Field"));
		fullMicroschema.addField(new ListFieldSchemaImpl().setListType("boolean").setName("listfield-boolean").setLabel("Boolean List Field"));
		fullMicroschema.addField(new ListFieldSchemaImpl().setListType("date").setName("listfield-date").setLabel("Date List Field"));
		fullMicroschema.addField(new ListFieldSchemaImpl().setListType("html").setName("listfield-html").setLabel("Html List Field"));
		fullMicroschema.addField(new ListFieldSchemaImpl().setListType("node").setName("listfield-node").setLabel("Node List Field"));
		fullMicroschema.addField(new ListFieldSchemaImpl().setListType("number").setName("listfield-number").setLabel("Number List Field"));
		fullMicroschema.addField(new ListFieldSchemaImpl().setListType("string").setName("listfield-string").setLabel("String List Field"));
		fullMicroschema.addField(new NodeFieldSchemaImpl().setName("nodefield").setLabel("Node Field"));
		fullMicroschema.addField(new NumberFieldSchemaImpl().setName("numberfield").setLabel("Number Field"));
		fullMicroschema.addField(new StringFieldSchemaImpl().setName("stringfield").setLabel("String Field"));

		MicroschemaContainer microschemaContainer = boot.microschemaContainerRoot().create(fullMicroschema, getRequestUser());

		Node node = folder("2015");
		Schema schema = node.getSchemaContainer().getLatestVersion().getSchema();
		schema.addField(new MicronodeFieldSchemaImpl().setName("micronodefield").setLabel("Micronode Field"));
		node.getSchemaContainer().getLatestVersion().setSchema(schema);

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		MicronodeGraphField micronodeField = container.createMicronode("micronodefield", microschemaContainer.getLatestVersion());
		Micronode micronode = micronodeField.getMicronode();
		assertNotNull("Micronode must not be null", micronode);
		//		micronode.createBinary("binaryfield");
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
		//		micronode.createSelect("selectfield");
		micronode.createString("stringfield").setString("String Value");

		JsonObject jsonObject = new JsonObject(getJson(node));
		JsonObject fields = jsonObject.getJsonObject("fields");
		assertNotNull("JSON Object must contain fields", fields);
		JsonObject micronodeFieldObject = fields.getJsonObject("micronodefield");
		assertNotNull("JSON Object must contain micronode field", micronodeFieldObject);
		JsonObject micronodeFields = micronodeFieldObject.getJsonObject("fields");
		assertNotNull("Micronode must contain fields", micronodeFields);
		// TODO check binary field
		assertEquals("Boolean Field", Boolean.TRUE, micronodeFields.getBoolean("booleanfield"));
		assertEquals("Date Field", date, micronodeFields.getLong("datefield"));
		assertEquals("HTML Field", "<b>HTML</b> value", micronodeFields.getString("htmlfield"));
		// TODO check binary list field
		assertThat(micronodeFields.getJsonArray("listfield-boolean")).as("Boolean List Field").matches(true, false);
		assertThat(micronodeFields.getJsonArray("listfield-date")).as("Date List Field").matches(date, 0);
		assertThat(micronodeFields.getJsonArray("listfield-html")).as("HTML List Field").matches("<b>first</b>", "<i>second</i>");
		assertThat(micronodeFields.getJsonArray("listfield-node")).as("Node List Field").key("uuid").matches(node.getUuid(), newOverview.getUuid());
		assertThat(micronodeFields.getJsonArray("listfield-number")).as("Number List Field").matches(47, 11);
		// TODO check select list field
		assertThat(micronodeFields.getJsonArray("listfield-string")).as("String List Field").matches("first", "second", "third");
		assertThat(micronodeFields.getJsonObject("nodefield")).as("Node Field").key("uuid").matches(newOverview.getUuid());
		assertEquals("Number Field", 4711, micronodeFields.getInteger("numberfield").intValue());
		// TODO check select field
		assertEquals("String Field", "String Value", micronodeFields.getString("stringfield"));
	}

	/**
	 * Test creation of field
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateMicronodeField() throws Exception {
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

	@Test
	public void testMicronodeUpdateFromRest() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);

		MicronodeGraphField field = container.createMicronode("testMicronodeField", dummyMicroschema.getLatestVersion());
		Micronode micronode = field.getMicronode();

		InternalActionContext ac = getMockedInternalActionContext("");

		FieldMap restFields = new FieldMapImpl();
		restFields.put("stringfield", new StringFieldImpl().setString("test"));
		field.getMicronode().updateFieldsFromRest(ac, restFields, micronode.getMicroschema());

		assertNotNull("The field should have been created.", field.getMicronode().getString("stringfield"));
		assertEquals("The field did not contain the expected value", "test", field.getMicronode().getString("stringfield").getString());
	}

	/**
	 * Test updating the field
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdateMicronodeField() throws Exception {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);

		MicronodeGraphField field = container.createMicronode("testMicronodeField", dummyMicroschema.getLatestVersion());
		Micronode micronode = field.getMicronode();
		String originalUuid = micronode.getUuid();

		Set<? extends MicronodeImpl> existingMicronodes = tx.getGraph().v().has(MicronodeImpl.class).toSetExplicit(MicronodeImpl.class);
		for (Micronode foundMicronode : existingMicronodes) {
			assertEquals(micronode.getUuid(), foundMicronode.getUuid());
		}

		// update by recreation
		MicronodeGraphField updatedField = container.createMicronode("testMicronodeField", dummyMicroschema.getLatestVersion());
		Micronode updatedMicronode = updatedField.getMicronode();

		assertFalse("Uuid of micronode must be different after update", StringUtils.equalsIgnoreCase(originalUuid, updatedMicronode.getUuid()));

		existingMicronodes = tx.getGraph().v().has(MicronodeImpl.class).toSetExplicit(MicronodeImpl.class);
		for (MicronodeImpl foundMicronode : existingMicronodes) {
			assertEquals(updatedMicronode.getUuid(), foundMicronode.getUuid());
		}
	}

	/**
	 * Create a dummy microschema
	 * 
	 * @return
	 * @throws MeshJsonException
	 */
	protected MicroschemaContainer createDummyMicroschema() throws MeshJsonException {
		Microschema dummyMicroschema = new MicroschemaModel();
		dummyMicroschema.setName("dummymicroschema");

		// firstname field
		StringFieldSchema stringFieldSchema = new StringFieldSchemaImpl();
		stringFieldSchema.setName("stringfield");
		stringFieldSchema.setLabel("String Field");
		dummyMicroschema.addField(stringFieldSchema);

		return boot.microschemaContainerRoot().create(dummyMicroschema, getRequestUser());
	}
}
