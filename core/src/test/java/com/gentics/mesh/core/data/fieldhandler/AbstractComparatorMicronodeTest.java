package com.gentics.mesh.core.data.fieldhandler;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.util.FieldUtil;

public abstract class AbstractComparatorMicronodeTest<C extends FieldSchemaContainer> extends AbstractSchemaComparatorTest<MicronodeFieldSchema, C> {

	@Override
	public MicronodeFieldSchema createField(String fieldName) {
		return FieldUtil.createMicronodeFieldSchema(fieldName);
	}

	@Test
	@Override
	public void testSameField() {
		C containerA = createContainer();
		C containerB = createContainer();

		MicronodeFieldSchema fieldA = FieldUtil.createMicronodeFieldSchema("test");
		fieldA.setRequired(true);
		fieldA.setLabel("label1");
		fieldA.setAllowedMicroSchemas("one", "two");
		containerA.addField(fieldA);

		MicronodeFieldSchema fieldB = FieldUtil.createMicronodeFieldSchema("test");
		fieldB.setRequired(true);
		fieldB.setLabel("label2");
		fieldB.setAllowedMicroSchemas("one", "two");
		containerB.addField(fieldB);

		List<SchemaChangeModel> changes = getComparator().diff(containerA, containerB);
		assertThat(changes).isEmpty();
	}

	@Test
	@Override
	public void testUpdateField() {
		C containerA = createContainer();
		C containerB = createContainer();

		MicronodeFieldSchema fieldA = FieldUtil.createMicronodeFieldSchema("test");
		fieldA.setRequired(true);
		fieldA.setLabel("label1");
		fieldA.setAllowedMicroSchemas("one", "two");
		containerA.addField(fieldA);

		MicronodeFieldSchema fieldB = FieldUtil.createMicronodeFieldSchema("test");
		fieldB.setRequired(true);
		fieldB.setLabel("label2");
		containerB.addField(fieldB);

		// assert allow field changes: 
		fieldB.setAllowedMicroSchemas("one", "two", "three");
		List<SchemaChangeModel> changes = getComparator().diff(containerA, containerB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasNoProperty("required").hasProperty("allow",
				new String[] { "one", "two", "three" });
		assertThat(changes.get(0).getProperties()).hasSize(2);

		// assert required flag:
		fieldB.setAllowedMicroSchemas("one", "two");
		fieldB.setRequired(false);
		changes = getComparator().diff(containerA, containerB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasProperty("required", false).hasNoProperty("allow");

	}

}
