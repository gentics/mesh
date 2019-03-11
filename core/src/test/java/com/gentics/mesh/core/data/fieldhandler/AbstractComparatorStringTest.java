package com.gentics.mesh.core.data.fieldhandler;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.ELASTICSEARCH_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.REQUIRED_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.util.IndexOptionHelper;

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
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasProperty(REQUIRED_KEY, true);
		assertThat(changes.get(0).getProperties()).hasSize(2);

		// index options:
		fieldB.setElasticsearch(IndexOptionHelper.getRawFieldOption());
		changes = getComparator().diff(containerA, containerB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasProperty(REQUIRED_KEY, true).hasProperty(ELASTICSEARCH_KEY, IndexOptionHelper
				.getRawFieldOption());
		assertThat(changes.get(0).getProperties()).hasSize(3);

		// allow property:
		fieldA.setRequired(true);
		fieldA.setElasticsearch(IndexOptionHelper.getRawFieldOption());
		fieldA.setAllowedValues("blib");
		fieldB.setAllowedValues("changed");
		changes = getComparator().diff(containerA, containerB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATEFIELD).forField("test").hasNoProperty(ELASTICSEARCH_KEY).hasNoProperty(REQUIRED_KEY).hasProperty("allow",
				new String[] { "changed" });
		assertThat(changes.get(0).getProperties()).hasSize(2);

	}

}
