package com.gentics.mesh.core.data.schema.handler;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.ADDFIELD;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.CHANGEFIELDTYPE;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.REMOVEFIELD;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATESCHEMA;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.test.AbstractEmptyDBTest;
import com.gentics.mesh.util.FieldUtil;

public abstract class AbstractSchemaComparatorTest<T extends FieldSchema> extends AbstractEmptyDBTest {

	@Autowired
	protected SchemaComparator comparator;

	/**
	 * Create a new field to be used for the tests.
	 * 
	 * @param fieldName
	 * @return
	 */
	public abstract T createField(String fieldName);

	/**
	 * Test comparing two schema fields with same properties. Assert that no change was generated.
	 */
	public abstract void testSameField();

	/**
	 * Test adding a field to a schema and assert that the expected change was generated.
	 */
	@Test
	public void testAddField() {
		Schema schemaA = new SchemaImpl();

		Schema schemaB = new SchemaImpl();
		schemaB.addField(createField("test"));

		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(2);
		assertThat(changes.get(0)).is(ADDFIELD).forField("test");
		assertThat(changes.get(1)).is(UPDATESCHEMA).hasProperty("order", new String[] { "test" });

	}

	/**
	 * Test removing a field from a schema and assert that the expected change was generated.
	 */
	@Test
	public void testRemoveField() {
		Schema schemaA = new SchemaImpl();
		schemaA.addField(createField("test"));

		Schema schemaB = new SchemaImpl();

		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(REMOVEFIELD).forField("test");
	}

	/**
	 * Test updating the properties of a field and assert that the expected change was generated.
	 */
	public abstract void testUpdateField();

	/**
	 * Test changing the field type in between two fields and assert that the expected change was generated.
	 */
	@Test
	public void testChangeFieldType() {

		Schema schemaA = new SchemaImpl();
		T fieldA = createField("test");
		schemaA.addField(fieldA);
		String newType = "html";
		Schema schemaB = new SchemaImpl();
		// Lists -> html field or basic field -> list 
		if (fieldA instanceof ListFieldSchema) {
			FieldSchema fieldB = FieldUtil.createHtmlFieldSchema("test");
			schemaB.addField(fieldB);
		} else {
			ListFieldSchema fieldB = FieldUtil.createListFieldSchema("test");
			fieldB.setListType("html");
			schemaB.addField(fieldB);
			newType = "list";
		}

		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(CHANGEFIELDTYPE).forField("test").hasProperty("newType", newType);
		if ("list".equals(newType)) {
			assertThat(changes.get(0)).hasProperty("listType", "html");
		}

	}

}
