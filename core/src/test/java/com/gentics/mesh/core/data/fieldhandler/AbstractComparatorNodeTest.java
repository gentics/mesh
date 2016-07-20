package com.gentics.mesh.core.data.fieldhandler;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

public abstract class AbstractComparatorNodeTest<C extends FieldSchemaContainer> extends AbstractSchemaComparatorTest<NodeFieldSchema, C> {

	@Override
	public NodeFieldSchema createField(String fieldName) {
		return FieldUtil.createNodeFieldSchema(fieldName);
	}

	@Test
	@Override
	public void testSameField() throws IOException {
		C containerA = createContainer();
		C containerB = createContainer();

		NodeFieldSchema fieldA = createField("test");
		fieldA.setRequired(true);
		fieldA.setAllowedSchemas("one", "two");
		fieldA.setLabel("label1");
		containerA.addField(fieldA);

		NodeFieldSchema fieldB = createField("test");
		fieldB.setRequired(true);
		fieldB.setAllowedSchemas("one", "two");
		fieldB.setLabel("label1");
		containerB.addField(fieldB);

		List<SchemaChangeModel> changes = getComparator().diff(containerA, containerB);
		assertThat(changes).isEmpty();
	}

	@Test
	@Override
	public void testUpdateField() throws IOException {
		C schemaA = createContainer();
		C schemaB = createContainer();

		NodeFieldSchema fieldA = createField("test");
		fieldA.setRequired(true);
		fieldA.setAllowedSchemas("one", "two");
		fieldA.setLabel("label1");
		schemaA.addField(fieldA);

		// field B has an additional allow property value
		NodeFieldSchema fieldB = createField("test");
		fieldB.setRequired(true);
		fieldB.setAllowedSchemas("one", "two", "three");
		fieldB.setLabel("label1");
		schemaB.addField(fieldB);

		// assert that the additional allow property has been detected
		List<SchemaChangeModel> changes = getComparator().diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasProperty("allow", new String[] { "one", "two", "three" });
		assertThat(changes.get(0).getProperties()).hasSize(2);

		// assert that required flag changes and that the allow property does no longer cause a diff since both values are not the same 
		fieldA.setAllowedSchemas("one", "two", "three");
		fieldB.setRequired(false);
		changes = getComparator().diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasNoProperty("allow").hasProperty("required", false);
		assertThat(changes.get(0).getProperties()).hasSize(2);

	}

}
