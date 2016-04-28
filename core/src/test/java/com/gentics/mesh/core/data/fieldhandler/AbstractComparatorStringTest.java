package com.gentics.mesh.core.data.fieldhandler;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.util.FieldUtil;

public abstract class AbstractComparatorStringTest<C extends FieldSchemaContainer> extends AbstractSchemaComparatorTest<StringFieldSchema, C> {

	@Override
	public StringFieldSchema createField(String fieldName) {
		return FieldUtil.createStringFieldSchema(fieldName);
	}

	@Test
	@Override
	public void testSameField() throws IOException {
		C containerA = createContainer();
		C containerB = createContainer();

		containerA.addField(createField("test"));
		containerB.addField(createField("test"));
		List<SchemaChangeModel> changes = getComparator().diff(containerA, containerB);
		assertThat(changes).isEmpty();
	}

	@Test
	public void testChangeFieldLabel() throws IOException {

		C containerA = createContainer();
		containerA.setName("test");
		NumberFieldSchema fieldA = FieldUtil.createNumberFieldSchema("test");
		fieldA.setLabel("OriginalLabel");
		containerA.addField(fieldA);

		String newLabel = "UpdatedLabel";
		C containerB = createContainer();
		containerB.setName("test");
		StringFieldSchema fieldB = createField("test");
		fieldB.setAllowedValues("testValueAllowed");
		fieldB.setLabel(newLabel);
		containerB.addField(fieldB);

		List<SchemaChangeModel> changes = getComparator().diff(containerA, containerB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(SchemaChangeOperation.CHANGEFIELDTYPE).forField("test").hasProperty(SchemaChangeModel.LABEL_KEY, newLabel)
				.hasProperty(SchemaChangeModel.ALLOW_KEY, new String[] { "testValueAllowed" }).hasProperty(SchemaChangeModel.TYPE_KEY, "string");

	}

	@Test
	@Override
	public void testUpdateField() throws IOException {
		C containerA = createContainer();
		C containerB = createContainer();

		StringFieldSchema fieldA = createField("test");
		containerA.addField(fieldA);
		StringFieldSchema fieldB = createField("test");
		containerB.addField(fieldB);

		// required flag:
		fieldB.setRequired(true);
		List<SchemaChangeModel> changes = getComparator().diff(containerA, containerB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasProperty("required", true);
		assertThat(changes.get(0).getProperties()).hasSize(2);

		// allow property:
		fieldA.setRequired(true);
		fieldA.setAllowedValues("blib");
		fieldB.setAllowedValues("changed");
		changes = getComparator().diff(containerA, containerB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasNoProperty("required").hasProperty("allow", new String[] { "changed" });
		assertThat(changes.get(0).getProperties()).hasSize(2);

	}

}
