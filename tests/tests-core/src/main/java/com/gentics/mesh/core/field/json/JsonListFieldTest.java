package com.gentics.mesh.core.field.json;

import static com.gentics.mesh.core.field.json.JsonFieldTestHelper.make;
import static com.gentics.mesh.core.field.json.JsonListFieldTestHelper.CREATE_EMPTY;
import static com.gentics.mesh.core.field.json.JsonListFieldTestHelper.FETCH;
import static com.gentics.mesh.core.field.json.JsonListFieldTestHelper.FILLTEXT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.list.HibJsonFieldList;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.JsonFieldListImpl;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.NoConsistencyCheck;
import com.gentics.mesh.util.CoreTestUtils;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = false)
public class JsonListFieldTest extends AbstractFieldTest<ListFieldSchema> {

	private static final String JSON_LIST = "jsonList";

	@Override
	protected ListFieldSchema createFieldSchema(boolean isRequired) {
		return createFieldSchema(JSON_LIST, isRequired);
	}
	protected ListFieldSchema createFieldSchema(String fieldKey, boolean isRequired) {
		ListFieldSchema schema = new ListFieldSchemaImpl();
		schema.setListType("json");
		schema.setName(fieldKey);
		schema.setRequired(isRequired);
		return schema;
	}

	@Test
	@Override
	public void testFieldTransformation() throws Exception {
		try (Tx tx = tx()) {
			HibNode node = folder("2015");
			ContentDao contentDao = tx.contentDao();
			prepareNode(node, JSON_LIST, "json");
			HibNodeFieldContainer container = contentDao.createFieldContainer(node, english(),
					node.getProject().getLatestBranch(), user(),
					contentDao.getLatestDraftFieldContainer(node, english()), true);
			HibJsonFieldList jsonList = container.createJsonList(JSON_LIST);
			jsonList.createJson(make("dummyString1"));
			jsonList.createJson(make("dummyString2"));
			tx.success();
		}

		try (Tx tx = tx()) {
			HibNode node = folder("2015");
			NodeResponse response = transform(node);
			assertList(2, JSON_LIST, "json", response);
		}
	}

