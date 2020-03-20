package com.gentics.mesh.core.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.ClientSchemaStorageImpl;
import com.gentics.mesh.util.UUIDUtil;

public class UserRestTest {

	@Test
	public void testUserJson() throws JsonParseException, JsonMappingException, IOException {

		SchemaModel schema = new SchemaModelImpl();
		schema.setName("content");

		ClientSchemaStorageImpl clientSchemaStorage = new ClientSchemaStorageImpl();
		clientSchemaStorage.addSchema(schema);

		UserResponse response = new UserResponse();
		response.setCreator(new UserReference().setFirstName("Joe").setLastName("Doe").setUuid(UUIDUtil.randomUUID()));

		NodeResponse nodeResponse = new NodeResponse();
		nodeResponse.setUuid(UUIDUtil.randomUUID());
		nodeResponse.setSchema(new SchemaReferenceImpl().setName("content"));
		response.setNodeResponse(nodeResponse);
		String json = response.toJson();
		assertNotNull(json);

		UserResponse deserializedResponse = JsonUtil.readValue(json, UserResponse.class);
		assertNotNull(deserializedResponse);
		assertNotNull("The node reference field could not be found.", deserializedResponse.getNodeReference());
		assertEquals(NodeResponse.class, deserializedResponse.getNodeReference().getClass());
		assertEquals(nodeResponse.getUuid(), deserializedResponse.getNodeReference().getUuid());

		// Test again with basic reference
		NodeReference reference = new NodeReference();
		reference.setUuid(UUIDUtil.randomUUID());
		reference.setProjectName("project123");
		reference.setDisplayName("123");
		response.setNodeReference(reference);
		json = response.toJson();
		assertNotNull(json);

		deserializedResponse = JsonUtil.readValue(json, UserResponse.class);
		assertNotNull(deserializedResponse);
		assertNotNull("The node reference field could not be found.", deserializedResponse.getNodeReference());
		assertEquals(reference.getUuid(), deserializedResponse.getNodeReference().getUuid());
		assertEquals(NodeReference.class, deserializedResponse.getNodeReference().getClass());

	}
}
