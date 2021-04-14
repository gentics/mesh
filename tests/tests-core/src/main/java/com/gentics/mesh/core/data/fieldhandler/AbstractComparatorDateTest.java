package com.gentics.mesh.core.data.fieldhandler;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.schema.DateFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

public abstract class AbstractComparatorDateTest<C extends FieldSchemaContainer> extends AbstractSchemaComparatorTest<DateFieldSchema, C> {

	@Override
	public DateFieldSchema createField(String fieldName) {
		return FieldUtil.createDateFieldSchema("test");
	}

	@Test
	@Override
	public void testSameField() throws IOException {
		C containerA = createContainer();
		containerA.setName("test");
		C containerB = createContainer();
		containerB.setName("test");

		DateFieldSchema fieldA = FieldUtil.createDateFieldSchema("test");
		fieldA.setLabel("label1");
		fieldA.setRequired(true);
		containerA.addField(fieldA);

		DateFieldSchema fieldB = FieldUtil.createDateFieldSchema("test");
		fieldB.setRequired(true);
		fieldB.setLabel("label1");
		containerB.addField(fieldB);

		List<SchemaChangeModel> changes = getComparator().diff(containerA, containerB);
		assertThat(changes).hasSize(0);
	}

	@Test
	@Override
	public void testUpdateField() throws IOException {
		C containerA = createContainer();
		containerA.setName("test");
		C containerB = createContainer();
		containerB.setName("test");

		DateFieldSchema fieldA = FieldUtil.createDateFieldSchema("test");
		fieldA.setLabel("label1");
		fieldA.setRequired(true);
		containerA.addField(fieldA);

		DateFieldSchema fieldB = FieldUtil.createDateFieldSchema("test");
		fieldB.setLabel("label2");
		containerB.addField(fieldB);

		// required flag:
		fieldB.setRequired(false);
		List<SchemaChangeModel> changes = getComparator().diff(containerA, containerB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasProperty("required", false).hasProperty("label", "label2");
		assertThat(changes.get(0).getProperties()).hasSize(3);

	}

}
