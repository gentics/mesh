package com.gentics.mesh.core.field.list;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.NodeFieldList;
import com.gentics.mesh.core.field.AbstractFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItem;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;

public class ListFieldNodeVerticleTest extends AbstractFieldNodeVerticleTest {

	@Before
	public void updateSchema() throws IOException {
		Schema schema = schemaContainer("folder").getSchema();
		ListFieldSchema listFieldSchema = new ListFieldSchemaImpl();
		listFieldSchema.setName("listField");
		listFieldSchema.setLabel("Some label");
		listFieldSchema.setListType("node");
		schema.addField(listFieldSchema);
		schemaContainer("folder").setSchema(schema);
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		Node node = folder("news");
		NodeFieldListImpl list = new NodeFieldListImpl();
		list.add(new NodeFieldListItem(node.getUuid()));
		NodeResponse response = updateNode("listField", list);
		NodeFieldListImpl field = response.getField("listField");
		assertEquals(1, field.getList().size());

		Node node2 = folder("deals");
		list = new NodeFieldListImpl();
		list.add(new NodeFieldListItem(node2.getUuid()));
		response = updateNode("listField", list);
		field = response.getField("listField");
		assertEquals(1, field.getList().size());
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		NodeFieldListImpl listField = new NodeFieldListImpl();
		NodeFieldListItem item = new NodeFieldListItem().setUuid(folder("news").getUuid());
		listField.add(item);
		NodeResponse response = createNode("listField", listField);
		NodeFieldListImpl listFromResponse = response.getField("listField");
		assertEquals(1, listFromResponse.getList().size());
	}

	@Test
	@Override
	public void testReadNodeWithExitingField() {
		Node node = folder("2015");

		NodeFieldContainer container = node.getFieldContainer(english());
		NodeFieldList nodeList = container.createNodeList("listField");
		nodeList.createNode("1", folder("news"));

		NodeResponse response = readNode(node);
		NodeFieldListImpl deserializedListField = response.getField("listField", NodeFieldListImpl.class);
		assertNotNull(deserializedListField);
		assertEquals(1, deserializedListField.getList().size());
	}

}
