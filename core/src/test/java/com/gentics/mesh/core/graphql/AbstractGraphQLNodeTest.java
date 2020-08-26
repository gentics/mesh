package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;

public abstract class AbstractGraphQLNodeTest extends AbstractMeshTest {

	public static final String NODE_1A_UUID = "b14469c742544efab294e0331232b9e0";
	public static final String NODE_1B_UUID = "02e3982b6ad14c1fa4a0b5998a02ff92";
	public static final String NODE_1C_UUID = "13c5981c51994d54998dd88b0c484720";
	public static final String SCHEMA_NAME = "test";

	protected void setupContents(boolean publish) {

		List<String> testNodeUuids = new ArrayList<>();

		// Delete all other tags
		tx(tx -> {
			for (HibTag tag : tags().values()) {
				if (tag.getName().equals("blue")) {
					continue;
				} else {
					tx.data().tagDao().delete(tag, createBulkContext());
				}
			}
		});

		String baseNodeUuid = tx(() -> project().getBaseNode().getUuid());

		SchemaCreateRequest schemaRequest = new SchemaCreateRequest();
		schemaRequest.setName(SCHEMA_NAME);
		schemaRequest.setContainer(true);
		schemaRequest.setSegmentField("name");
		schemaRequest.addField(FieldUtil.createStringFieldSchema("name"));
		schemaRequest.addField(FieldUtil.createStringFieldSchema("extra"));
		schemaRequest.addField(FieldUtil.createNodeFieldSchema("node"));
		schemaRequest.addField(FieldUtil.createListFieldSchema("nodeList").setListType("node"));
		// TODO test micronodes

		SchemaResponse schemaResponse = call(() -> client().createSchema(schemaRequest));
		call(() -> client().assignSchemaToProject(PROJECT_NAME, schemaResponse.getUuid()));

		// level 1 - A [en]
		NodeCreateRequest level1ANodeCreateRequest = new NodeCreateRequest();
		level1ANodeCreateRequest.setParentNodeUuid(baseNodeUuid);
		level1ANodeCreateRequest.setSchemaName(SCHEMA_NAME);
		level1ANodeCreateRequest.setLanguage("en");
		level1ANodeCreateRequest.getFields().put("name", FieldUtil.createStringField("level1A"));
		call(() -> client().createNode(NODE_1A_UUID, PROJECT_NAME, level1ANodeCreateRequest));
		testNodeUuids.add(NODE_1A_UUID);

		// level 1 - B [en]
		NodeCreateRequest level1BNodeCreateRequest = new NodeCreateRequest();
		level1BNodeCreateRequest.setParentNodeUuid(baseNodeUuid);
		level1BNodeCreateRequest.setSchemaName(SCHEMA_NAME);
		level1BNodeCreateRequest.setLanguage("en");
		level1BNodeCreateRequest.getFields().put("name", FieldUtil.createStringField("level1B"));
		level1BNodeCreateRequest.getFields().put("node", FieldUtil.createNodeField(NODE_1A_UUID));
		level1BNodeCreateRequest.getFields().put("nodeList", FieldUtil.createNodeListField(NODE_1A_UUID, NODE_1A_UUID));
		call(() -> client().createNode(NODE_1B_UUID, PROJECT_NAME, level1BNodeCreateRequest));
		testNodeUuids.add(NODE_1B_UUID);

		// level 1 - C [en]
		NodeCreateRequest level1CNodeCreateRequest = new NodeCreateRequest();
		level1CNodeCreateRequest.setParentNodeUuid(baseNodeUuid);
		level1CNodeCreateRequest.setSchemaName(SCHEMA_NAME);
		level1CNodeCreateRequest.setLanguage("en");
		level1CNodeCreateRequest.getFields().put("name", FieldUtil.createStringField("level1C"));
		level1CNodeCreateRequest.getFields().put("node", FieldUtil.createNodeField(NODE_1B_UUID));
		level1CNodeCreateRequest.getFields().put("nodeList", FieldUtil.createNodeListField(NODE_1B_UUID, NODE_1B_UUID));
		call(() -> client().createNode(NODE_1C_UUID, PROJECT_NAME, level1CNodeCreateRequest));
		testNodeUuids.add(NODE_1C_UUID);

		// level 2 [en]
		for (int i = 0; i < 10; i++) {
			NodeCreateRequest subNodeCreateRequest = new NodeCreateRequest();
			subNodeCreateRequest.setParentNodeUuid(NODE_1B_UUID);
			subNodeCreateRequest.setSchemaName(SCHEMA_NAME);
			subNodeCreateRequest.setLanguage("en");
			subNodeCreateRequest.getFields().put("name", FieldUtil.createStringField("level2-" + i));
			subNodeCreateRequest.getFields().put("node", FieldUtil.createNodeField(NODE_1A_UUID));
			subNodeCreateRequest.getFields().put("nodeList", FieldUtil.createNodeListField(NODE_1A_UUID, NODE_1B_UUID));
			NodeResponse level2NodeResponse = call(() -> client().createNode(PROJECT_NAME, subNodeCreateRequest));
			testNodeUuids.add(level2NodeResponse.getUuid());
		}

		// Add node reference to user
		UserResponse user = call(() -> client().me());
		NodeReference nodeRef = new NodeReference();
		nodeRef.setProjectName(PROJECT_NAME);
		nodeRef.setUuid(NODE_1B_UUID);
		call(() -> client().updateUser(user.getUuid(), new UserUpdateRequest().setNodeReference(nodeRef)));

		// Tag nodes
		String tagUuid = tx(() -> tag("blue").getUuid());
		call(() -> client().addTagToNode(PROJECT_NAME, NODE_1B_UUID, tagUuid));
		call(() -> client().addTagToNode(PROJECT_NAME, NODE_1A_UUID, tagUuid));

		// Apply publish flag
		if (publish) {
			call(() -> client().publishNode(PROJECT_NAME, baseNodeUuid,
				new PublishParametersImpl().setRecursive(true)));
		} else {
			call(() -> client().takeNodeOffline(PROJECT_NAME, baseNodeUuid,
				new PublishParametersImpl().setRecursive(true)));
		}

		// Create drafts for tested data
		for (String uuid : testNodeUuids) {
			NodeUpdateRequest subNodeUpdateRequest = new NodeUpdateRequest();
			subNodeUpdateRequest.setLanguage("en");
			subNodeUpdateRequest.getFields().putString("extra", "DRAFT");
			call(() -> client().updateNode(PROJECT_NAME, uuid, subNodeUpdateRequest));
		}

		NodeUpdateRequest rootNodeUpdateRequest = new NodeUpdateRequest();
		rootNodeUpdateRequest.setLanguage("en");
		rootNodeUpdateRequest.getFields().putString("name", "root");
		call(() -> client().updateNode(PROJECT_NAME, baseNodeUuid, rootNodeUpdateRequest));
	}

}
