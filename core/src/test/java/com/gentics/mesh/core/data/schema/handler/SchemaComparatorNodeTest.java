package com.gentics.mesh.core.data.schema.handler;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.util.FieldUtil;

public class SchemaComparatorNodeTest extends AbstractSchemaComparatorTest<NodeFieldSchema> {

	@Override
	public NodeFieldSchema createField(String fieldName) {
		return FieldUtil.createNodeFieldSchema(fieldName);
	}

	@Test
	@Override
	public void testSameField() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();

		NodeFieldSchema fieldA = createField("test");
		fieldA.setRequired(true);
		fieldA.setAllowedSchemas("one", "two");
		fieldA.setLabel("label1");
		schemaA.addField(fieldA);

		NodeFieldSchema fieldB = createField("test");
		fieldB.setRequired(true);
		fieldB.setAllowedSchemas("one", "two");
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

		NodeFieldSchema fieldA = createField("test");
		fieldA.setRequired(true);
		fieldA.setAllowedSchemas("one", "two");
		fieldA.setLabel("label1");
		schemaA.addField(fieldA);

		NodeFieldSchema fieldB = createField("test");
		fieldB.setRequired(true);
		fieldB.setLabel("label1");
		schemaB.addField(fieldB);

		// assert allow property:
		fieldB.setAllowedSchemas("one", "two", "three");
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasProperty("allow", new String[] { "one", "two", "three" });
		assertThat(changes.get(0).getProperties()).hasSize(2);

		// assert required flag:
		fieldA.setAllowedSchemas("one", "two", "three");
		fieldB.setRequired(false);
		changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasNoProperty("allow").hasProperty("required", false);
		assertThat(changes.get(0).getProperties()).hasSize(2);

	}

}
