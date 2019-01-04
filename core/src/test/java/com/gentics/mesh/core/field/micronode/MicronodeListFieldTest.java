package com.gentics.mesh.core.field.micronode;

import static com.gentics.mesh.core.field.micronode.MicronodeListFieldHelper.CREATE_EMPTY;
import static com.gentics.mesh.core.field.micronode.MicronodeListFieldHelper.FETCH;
import static com.gentics.mesh.core.field.micronode.MicronodeListFieldHelper.FILL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.MicronodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.test.context.MeshTestSetting;
import static com.gentics.mesh.test.TestSize.FULL;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = false)
public class MicronodeListFieldTest extends AbstractFieldTest<ListFieldSchema> {

	private static final String MICRONODE_LIST = "micronodeList";

	@Override
	protected ListFieldSchema createFieldSchema(boolean isRequired) {
		ListFieldSchema schema = new ListFieldSchemaImpl();
		schema.setListType("micronode");
		schema.setName(MICRONODE_LIST);
		schema.setRequired(isRequired);
		return schema;
	}

	@Test
	@Override
	public void testFieldTransformation() throws Exception {
		Node node = folder("2015");
		
		try (Tx tx = tx()) {
			prepareNode(node, MICRONODE_LIST, "micronode");
			InternalActionContext ac = mockActionContext("");

			NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english());

			MicronodeFieldListImpl field = new MicronodeFieldListImpl();
			MicronodeResponse micronodeA = new MicronodeResponse();
			micronodeA.setMicroschema(new MicroschemaReferenceImpl().setName("vcard"));
			micronodeA.getFields().put("firstName", FieldUtil.createStringField("updatedFirstname1"));
			micronodeA.getFields().put("lastName", FieldUtil.createStringField("updatedLastname1"));
			field.getItems().add(micronodeA);

			MicronodeResponse micronodeB = new MicronodeResponse();
			micronodeB.setMicroschema(new MicroschemaReferenceImpl().setName("vcard"));
			micronodeB.getFields().put("firstName", FieldUtil.createStringField("updatedFirstname2"));
			micronodeB.getFields().put("lastName", FieldUtil.createStringField("updatedLastname2"));
			field.getItems().add(micronodeB);

			updateContainer(ac, container, MICRONODE_LIST, field);
			tx.success();
		}
		
