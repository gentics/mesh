package com.gentics.mesh.core.data.fieldhandler;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

public abstract class AbstractComparatorNumberTest<C extends FieldSchemaContainer> extends AbstractSchemaComparatorTest<NumberFieldSchema, C> {

	@Override
	public NumberFieldSchema createField(String fieldName) {
		return FieldUtil.createNumberFieldSchema(fieldName);
	}

	@Test
	@Override
	public void testSameField() throws IOException {

		C containerA = createContainer();
		NumberFieldSchema fieldA = createField("test");
		fieldA.setLabel("label1");
		//		fieldA.setMin(1);
		//		fieldA.setMax(2);
		fieldA.setRequired(true);
		//		fieldA.setStep(0.1f);
		containerA.addField(fieldA);

		C containerB = createContainer();
		NumberFieldSchema fieldB = createField("test");
		//		fieldB.setMin(1);
		//		fieldB.setMax(2);
		fieldB.setRequired(true);
		//		fieldB.setStep(0.1f);
		fieldB.setLabel("label1");
		containerB.addField(fieldB);

		List<SchemaChangeModel> changes = getComparator().diff(containerA, containerB);
		assertThat(changes).isEmpty();
	}

	@Test
	@Override
	public void testUpdateField() throws IOException {

		C containerA = createContainer();
		NumberFieldSchema fieldA = createField("test");
		fieldA.setLabel("label1");
		//		fieldA.setMin(1);
		//		fieldA.setMax(2);
		fieldA.setRequired(true);
		//		fieldA.setStep(0.1f);
		containerA.addField(fieldA);

		C containerB = createContainer();
		NumberFieldSchema fieldB = createField("test");
		//		fieldB.setMin(1);
		//		fieldB.setMax(2);
		fieldB.setRequired(true);
		//		fieldB.setStep(0.1f);
		fieldB.setLabel("label2");
		containerB.addField(fieldB);

		// required flag:
		fieldB.setRequired(false);
		List<SchemaChangeModel> changes = getComparator().diff(containerA, containerB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasProperty("required", false).hasProperty("label", "label2");
		assertThat(changes.get(0).getProperties()).hasSize(3);
		fieldB.setRequired(true);

		//TODO min, max, step?

	}

}
