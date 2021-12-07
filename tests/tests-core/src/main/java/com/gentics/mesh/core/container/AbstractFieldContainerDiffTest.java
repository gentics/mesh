package com.gentics.mesh.core.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.CoreTestUtils;

public class AbstractFieldContainerDiffTest extends AbstractMeshTest {

	protected HibNodeFieldContainer createContainer(FieldSchema field) {
		return CoreTestUtils.createContainer(field);
	}

	protected SchemaVersionModel createSchema(FieldSchema field) {
		return CoreTestUtils.createSchema(field);
	}

	protected void assertChanges(List<FieldContainerChange> list, FieldChangeTypes expectedType) {
		assertNotNull("The list should never be null.", list);
		assertThat(list).hasSize(1);
		assertEquals("dummy", list.get(0).getFieldKey());
		assertEquals(expectedType, list.get(0).getType());
	}

	protected void assertNoDiff(List<FieldContainerChange> list) {
		assertNotNull("The list should never be null.", list);
		assertThat(list).isEmpty();
	}
}
