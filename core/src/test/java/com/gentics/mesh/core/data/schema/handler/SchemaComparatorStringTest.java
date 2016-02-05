package com.gentics.mesh.core.data.schema.handler;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.util.FieldUtil;

public class SchemaComparatorStringTest extends AbstractSchemaComparatorTest<StringFieldSchema> {

	@Override
	public StringFieldSchema createField(String fieldName) {
		return FieldUtil.createStringFieldSchema(fieldName);
	}

	@Test
	@Override
	public void testSameField() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.addField(createField("test"));
		schemaB.addField(createField("test"));
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).isEmpty();
	}

	@Test
	@Override
	public void testUpdateField() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		StringFieldSchema fieldA = createField("test");
		schemaA.addField(fieldA);
		StringFieldSchema fieldB = createField("test");
		schemaB.addField(fieldB);

		// required flag:
		fieldB.setRequired(true);
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasProperty("required", true);
		assertThat(changes.get(0).getProperties()).hasSize(2);

		// allow property:
		fieldA.setRequired(true);
		fieldA.setAllowedValues("blib");
		fieldB.setAllowedValues("changed");
		changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasNoProperty("required").hasProperty("allow", new String[] { "changed" });
		assertThat(changes.get(0).getProperties()).hasSize(2);

	}

}
