package com.gentics.mesh.core.data.fieldhandler;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.ADDFIELD;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.CHANGEFIELDTYPE;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.REMOVEFIELD;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;

import java.io.IOException;
import java.util.List;

import io.vertx.core.json.JsonObject;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.schema.handler.AbstractFieldSchemaContainerComparator;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.test.context.AbstractMeshTest;

public abstract class AbstractSchemaComparatorTest<T extends FieldSchema, C extends FieldSchemaContainer> extends AbstractMeshTest {

	public abstract AbstractFieldSchemaContainerComparator<C> getComparator();

	/**
	 * Create a new field container instance that will be used for comparison.
	 * 
	 * @return
	 */
	public abstract C createContainer();

	/**
	 * Create a new field to be used for the tests.
	 * 
	 * @param fieldName
	 * @return
	 */
	public abstract T createField(String fieldName);

	/**
	 * Test comparing two schema fields with same properties. Assert that no change was generated.
	 * 
	 * @throws IOException
	 */
	public abstract void testSameField() throws IOException;

	/**
	 * Test adding a field to a schema and assert that the expected change was generated.
	 *
	 * @throws IOException
	 */
	@Test
	public void testAddField() {
		C containerA = createContainer();
		containerA.setName("test");
		containerA.addField(FieldUtil.createStringFieldSchema("first"));

		C containerB = createContainer();
		containerB.setName("test");
		containerB.addField(FieldUtil.createStringFieldSchema("first"));

		// Add new field in B
		T field = createField("test");
		JsonObject indexSettings = new JsonObject().put("settings", "test");
		field.setElasticsearch(indexSettings);
		containerB.addField(field);

		List<SchemaChangeModel> changes = getComparator().diff(containerA, containerB);
		assertThat(changes).hasSize(2);
		assertThat(changes.get(0)).is(ADDFIELD).forField("test")
				.hasProperty("type", field.getType())
				.hasProperty("after", "first")
				.hasProperty("elasticsearch", indexSettings);
		assertThat(changes.get(1)).isUpdateOperation(containerA);
	}

	/**
	 * Test adding a field to a empty schema and assert that the expected change was generated.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testAddFieldToSchema() throws IOException {
		C containerA = createContainer();
		containerA.setName("test");
		C containerB = createContainer();
		containerB.setName("test");
		T field = createField("test");
		containerB.addField(field);

		List<SchemaChangeModel> changes = getComparator().diff(containerA, containerB);
		assertThat(changes).hasSize(2);
		assertThat(changes.get(0)).is(ADDFIELD).forField("test").hasProperty("type", field.getType());

		if (containerA.getFields().size() > 0) {
			String lastField = containerA.getFields().get(containerA.getFields().size() - 1).getName();
			assertThat(changes.get(0)).hasProperty("after", lastField);
		} else {
			assertThat(changes.get(0)).hasNoProperty("order");
		}
		assertThat(changes.get(1)).isUpdateOperation(containerA);

	}

	/**
	 * Test removing a field from a schema and assert that the expected change was generated.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testRemoveField() throws IOException {
		C containerA = createContainer();
		containerA.addField(createField("test"));
		C containerB = createContainer();

		List<SchemaChangeModel> changes = getComparator().diff(containerA, containerB);
		assertThat(changes.get(0)).is(REMOVEFIELD).forField("test");
	}

	/**
	 * Test updating the properties of a field and assert that the expected change was generated.
	 * 
	 * @throws IOException
	 */
	public abstract void testUpdateField() throws IOException;

	/**
	 * Test changing the field label in between two fields and assert that the expected change was generated.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testChangeFieldLabel() throws IOException {

		C containerA = createContainer();
		containerA.setName("test");
		T fieldA = createField("test");
		fieldA.setLabel("OriginalLabel");
		containerA.addField(fieldA);

		String newLabel = "UpdatedLabel";
		C containerB = createContainer();
		containerB.setName("test");
		T fieldB = createField("test");
		fieldB.setLabel(newLabel);
		containerB.addField(fieldB);

		List<SchemaChangeModel> changes = getComparator().diff(containerA, containerB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasProperty(SchemaChangeModel.LABEL_KEY, newLabel);

	}

	/**
	 * Test changing the field type in between two fields and assert that the expected change was generated.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testChangeFieldType() throws IOException {

		C containerA = createContainer();
		containerA.setName("test");
		T fieldA = createField("test");
		containerA.addField(fieldA);
		String newType = "html";

		C containerB = createContainer();
		containerB.setName("test");
		// Lists -> html field or basic field -> list 
		if (fieldA instanceof ListFieldSchema) {
			FieldSchema fieldB = FieldUtil.createHtmlFieldSchema("test");
			containerB.addField(fieldB);
		} else {
			ListFieldSchema fieldB = FieldUtil.createListFieldSchema("test");
			fieldB.setListType("html");
			containerB.addField(fieldB);
			newType = "list";
		}

		List<SchemaChangeModel> changes = getComparator().diff(containerA, containerB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(CHANGEFIELDTYPE).forField("test").hasProperty(SchemaChangeModel.TYPE_KEY, newType);
		if ("list".equals(newType)) {
			assertThat(changes.get(0)).hasProperty(SchemaChangeModel.LIST_TYPE_KEY, "html");
		}
	}

}
