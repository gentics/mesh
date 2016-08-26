package com.gentics.mesh.core.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Before;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.test.AbstractDBTest;
import com.syncleus.ferma.FramedGraph;

public class AbstractFieldContainerDiffTest extends AbstractDBTest {

	@Before
	public void setup() throws Exception {
		initDagger();
	}

	protected NodeGraphFieldContainer createContainer(FieldSchema field) {
		FramedGraph graph = Database.getThreadLocalGraph();
		// 1. Setup schema
		SchemaContainer schemaContainer = graph.addFramedVertex(SchemaContainerImpl.class);
		SchemaContainerVersionImpl version = graph.addFramedVertex(SchemaContainerVersionImpl.class);
		version.setSchemaContainer(schemaContainer);

		Schema schema = createSchema(field);
		version.setSchema(schema);

		NodeGraphFieldContainerImpl container = graph.addFramedVertex(NodeGraphFieldContainerImpl.class);
		container.setSchemaContainerVersion(version);
		return container;
	}

	protected Schema createSchema(FieldSchema field) {
		Schema schema = new SchemaModel();
		schema.setName("dummySchema");
		if (field != null) {
			schema.addField(field);
		}
		return schema;
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
