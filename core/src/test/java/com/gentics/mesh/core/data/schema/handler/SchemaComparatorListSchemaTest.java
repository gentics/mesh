package com.gentics.mesh.core.data.schema.handler;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.util.FieldUtil;

public class SchemaComparatorListSchemaTest extends AbstractSchemaComparatorTest<ListFieldSchema> {

	@Override
	public ListFieldSchema createField(String fieldName) {
		return FieldUtil.createListFieldSchema("test");
	}

	@Test
	@Override
	public void testSameField() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();

		ListFieldSchema fieldA = FieldUtil.createListFieldSchema("test");
		fieldA.setRequired(true);
		schemaA.addField(fieldA);

		ListFieldSchema fieldB = FieldUtil.createListFieldSchema("test");
		fieldB.setRequired(true);
		schemaB.addField(fieldB);

		List<SchemaChangeModel> list = comparator.diff(schemaA, schemaB);
		assertThat(list).isEmpty();

	}

	@Test
	@Override
	public void testUpdateField() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();

		ListFieldSchema fieldA = FieldUtil.createListFieldSchema("test");
		fieldA.setRequired(true);
		schemaA.addField(fieldA);

		ListFieldSchema fieldB = FieldUtil.createListFieldSchema("test");
		schemaB.addField(fieldB);

		// required flag:
		fieldB.setRequired(false);
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasProperty("required", false);
		assertThat(changes.get(0).getProperties()).hasSize(2);
		fieldB.setRequired(true);

		// list type:
		fieldB.setListType("boolean");
		changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasProperty("listType", "boolean");
		assertThat(changes.get(0).getProperties()).hasSize(2);
	}
}
