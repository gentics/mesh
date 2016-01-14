package com.gentics.mesh.core.field.select;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.StringGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.nesting.SelectGraphField;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.field.bool.AbstractBasicDBTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.SelectFieldImpl;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SelectFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.SelectFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;

public class SelectGraphFieldTest extends AbstractBasicDBTest {

	@Autowired
	private ServerSchemaStorage schemaStorage;

	@Test
	@Ignore("Not yet implemented")
	public void testSelectFieldTransformation() throws Exception {
		Node node = folder("2015");
		Schema schema = node.getSchema();
		SelectFieldSchema selectFieldSchema = new SelectFieldSchemaImpl();
		selectFieldSchema.setName("selectField");
		selectFieldSchema.setRequired(true);
		schema.addField(selectFieldSchema);
		node.getSchemaContainer().setSchema(schema);

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());

		SelectGraphField<?> selectField = container.createSelect("selectField");
		assertNotNull(selectField);

		String json = getJson(node);
		assertTrue(json.indexOf("selectField") > 1);
		assertNotNull(json);
		NodeResponse response = JsonUtil.readNode(json, NodeResponse.class, schemaStorage);
		assertNotNull(response);
		SelectFieldImpl deserializedSelectField = response.getField("selectField");
	}

	@Test
	@Ignore("Not yet implemented")
	public void testStringSelection() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);

		SelectGraphField<StringGraphField> field = container.createSelect("dummySelect");
		field.addOption(new StringGraphFieldImpl("test", null));
		assertEquals(1, field.getOptions().size());
	}

	@Test
	@Ignore("Not yet implemented")
	public void testNodeSelection() {
		fail("Not yet implemented");
	}

	@Test
	@Ignore("Not yet implemented")
	public void testNumberSelection() {
		fail("Not yet implemented");
	}

	@Test
	@Ignore("Not yet implemented")
	public void testBooleanSelection() {
		fail("Not yet implemented");
	}
}
