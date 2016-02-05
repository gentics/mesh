package com.gentics.mesh.core.data.schema.handler;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.util.FieldUtil;

public class SchemaComparatorNumberTest extends AbstractSchemaComparatorTest {

	@Override
	public FieldSchema createField(String fieldName) {
		return FieldUtil.createNumberFieldSchema(fieldName);
	}

	@Test
	@Override
	public void testSameField() {

		Schema schemaA = new SchemaImpl();
		NumberFieldSchema fieldA = FieldUtil.createNumberFieldSchema("test");
		fieldA.setLabel("label1");
		fieldA.setMin(1);
		fieldA.setMax(2);
		fieldA.setRequired(true);
		fieldA.setStep(0.1f);
		schemaA.addField(fieldA);

		Schema schemaB = new SchemaImpl();
		NumberFieldSchema fieldB = FieldUtil.createNumberFieldSchema("test");
		fieldB.setMin(1);
		fieldB.setMax(2);
		fieldB.setRequired(true);
		fieldB.setStep(0.1f);
		fieldB.setLabel("label2");
		schemaB.addField(fieldB);

		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).isEmpty();
	}

	@Test
	@Override
	public void testUpdateField() {

		Schema schemaA = new SchemaImpl();
		NumberFieldSchema fieldA = FieldUtil.createNumberFieldSchema("test");
		fieldA.setLabel("label1");
		fieldA.setMin(1);
		fieldA.setMax(2);
		fieldA.setRequired(true);
		fieldA.setStep(0.1f);
		schemaA.addField(fieldA);

		Schema schemaB = new SchemaImpl();
		NumberFieldSchema fieldB = FieldUtil.createNumberFieldSchema("test");
		fieldB.setMin(1);
		fieldB.setMax(2);
		fieldB.setRequired(true);
		fieldB.setStep(0.1f);
		fieldB.setLabel("label2");
		schemaB.addField(fieldB);

		// required flag:
		fieldB.setRequired(false);
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasProperty("required", false);
		assertThat(changes.get(0).getProperties()).hasSize(2);
		fieldB.setRequired(true);

		//TODO min, max, step?

	}

}
