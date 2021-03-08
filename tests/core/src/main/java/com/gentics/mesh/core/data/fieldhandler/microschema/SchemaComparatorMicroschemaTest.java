package com.gentics.mesh.core.data.fieldhandler.microschema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.TestSize.FULL;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparatorImpl;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = false)
public class SchemaComparatorMicroschemaTest extends AbstractMeshTest {

	@Test
	public void testEmptyMicroschema() throws IOException {
		MicroschemaModel schemaA = FieldUtil.createMinimalValidMicroschema();
		MicroschemaModel schemaB = FieldUtil.createMinimalValidMicroschema();
		List<SchemaChangeModel> changes = new MicroschemaComparatorImpl().diff(schemaA, schemaB);
		assertThat(changes).isEmpty();
	}

	@Test
	public void testSchemaFieldReorder() throws IOException {
		MicroschemaModel schemaA = FieldUtil.createMinimalValidMicroschema();
		schemaA.addField(FieldUtil.createHtmlFieldSchema("first"));
		schemaA.addField(FieldUtil.createHtmlFieldSchema("second"));

		MicroschemaModel schemaB = FieldUtil.createMinimalValidMicroschema();
		schemaB.addField(FieldUtil.createHtmlFieldSchema("second"));
		schemaB.addField(FieldUtil.createHtmlFieldSchema("first"));
		List<SchemaChangeModel> changes = new MicroschemaComparatorImpl().diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).isUpdateOperation(schemaA).hasProperty("order", new String[] { "second", "first" });
	}

}
