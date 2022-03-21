package com.gentics.mesh.core.data.fieldhandler;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

public abstract class AbstractComparatorHtmlTest<C extends FieldSchemaContainer> extends AbstractSchemaComparatorTest<HtmlFieldSchema, C> {

	@Override
	public HtmlFieldSchema createField(String fieldName) {
		return FieldUtil.createHtmlFieldSchema(fieldName);
	}

	@Test
	@Override
	public void testSameField() throws IOException {
		C containerA = createContainer();
		C containerB = createContainer();

		HtmlFieldSchema fieldA = FieldUtil.createHtmlFieldSchema("test");
		fieldA.setRequired(true);
		containerA.addField(fieldA);

		HtmlFieldSchema fieldB = FieldUtil.createHtmlFieldSchema("test");
		fieldB.setRequired(true);
		containerB.addField(fieldB);

		List<SchemaChangeModel> changes = getComparator().diff(containerA, containerB);
		assertThat(changes).isEmpty();
	}

	@Test
	@Override
	public void testUpdateField() throws IOException {
		C containerA = createContainer();
		C containerB = createContainer();

		HtmlFieldSchema fieldA = FieldUtil.createHtmlFieldSchema("test");
		fieldA.setRequired(true);
		containerA.addField(fieldA);

		HtmlFieldSchema fieldB = FieldUtil.createHtmlFieldSchema("test");
		containerB.addField(fieldB);

		// required flag:
		fieldB.setRequired(false);
		List<SchemaChangeModel> changes = getComparator().diff(containerA, containerB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasProperty("required", false);
		assertThat(changes.get(0).getProperties()).hasSize(2);
	}

}
