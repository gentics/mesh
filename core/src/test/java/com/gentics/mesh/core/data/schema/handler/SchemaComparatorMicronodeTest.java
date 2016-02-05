package com.gentics.mesh.core.data.schema.handler;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.util.FieldUtil;

public class SchemaComparatorMicronodeTest extends AbstractSchemaComparatorTest {

	@Override
	public FieldSchema createField(String fieldName) {
		return FieldUtil.createMicronodeFieldSchema(fieldName);
	}

	@Test
	@Override
	public void testSameField() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();

		MicronodeFieldSchema fieldA = FieldUtil.createMicronodeFieldSchema("test");
		fieldA.setRequired(true);
		fieldA.setLabel("label1");
		fieldA.setAllowedMicroSchemas("one", "two");
		schemaA.addField(fieldA);

		MicronodeFieldSchema fieldB = FieldUtil.createMicronodeFieldSchema("test");
		fieldB.setRequired(true);
		fieldB.setLabel("label2");
		fieldB.setAllowedMicroSchemas("one", "two");
		schemaB.addField(fieldB);

		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).isEmpty();
	}

	@Test
	@Override
	public void testUpdateField() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();

		MicronodeFieldSchema fieldA = FieldUtil.createMicronodeFieldSchema("test");
		fieldA.setRequired(true);
		fieldA.setLabel("label1");
		fieldA.setAllowedMicroSchemas("one", "two");
		schemaA.addField(fieldA);

		MicronodeFieldSchema fieldB = FieldUtil.createMicronodeFieldSchema("test");
		fieldB.setRequired(true);
		fieldB.setLabel("label2");
		schemaB.addField(fieldB);

		// assert allow field changes: 
		fieldB.setAllowedMicroSchemas("one", "two", "three");
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasNoProperty("required").hasProperty("allow",
				new String[] { "one", "two", "three" });
		assertThat(changes.get(0).getProperties()).hasSize(2);

		// assert required flag:
		fieldB.setAllowedMicroSchemas("one", "two");
		fieldB.setRequired(false);
		changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasProperty("required", false).hasNoProperty("allow");

	}

}
