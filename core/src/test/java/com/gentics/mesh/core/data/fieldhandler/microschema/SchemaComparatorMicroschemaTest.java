package com.gentics.mesh.core.data.fieldhandler.microschema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATESCHEMA;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaImpl;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.test.AbstractDBTest;
import com.gentics.mesh.util.FieldUtil;

public class SchemaComparatorMicroschemaTest extends AbstractDBTest {

	@Autowired
	private MicroschemaComparator comparator;

	@Test
	public void testEmptyMicroschema() {
		Microschema schemaA = new MicroschemaImpl();
		Microschema schemaB = new MicroschemaImpl();
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).isEmpty();
	}

	@Test
	public void testSchemaFieldReorder() {
		Microschema schemaA = new MicroschemaImpl();
		schemaA.addField(FieldUtil.createHtmlFieldSchema("first"));
		schemaA.addField(FieldUtil.createHtmlFieldSchema("second"));

		Microschema schemaB = new MicroschemaImpl();
		schemaB.addField(FieldUtil.createHtmlFieldSchema("second"));
		schemaB.addField(FieldUtil.createHtmlFieldSchema("first"));
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATESCHEMA).hasProperty("order", new String[] { "second", "first" });
	}

}
