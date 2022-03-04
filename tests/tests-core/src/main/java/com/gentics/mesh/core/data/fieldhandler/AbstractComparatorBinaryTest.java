package com.gentics.mesh.core.data.fieldhandler;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

public abstract class AbstractComparatorBinaryTest<C extends FieldSchemaContainer> extends AbstractSchemaComparatorTest<BinaryFieldSchema, C> {

	@Override
	public BinaryFieldSchema createField(String fieldName) {
		return FieldUtil.createBinaryFieldSchema(fieldName);
	}

	@Test
	@Override
	public void testSameField() throws IOException {
		C containerA = createContainer();
		C containerB = createContainer();

		BinaryFieldSchema fieldA = FieldUtil.createBinaryFieldSchema("test");
		fieldA.setAllowedMimeTypes("some", "values");
		fieldA.setLabel("someLabel");
		fieldA.setRequired(true);

		BinaryFieldSchema fieldB = FieldUtil.createBinaryFieldSchema("test");
		fieldB.setAllowedMimeTypes("some", "values");
		fieldB.setLabel("someLabel");
		fieldB.setRequired(true);

		containerA.addField(fieldA);
		containerB.addField(fieldB);

		List<SchemaChangeModel> changes = getComparator().diff(containerA, containerB);
		assertThat(changes).isEmpty();

	}

	@Test
	@Override
	public void testUpdateField() throws IOException {
		C containerA = createContainer();
		C containerB = createContainer();

		BinaryFieldSchema fieldA = FieldUtil.createBinaryFieldSchema("test");
		fieldA.setRequired(true);
		containerA.addField(fieldA);

		BinaryFieldSchema fieldB = FieldUtil.createBinaryFieldSchema("test");
		containerB.addField(fieldB);

		// assert required flag:
		fieldB.setRequired(false);
		List<SchemaChangeModel> changes = getComparator().diff(containerA, containerB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasProperty("required", false);
		assertThat(changes.get(0).getProperties()).hasSize(2);

		// assert allowed mimetypes
		fieldB.setRequired(true);
		fieldB.setAllowedMimeTypes("one", "two");
		changes = getComparator().diff(containerA, containerB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasProperty("allow", new String[] { "one", "two" });
		assertThat(changes.get(0).getProperties()).hasSize(2);

	}

}