	@Test
	@Override
	public void testFieldUpdate() throws Exception {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
			HibJsonFieldList list = container.createJsonList(JSON_LIST);

			list.createJson(make("1"));
			assertEquals(JSON_LIST, list.getFieldKey());
			assertNotNull(list.getList());

			assertEquals(1, list.getList().size());
			assertEquals(list.getSize(), list.getList().size());
			list.createJson(make("2"));
			assertEquals(2, list.getList().size());
			list.createJson(make("3")).setJson(make("Some json 3"));
			assertEquals(3, list.getList().size());
			assertEquals(make("Some json 3"), list.getList().get(2).getJson());

			HibJsonFieldList loadedList = container.getJsonList(JSON_LIST);
			assertNotNull(loadedList);
			assertEquals(3, loadedList.getSize());
			list.removeAll();
			assertEquals(0, list.getSize());
			assertEquals(0, list.getList().size());
		}
	}

	@Test
	@Override
	public void testBulkFieldUpdate() throws Exception {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
			HibJsonFieldList list = container.createJsonList(JSON_LIST);
			List<JsonObject> params = List.of("1","2","3","4","whatever").stream().map(JsonFieldTestHelper::make).collect(Collectors.toList());
			list.createJsons(params);
			assertEquals(5, list.getSize());
			assertEquals(5, list.getList().size());
			assertTrue(CollectionUtils.isEqualCollection(params, list.getValues()));
		}
	}

	@Test
	@NoConsistencyCheck
	@Override
	public void testClone() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
			HibJsonFieldList testField = container.createJsonList(JSON_LIST);
			testField.createJson(make("one"));
			testField.createJson(make("two"));
			testField.createJson(make("three"));

			HibNodeFieldContainer otherContainer = CoreTestUtils.createContainer(createFieldSchema(true));
			testField.cloneTo(otherContainer);

			assertThat(otherContainer.getJsonList(JSON_LIST).equals(testField));
		}
	}

	@Test
	@Override
	public void testEquals() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema("fieldA", true), createFieldSchema("fieldB", true));
			HibJsonFieldList fieldA = container.createJsonList("fieldA");
			HibJsonFieldList fieldB = container.createJsonList("fieldB");
			assertTrue("The field should  be equal to itself", fieldA.equals(fieldA));
			fieldA.addItem(fieldA.createJson(make("testString")));
			assertTrue("The field should  still be equal to itself", fieldA.equals(fieldA));

			assertFalse("The field should not be equal to a non-json field", fieldA.equals("bogus"));
			assertFalse("The field should not be equal since fieldB has no value", fieldA.equals(fieldB));
			fieldB.addItem(fieldB.createJson(make("testString")));
			assertTrue("Both fields have the same value and should be equal", fieldA.equals(fieldB));
		}
	}

	@Test
	@Override
	public void testEqualsNull() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
			HibJsonFieldList fieldA = container.createJsonList(JSON_LIST);
			assertFalse(fieldA.equals((Field) null));
			assertFalse(fieldA.equals((HibJsonFieldList) null));
		}
	}

	@Test
	@Override
	public void testEqualsRestField() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
			String dummyValue = "test123";

			// rest null - graph null
			HibJsonFieldList fieldA = container.createJsonList(JSON_LIST);

			JsonFieldListImpl restField = new JsonFieldListImpl();
			assertTrue("Both fields should be equal to eachother since both values are null", fieldA.equals(restField));

			// rest set - graph set - different values
			fieldA.addItem(fieldA.createJson(make(dummyValue)));
			restField.add(make(dummyValue + 1L));
			assertFalse("Both fields should be different since both values are not equal", fieldA.equals(restField));

			// rest set - graph set - same value
			restField.getItems().clear();
			restField.add(make(dummyValue));
			assertTrue("Both fields should be equal since values are equal", fieldA.equals(restField));

			HtmlFieldListImpl otherTypeRestField = new HtmlFieldListImpl();
			otherTypeRestField.add(dummyValue);
			// rest set - graph set - same value different type
			assertFalse("Fields should not be equal since the type does not match.", fieldA.equals(otherTypeRestField));
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreate() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestTestcase(JSON_LIST, FETCH, CREATE_EMPTY);
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreateRequired() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestNullOnCreateRequiredTestcase(JSON_LIST, FETCH);
		}
	}

	@Test
	@Override
	public void testRemoveFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveFieldViaNullTestcase(JSON_LIST, FETCH, FILLTEXT, (node) -> {
				updateContainer(ac, node, JSON_LIST, null);
			});
		}
	}

	@Test
	@Override
	public void testRemoveRequiredFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveRequiredFieldViaNullTestcase(JSON_LIST, FETCH, FILLTEXT, (container) -> {
				updateContainer(ac, container, JSON_LIST, null);
			});
		}
	}

	@Override
	public void testUpdateFromRestValidSimpleValue() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeUpdateFromRestValidSimpleValueTestcase(JSON_LIST, FILLTEXT, (container) -> {
				JsonFieldListImpl field = new JsonFieldListImpl();
				field.getItems().add(make("someValue"));
				field.getItems().add(make("someValue2"));
				updateContainer(ac, container, JSON_LIST, field);
			}, (container) -> {
				HibJsonFieldList field = container.getJsonList(JSON_LIST);
				assertNotNull("The graph field {" + JSON_LIST + "} could not be found.", field);
				assertEquals("The list of the field was not updated.", 2, field.getList().size());
				assertEquals("The list item of the field was not updated.", make("someValue"), field.getList().get(0).getJson());
				assertEquals("The list item of the field was not updated.", make("someValue2"), field.getList().get(1).getJson());
			});
		}
	}

}
