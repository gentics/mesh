package com.gentics.mesh.core.data.schema.handler;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.rest.schema.DateFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.util.FieldUtil;

public class SchemaComparatorDateTest extends AbstractSchemaComparatorTest<DateFieldSchema> {

	@Override
	public DateFieldSchema createField(String fieldName) {
		return FieldUtil.createDateFieldSchema("test");
	}

	@Test
	@Override
	public void testSameField() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();

		DateFieldSchema fieldA = FieldUtil.createDateFieldSchema("test");
		fieldA.setLabel("label1");
		fieldA.setRequired(true);
		schemaA.addField(fieldA);

		DateFieldSchema fieldB = FieldUtil.createDateFieldSchema("test");
		fieldB.setRequired(true);
		fieldB.setLabel("label2");
		schemaB.addField(fieldB);

		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(0);
	}

	@Test
	@Override
	public void testUpdateField() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();

		DateFieldSchema fieldA = FieldUtil.createDateFieldSchema("test");
		fieldA.setLabel("label1");
		fieldA.setRequired(true);
		schemaA.addField(fieldA);

		DateFieldSchema fieldB = FieldUtil.createDateFieldSchema("test");
		fieldB.setLabel("label2");
		schemaB.addField(fieldB);

		// required flag:
		fieldB.setRequired(false);
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasProperty("required", false);
		assertThat(changes.get(0).getProperties()).hasSize(2);

	}

}
