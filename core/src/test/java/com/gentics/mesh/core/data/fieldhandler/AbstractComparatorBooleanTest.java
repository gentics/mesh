package com.gentics.mesh.core.data.fieldhandler;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

public abstract class AbstractComparatorBooleanTest<C extends FieldSchemaContainer> extends AbstractSchemaComparatorTest<BooleanFieldSchema, C> {

	@Override
	public BooleanFieldSchema createField(String fieldName) {
		return FieldUtil.createBooleanFieldSchema(fieldName);
	}

	@Test
	@Override
	public void testSameField() throws IOException {
		C containerA = createContainer();
		C containerB = createContainer();

		BooleanFieldSchema fieldA = createField("test");
		fieldA.setRequired(true);
		fieldA.setLabel("label1");
		containerA.addField(fieldA);

		BooleanFieldSchema fieldB = createField("test");
		fieldB.setRequired(true);
		fieldB.setLabel("label1");
		containerB.addField(fieldB);

		List<SchemaChangeModel> changes = getComparator().diff(containerA, containerB);
		assertThat(changes).isEmpty();

	}

	@Test
	@Override
	public void testUpdateField() throws IOException {
		C containerA = createContainer();
		C containerB = createContainer();
		BooleanFieldSchema fieldA = createField("test");
		containerA.addField(fieldA);
		BooleanFieldSchema fieldB = createField("test");
		containerB.addField(fieldB);

		// required flag:
		fieldB.setRequired(true);
		List<SchemaChangeModel> changes = getComparator().diff(containerA, containerB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasProperty("required", true);
		assertThat(changes.get(0).getProperties()).hasSize(2);

	}
}
