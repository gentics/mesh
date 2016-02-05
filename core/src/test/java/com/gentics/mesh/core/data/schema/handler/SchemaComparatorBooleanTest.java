package com.gentics.mesh.core.data.schema.handler;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.util.FieldUtil;

public class SchemaComparatorBooleanTest extends AbstractSchemaComparatorTest<BooleanFieldSchema> {

	@Override
	public BooleanFieldSchema createField(String fieldName) {
		return FieldUtil.createBooleanFieldSchema(fieldName);
	}

	@Test
	@Override
	public void testSameField() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();

		BooleanFieldSchema fieldA = createField("test");
		fieldA.setRequired(true);
		fieldA.setLabel("label1");
		schemaA.addField(fieldA);

		BooleanFieldSchema fieldB = createField("test");
		fieldB.setRequired(true);
		fieldB.setLabel("label2");
		schemaB.addField(fieldB);

		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).isEmpty();

	}

	@Test
	@Override
	public void testUpdateField() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		BooleanFieldSchema fieldA = createField("test");
		schemaA.addField(fieldA);
		BooleanFieldSchema fieldB = createField("test");
		schemaB.addField(fieldB);

		// required flag:
		fieldB.setRequired(true);
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasProperty("required", true);
		assertThat(changes.get(0).getProperties()).hasSize(2);

	}
}