		try (Tx tx = tx()) {
			NodeResponse response = transform(node);
			assertList(2, "micronodeList", "micronode", response);
			MicronodeFieldList micronodeRestList = response.getFields().getMicronodeFieldList(MICRONODE_LIST);

			// Assert first micronode
			StringField firstnameAField = micronodeRestList.getItems().get(0).getFields().getStringField("firstName");
			assertNotNull(
					"The firstname string field for the first micronode could not be found. It should not be null.",
					firstnameAField);
			assertEquals("updatedFirstname1", firstnameAField.getString());
			StringField lastnameAField = micronodeRestList.getItems().get(0).getFields().getStringField("lastName");
			assertNotNull(
					"The lastname string field for the first micronode could not be found. It should not be null.",
					lastnameAField);
			assertEquals("updatedLastname1", lastnameAField.getString());

			// Assert second micronode
			StringField firstnameBField = micronodeRestList.getItems().get(1).getFields().getStringField("firstName");
			assertNotNull("The string field for the second micronode could not be found. It should not be null.",
					firstnameBField);
			assertEquals("updatedFirstname2", firstnameBField.getString());
			StringField lastnameBField = micronodeRestList.getItems().get(1).getFields().getStringField("lastName");
			assertNotNull("The string field for the second micronode could not be found. It should not be null.",
					lastnameBField);
			assertEquals("updatedLastname2", lastnameBField.getString());
		}
	}

	@Test
	@Override
	public void testFieldUpdate() throws Exception {
		try (Tx tx = tx()) {
			NodeGraphFieldContainer container = tx.createVertex(NodeGraphFieldContainerImpl.class);
			MicronodeGraphFieldList list = container.createMicronodeFieldList("dummyList");
			assertNotNull(list);
		}
	}

	@Test
	@Override
	public void testClone() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainer container = tx.createVertex(NodeGraphFieldContainerImpl.class);
			MicronodeGraphFieldList testField = container.createMicronodeFieldList("testField");

			Micronode micronode = testField.createMicronode();
			micronode.setSchemaContainerVersion(microschemaContainers().get("vcard").getLatestVersion());
			micronode.createString("firstName").setString("Donald");
			micronode.createString("lastName").setString("Duck");

			micronode = testField.createMicronode();
			micronode.setSchemaContainerVersion(microschemaContainers().get("vcard").getLatestVersion());
			micronode.createString("firstName").setString("Mickey");
			micronode.createString("lastName").setString("Mouse");

			NodeGraphFieldContainerImpl otherContainer = tx.getGraph()
					.addFramedVertex(NodeGraphFieldContainerImpl.class);
			testField.cloneTo(otherContainer);

			assertThat(otherContainer.getMicronodeList("testField")).as("cloned field")
					.isEqualToComparingFieldByField(testField);
		}
	}

	@Test
	@Override
	public void testEquals() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainerImpl container = tx.createVertex(NodeGraphFieldContainerImpl.class);
			MicronodeGraphFieldList fieldA = container.createMicronodeFieldList("fieldA");
			MicronodeGraphFieldList fieldB = container.createMicronodeFieldList("fieldB");
			assertTrue("The field should  be equal to itself", fieldA.equals(fieldA));

			// Create a micronode within the first list
			Micronode micronodeA = fieldA.createMicronode();
			micronodeA.setSchemaContainerVersion(microschemaContainer("vcard").getLatestVersion());
			micronodeA.createString("firstName").setString("Donald");
			micronodeA.createString("lastName").setString("Duck");
			assertTrue("The field should  still be equal to itself", fieldA.equals(fieldA));

			assertFalse("The field should not be equal to a non-string field", fieldA.equals("bogus"));
			assertFalse("The field should not be equal since fieldB has no value", fieldA.equals(fieldB));
			// Create another micronode in the second list
			Micronode micronodeB = fieldB.createMicronode();
			micronodeB.setSchemaContainerVersion(microschemaContainer("vcard").getLatestVersion());
			micronodeB.createString("firstName").setString("Donald");
			micronodeB.createString("lastName").setString("Duck");

			assertTrue("Both fields have the same value and should be equal", fieldA.equals(fieldB));

			// Add another list to fieldB
			Micronode micronodeC = fieldB.createMicronode();
			micronodeC.setSchemaContainerVersion(microschemaContainer("vcard").getLatestVersion());
			micronodeC.createString("firstName").setString("Donald");
			micronodeC.createString("lastName").setString("Duck");
			assertFalse("Field b contains more items compared to field a and thus both lists are not equal",
					fieldA.equals(fieldB));
		}
	}

	@Test
	@Override
	public void testEqualsNull() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainerImpl container = tx.createVertex(NodeGraphFieldContainerImpl.class);
			MicronodeGraphFieldList fieldA = container.createMicronodeFieldList("fieldA");
			assertFalse(fieldA.equals((Field) null));
			assertFalse(fieldA.equals((GraphField) null));
		}
	}

	@Test
	@Override
	public void testEqualsRestField() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainer container = tx.createVertex(NodeGraphFieldContainerImpl.class);

			// rest null - graph null
			MicronodeGraphFieldList fieldA = container.createMicronodeFieldList(MICRONODE_LIST);

			MicronodeFieldListImpl restField = new MicronodeFieldListImpl();
			assertTrue("Both fields should be equal to eachother since both values are null", fieldA.equals(restField));

			// rest set - graph set - different values
			Micronode micronodeA = fieldA.createMicronode();
			micronodeA.setSchemaContainerVersion(microschemaContainer("vcard").getLatestVersion());
			micronodeA.createString("firstName").setString("Donald");
			MicronodeResponse dummyValue2 = new MicronodeResponse();
			dummyValue2.getFields().put("firstName", FieldUtil.createStringField("Dagobert"));
			restField.add(dummyValue2);
			assertFalse("Both fields should be different since both values are not equal", fieldA.equals(restField));

			// rest set - graph set - same value
			restField.getItems().clear();
			MicronodeResponse dummyValue = new MicronodeResponse();
			dummyValue.getFields().put("firstName", FieldUtil.createStringField("Donald"));
			restField.add(dummyValue);
			assertTrue("Both fields should be equal since values are equal", fieldA.equals(restField));

			StringFieldListImpl otherTypeRestField = new StringFieldListImpl();
			otherTypeRestField.add("test");
			// rest set - graph set - same value different type
			assertFalse("Fields should not be equal since the type does not match.", fieldA.equals(otherTypeRestField));
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreate() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestTestcase(MICRONODE_LIST, FETCH, CREATE_EMPTY);
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreateRequired() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestNullOnCreateRequiredTestcase(MICRONODE_LIST, FETCH);
		}
	}

	@Test
	@Override
	public void testRemoveFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveFieldViaNullTestcase(MICRONODE_LIST, FETCH, FILL, (node) -> {
				updateContainer(ac, node, MICRONODE_LIST, null);
			});
		}
	}

	@Test
	@Override
	public void testRemoveRequiredFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveRequiredFieldViaNullTestcase(MICRONODE_LIST, FETCH, FILL, (container) -> {
				updateContainer(ac, container, MICRONODE_LIST, null);
			});
		}
	}

	@Test
	@Override
	public void testUpdateFromRestValidSimpleValue() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeUpdateFromRestValidSimpleValueTestcase(MICRONODE_LIST, FILL, (container) -> {
				MicronodeFieldListImpl field = new MicronodeFieldListImpl();
				MicronodeResponse micronodeA = new MicronodeResponse();
				micronodeA.setMicroschema(new MicroschemaReferenceImpl().setName("vcard"));
				micronodeA.getFields().put("firstName", FieldUtil.createStringField("updatedFirstname1"));
				micronodeA.getFields().put("lastName", FieldUtil.createStringField("updatedLastname1"));
				field.getItems().add(micronodeA);

				MicronodeResponse micronodeB = new MicronodeResponse();
				micronodeB.setMicroschema(new MicroschemaReferenceImpl().setName("vcard"));
				micronodeB.getFields().put("firstName", FieldUtil.createStringField("updatedFirstname2"));
				micronodeB.getFields().put("lastName", FieldUtil.createStringField("updatedLastname2"));
				field.getItems().add(micronodeB);

				updateContainer(ac, container, MICRONODE_LIST, field);
			}, (container) -> {
				MicronodeGraphFieldList field = container.getMicronodeList(MICRONODE_LIST);
				assertNotNull("The graph field {" + MICRONODE_LIST + "} could not be found.", field);
				assertEquals("The list of the field was not updated.", 2, field.getList().size());
				assertEquals("The list item of the field was not updated.", "updatedLastname1",
						field.getList().get(0).getMicronode().getString("lastName").getString());
				assertEquals("The list item of the field was not updated.", "updatedFirstname1",
						field.getList().get(0).getMicronode().getString("firstName").getString());

				assertEquals("The list item of the field was not updated.", "updatedLastname2",
						field.getList().get(1).getMicronode().getString("lastName").getString());
				assertEquals("The list item of the field was not updated.", "updatedFirstname2",
						field.getList().get(1).getMicronode().getString("firstName").getString());

			});
		}
	}

}
