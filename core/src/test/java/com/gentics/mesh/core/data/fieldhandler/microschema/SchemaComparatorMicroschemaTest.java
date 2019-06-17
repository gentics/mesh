package com.gentics.mesh.core.data.fieldhandler.microschema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import static com.gentics.mesh.test.TestSize.FULL;

@MeshTestSetting(testSize = FULL, startServer = false)
public class SchemaComparatorMicroschemaTest extends AbstractMeshTest {

	@Test
	public void testEmptyMicroschema() throws IOException {
		Microschema schemaA = FieldUtil.createMinimalValidMicroschema();
		Microschema schemaB = FieldUtil.createMinimalValidMicroschema();
		List<SchemaChangeModel> changes = new MicroschemaComparator().diff(schemaA, schemaB);
		assertThat(changes).isEmpty();
	}

	@Test
	public void testSchemaFieldReorder() throws IOException {
		Microschema schemaA = FieldUtil.createMinimalValidMicroschema();
		schemaA.addField(FieldUtil.createHtmlFieldSchema("first"));
		schemaA.addField(FieldUtil.createHtmlFieldSchema("second"));

		Microschema schemaB = FieldUtil.createMinimalValidMicroschema();
		schemaB.addField(FieldUtil.createHtmlFieldSchema("second"));
		schemaB.addField(FieldUtil.createHtmlFieldSchema("first"));
		List<SchemaChangeModel> changes = new MicroschemaComparator().diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).isUpdateOperation(schemaA).hasProperty("order", new String[] { "second", "first" });
	}

}
