package com.gentics.mesh.core.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.test.AbstractDBTest;
import com.gentics.mesh.util.FieldUtil;
import com.syncleus.ferma.FramedGraph;

public class NodeGraphFieldContainerDiffTest extends AbstractDBTest {

	protected NodeGraphFieldContainer createContainer(FieldSchema field) {
		FramedGraph graph = Database.getThreadLocalGraph();
		// 1. Setup schema
		SchemaContainer schemaContainer = graph.addFramedVertex(SchemaContainerImpl.class);
		SchemaContainerVersionImpl version = graph.addFramedVertex(SchemaContainerVersionImpl.class);
		version.setSchemaContainer(schemaContainer);

		Schema schema = new SchemaModel();
		schema.setName("dummySchema");
		schema.addField(field);
		version.setSchema(schema);

		NodeGraphFieldContainerImpl container = Database.getThreadLocalGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		container.setSchemaContainerVersion(version);
		return container;
	}

	@Test
	public void testDiff() {
		db.trx(() -> {
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString("someValue");
			NodeGraphFieldContainer containerB = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerB.createString("dummy").setString("someValue2");
			List<FieldContainerChange> list = containerA.compareTo(containerB);
			assertNotNull("The list should never be null.", list);
			assertThat(list).isNotEmpty();
			return null;
		});

	}
}
