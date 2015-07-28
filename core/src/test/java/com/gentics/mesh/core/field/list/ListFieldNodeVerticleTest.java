package com.gentics.mesh.core.field.list;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.commons.lang.NotImplementedException;
import org.junit.Before;
import org.junit.Test;

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
		throw new NotImplementedException();
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithNoField() {
		throw new NotImplementedException();
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
		throw new NotImplementedException();
	}

}
