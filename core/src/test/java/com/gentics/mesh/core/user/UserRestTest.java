package com.gentics.mesh.core.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.core.rest.user.NodeReferenceImpl;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.ClientSchemaStorage;
import com.gentics.mesh.util.UUIDUtil;

public class UserRestTest {

	@Test
	public void testUserJson() throws JsonParseException, JsonMappingException, IOException {

		Schema schema = new SchemaModel();
		schema.setName("content");

		ClientSchemaStorage clientSchemaStorage = new ClientSchemaStorage();
		clientSchemaStorage.addSchema(schema);

		UserResponse response = new UserResponse();
		response.setCreator(new UserReference().setName("Joe").setUuid(UUIDUtil.randomUUID()));

		NodeResponse nodeResponse = new NodeResponse();
		nodeResponse.setUuid(UUIDUtil.randomUUID());
		nodeResponse.setSchema(new SchemaReference().setName("content"));
		response.setNodeReference(nodeResponse);
		String json = JsonUtil.toJson(response);
		assertNotNull(json);

		UserResponse deserializedResponse = JsonUtil.readNode(json, UserResponse.class, clientSchemaStorage);
		assertNotNull(deserializedResponse);
		assertNotNull("The node reference field could not be found.", deserializedResponse.getNodeReference());
		assertEquals(NodeResponse.class, deserializedResponse.getNodeReference().getClass());
		assertEquals(nodeResponse.getUuid(), deserializedResponse.getNodeReference().getUuid());

		// Test again with basic reference
		NodeReferenceImpl reference = new NodeReferenceImpl();
		reference.setUuid(UUIDUtil.randomUUID());
		reference.setProjectName("project123");
		reference.setDisplayName("123");
		response.setNodeReference(reference);
		json = JsonUtil.toJson(response);
		assertNotNull(json);

		deserializedResponse = JsonUtil.readNode(json, UserResponse.class, clientSchemaStorage);
		assertNotNull(deserializedResponse);
		assertNotNull("The node reference field could not be found.", deserializedResponse.getNodeReference());
		assertEquals(reference.getUuid(), deserializedResponse.getNodeReference().getUuid());
		assertEquals(NodeReferenceImpl.class, deserializedResponse.getNodeReference().getClass());

	}
}
