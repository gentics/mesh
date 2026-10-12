package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class GraphQLEndpointFilterTest extends AbstractMeshTest {

	@Test
	public void testIsContainerFilter() {
		String queryName = "filtering/isContainer";
		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName)));
		JsonObject json = new JsonObject(response.toJson());
		JsonArray nodes = json.getJsonObject("data").getJsonObject("nodes").getJsonArray("elements");
		assertThat(nodes.size()).isGreaterThan(0);
		nodes.forEach(node -> {
			JsonObject nodeObj = (JsonObject) node;
			assertThat(nodeObj.getBoolean("isContainer")).isTrue();
		});
	}

	@Test
	public void testFilterOfNonDefaultLanguageNode() throws IOException {
		createNodeOfNonDefaultLanguage();

		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery("filtering/nodes-de-field")));
		JsonObject json = new JsonObject(response.toJson());
		assertThat(json).compliesToAssertions("filtering/nodes-de-field");
	}

	@Test
	public void testFilterOfNonDefaultLanguageNodeWithCorrectLanguage() throws IOException {
		createNodeOfNonDefaultLanguage();

		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery("filtering/nodes-de-field-correct-language")));
		JsonObject json = new JsonObject(response.toJson());
		assertThat(json).compliesToAssertions("filtering/nodes-de-field-correct-language");
	}

	@Test
	public void testOrOverBinaryAndNodeListBinaryFields() throws IOException {
		SchemaCreateRequest itemSchema = new SchemaCreateRequest();
		itemSchema.setName("form_binary_item");
		itemSchema.addField(FieldUtil.createBinaryFieldSchema("binary"));
		createSchema(itemSchema);

		SchemaCreateRequest formSchema = new SchemaCreateRequest();
		formSchema.setName("form_e2933548c2c54daea0a5f61f8c0e39e2");
		formSchema.addField(FieldUtil.createStringFieldSchema("input1"));
		formSchema.addField(FieldUtil.createBinaryFieldSchema("file1"));
		formSchema.addField(FieldUtil.createBinaryFieldSchema("file2"));
		formSchema.addField(FieldUtil.createListFieldSchema("filesssss", "node").setAllowedSchemas("form_binary_item"));
		formSchema.addField(FieldUtil.createListFieldSchema("sssss", "node").setAllowedSchemas("form_binary_item"));
		createSchema(formSchema);

		NodeCreateRequest itemRequest = new NodeCreateRequest();
		itemRequest.setSchemaName("form_binary_item");
		itemRequest.setLanguage("en");
		itemRequest.setParentNodeUuid(folderUuid());
		NodeResponse item = call(() -> client().createNode(PROJECT_NAME, itemRequest));
		uploadText(item, "binary", "image.txt");

		// The single object given to "or" is one filter, so all of its fields must match
		NodeFieldListImpl files = new NodeFieldListImpl();
		files.add(new NodeFieldListItemImpl(item.getUuid()));
		NodeCreateRequest formRequest = new NodeCreateRequest();
		formRequest.setSchemaName("form_e2933548c2c54daea0a5f61f8c0e39e2");
		formRequest.setLanguage("en");
		formRequest.setParentNodeUuid(folderUuid());
		formRequest.setFields(FieldMap.of(
			"input1", StringField.of("input"),
			"filesssss", files,
			"sssss", files
		));
		NodeResponse form = call(() -> client().createNode(PROJECT_NAME, formRequest));
		form = uploadText(form, "file1", "first.txt");
		uploadText(form, "file2", "file2.txt");

		String queryName = "filtering/nodes-or-binary-nodelist-binary";
		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName)));
		JsonObject json = new JsonObject(response.toJson());
		assertThat(json).compliesToAssertions(queryName);
		assertThat(json.getJsonObject("data").getJsonObject("nodes").getJsonArray("elements").getJsonObject(0).getString("uuid"))
			.isEqualTo(form.getUuid());
	}

	private NodeResponse uploadText(NodeResponse node, String fieldName, String fileName) {
		byte[] data = "binary content".getBytes();
		return call(() -> client().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), "en", node.getVersion(), fieldName,
			new ByteArrayInputStream(data), data.length, fileName, "text/plain"));
	}

	private void createNodeOfNonDefaultLanguage() {
		NodeCreateRequest request = new NodeCreateRequest();
		request.setSchemaName("folder");
		request.setLanguage("de");
		request.setParentNodeUuid(folderUuid());
		request.setFields(FieldMap.of(
			"name", StringField.of("deFieldTest")
		));
		client().createNode(PROJECT_NAME, request).blockingAwait();
	}
}
