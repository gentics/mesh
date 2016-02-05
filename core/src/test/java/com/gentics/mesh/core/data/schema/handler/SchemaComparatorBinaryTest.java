package com.gentics.mesh.core.data.schema.handler;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.util.FieldUtil;

public class SchemaComparatorBinaryTest extends AbstractSchemaComparatorTest<BinaryFieldSchema> {

	@Override
	public BinaryFieldSchema createField(String fieldName) {
		return FieldUtil.createBinaryFieldSchema(fieldName);
	}

	@Test
	@Override
	public void testSameField() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();

		BinaryFieldSchema fieldA = FieldUtil.createBinaryFieldSchema("test");
		fieldA.setAllowedMimeTypes("some", "values");
		// The label has no effect on the content and thus can be different.
		fieldA.setLabel("someOtherLabel");
		fieldA.setRequired(true);

		BinaryFieldSchema fieldB = FieldUtil.createBinaryFieldSchema("test");
		fieldB.setAllowedMimeTypes("some", "values");
		fieldB.setLabel("someLabel");
		fieldB.setRequired(true);

		schemaA.addField(fieldA);
		schemaB.addField(fieldB);

		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).isEmpty();

	}

	@Test
	@Override
	public void testUpdateField() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();

		BinaryFieldSchema fieldA = FieldUtil.createBinaryFieldSchema("test");
		fieldA.setRequired(true);
		schemaA.addField(fieldA);

		BinaryFieldSchema fieldB = FieldUtil.createBinaryFieldSchema("test");
		schemaB.addField(fieldB);

		// assert required flag:
		fieldB.setRequired(false);
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasProperty("required", false);
		assertThat(changes.get(0).getProperties()).hasSize(2);

		// assert allowed mimetypes
		fieldB.setRequired(true);
		fieldB.setAllowedMimeTypes("one", "two");
		changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasProperty("allow", new String[] { "one", "two" });
		assertThat(changes.get(0).getProperties()).hasSize(2);

	}

}