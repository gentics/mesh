package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.i18n.I18NUtil;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.Tuple;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class NodeConflictEndpointTest extends AbstractMeshTest {

	private Node getTestNode() {
		return content("concorde");
	}

	private NodeUpdateRequest prepareNameFieldUpdateRequest(String nameFieldValue, String baseVersion) {
		NodeUpdateRequest request = new NodeUpdateRequest();
		request.setLanguage("en");
		// Only update the name field
		request.getFields().put("teaser", FieldUtil.createStringField(nameFieldValue));
		request.setVersion(baseVersion);
		return request;
	}

	@Test
	public void testNoConflictUpdate() {

		try (Tx trx = tx()) {

			Node node = getTestNode();
			NodeUpdateRequest request = prepareNameFieldUpdateRequest("1234", "1.0");
			NodeParametersImpl parameters = new NodeParametersImpl();
			parameters.setLanguages("en", "de");

			// Invoke an initial update on the node
			NodeResponse restNode = call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), request, parameters));
			assertThat(restNode).hasVersion("1.1");

			// Update the node again but don't change any data. Base the update on 1.0 thus a conflict check must be performed. No conflict should occur. Since
			// no fields have been altered no version should be created.
			restNode = call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), request, parameters));
			assertThat(restNode).hasVersion("1.1");

			// Update the node again but change some fields. A new version should be created (1.2)
			request.getFields().put("content", FieldUtil.createHtmlField("someValue"));
			restNode = call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), request, parameters));
			assertThat(restNode).hasVersion("1.2");

		}
	}

	@Test
	public void testConflictDetection() {

		try (Tx trx = tx()) {

			// Invoke an initial update on the node - Update Version 1.0 teaser -> 1.1
			Node node = getTestNode();
			NodeUpdateRequest request1 = prepareNameFieldUpdateRequest("1234", "1.0");
			NodeParametersImpl parameters = new NodeParametersImpl();
			parameters.setLanguages("en", "de");
			NodeResponse restNode = call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), request1, parameters));
			assertThat(restNode).hasVersion("1.1");

			// Invoke another update which just changes the title field - Update Title 1.1 -> 1.2
			NodeUpdateRequest request2 = prepareNameFieldUpdateRequest("1234", "1.1");
			request2.getFields().put("title", FieldUtil.createStringField("updatedTitle"));
			restNode = call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), request2, parameters));
			assertThat(restNode).hasVersion("1.2");

			// Update the node and change the name field. Base the update on 1.0 thus a conflict check must be performed. A conflict should be detected.
			NodeUpdateRequest request3 = prepareNameFieldUpdateRequest("1234", "1.0");
			request3.getFields().put("teaser", FieldUtil.createStringField("updatedField"));

			Throwable error = null;
			try {
				client().updateNode(PROJECT_NAME, node.getUuid(), request3, parameters).blockingGet();
				fail("The node update should fail with a conflict error");
			} catch (RuntimeException e) {
				error = e.getCause();
			}
			assertThat(error).isNotNull().isInstanceOf(MeshRestClientMessageException.class);
			MeshRestClientMessageException conflictException = ((MeshRestClientMessageException) error);

			assertThat((List) conflictException.getResponseMessage().getProperty("conflicts")).hasSize(1).containsExactly("teaser");
			assertThat(conflictException.getStatusCode()).isEqualTo(CONFLICT.code());
			assertThat(conflictException.getMessage()).isEqualTo("Error:409 in POST /api/v1/dummy/nodes/" + node.getUuid()
				+ "?lang=en,de : Conflict Info: " + I18NUtil.get(Locale.ENGLISH, "node_error_conflict_detected"));
			assertThat(conflictException.getResponseMessage().getProperty("oldVersion")).isEqualTo("1.0");
			assertThat(conflictException.getResponseMessage().getProperty("newVersion")).isEqualTo("1.2");
		}
	}

	/**
	 * Update a node and verify that only modified fields are bound to the new node version. Other fields should be referenced from the previous node version.
	 */
	@Test
	public void testDeduplicationDuringUpdate() {

		try (Tx trx = tx()) {
			updateSchema();
			NodeGraphFieldContainer origContainer = getTestNode().getLatestDraftFieldContainer(english());
			assertEquals("Concorde_english_name", origContainer.getString("teaser").getString());
			assertEquals("Concorde english title", origContainer.getString("title").getString());
			trx.success();
		}

		// First request - Update 1.0 and add basic fields and complex fields -> 1.1
		initialRequest();

		// Second request - Modify referenced elements (stringList, micronode) (1.1 -> 1.2)
		NodeUpdateRequest request = modifingRequest();

		// Third request - Update the node again and don't change anything (1.2 -> 1.2)
		repeatRequest(request);

		// Fourth request - Remove referenced elements (stringList, micronode)
		deletingRequest();
	}

	private void initialRequest() {

		Node node = getTestNode();
		String nodeUuid = tx(() -> node.getUuid());

		NodeGraphFieldContainer oldContainer = tx(() -> node.findVersion("en", project().getLatestBranch().getUuid(), "1.0"));
		NodeUpdateRequest request = prepareNameFieldUpdateRequest("1234", "1.0");
		// Add micronode / string list
		request.getFields().put("stringList", FieldUtil.createStringListField("a", "b", "c"));
		request.getFields().put("micronode", FieldUtil.createMicronodeField("vcard", Tuple.tuple("firstName", FieldUtil.createStringField(
			"test-firstname")), Tuple.tuple("lastName", FieldUtil.createStringField("test-lastname"))));
		NodeParametersImpl parameters = new NodeParametersImpl();
		parameters.setLanguages("en", "de");

		NodeResponse restNode = call(() -> client().updateNode(PROJECT_NAME, nodeUuid, request, parameters));
		assertThat(restNode).hasVersion("1.1");

		try (Tx tx = tx()) {
			assertNotNull("The old version should have a new version 1.1", oldContainer.getNextVersions().iterator().next());
			NodeGraphFieldContainer newContainer = node.findVersion("en", project().getLatestBranch().getUuid(), "1.1");
			assertEquals("The name field value of the old container version should not have been changed.", "Concorde_english_name", oldContainer
				.getString("teaser").getString());
			assertEquals("The name field value of the new container version should contain the expected value.", "1234", newContainer.getString(
				"teaser").getString());
			assertNotNull("The new container should also contain the title since basic field types are not deduplicated", newContainer.getString(
				"title"));
			assertNotNull("The container for version 0.1 should contain the title value.", oldContainer.getString("title"));
		}
	}

	private NodeUpdateRequest modifingRequest() {
		try (Tx trx = tx()) {
			Node node = getTestNode();
			NodeParametersImpl parameters = new NodeParametersImpl();
			parameters.setLanguages("en", "de");
			NodeUpdateRequest request = prepareNameFieldUpdateRequest("1234", "1.1");

			// Add micronode / string list - This time only change the order
			request.getFields().put("stringList", FieldUtil.createStringListField("b", "c", "d"));
			request.getFields().put("micronode", FieldUtil.createMicronodeField("vcard", Tuple.tuple("firstName", FieldUtil.createStringField(
				"test-updated-firstname")), Tuple.tuple("lastName", FieldUtil.createStringField("test-updated-lastname"))));

			NodeResponse restNode = call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), request, parameters));
			assertThat(restNode).hasVersion("1.2");

			NodeGraphFieldContainer createdVersion = node.findVersion(Arrays.asList("en"), project().getLatestBranch().getUuid(),
				"1.2");
			assertNotNull("The graph field container for version 1.2 could not be found.", createdVersion);
			return request;
		}

	}

	/**
	 * Assert that deduplication occurred correctly. The complex fields have not changed and should thus be referenced from the previous graph field container
	 * version.
	 * 
	 * @param request
	 */
	private void repeatRequest(NodeUpdateRequest request) {
		try (Tx trx = tx()) {
			Node node = getTestNode();
			NodeParametersImpl parameters = new NodeParametersImpl();
			parameters.setLanguages("en", "de");
			// Add another field to the request in order to invoke an update. Otherwise no update would occure and no 1.3 would be created.
			request.getFields().put("content", FieldUtil.createHtmlField("changed"));
			NodeResponse restNode = call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), request, parameters));
			assertThat(restNode).hasVersion("1.3");
		}

		try (Tx trx = tx()) {
			Node node = getTestNode();
			NodeGraphFieldContainer createdVersion = node.findVersion(Arrays.asList("en"), project().getLatestBranch().getUuid(),
				"1.3");
			assertNotNull("The graph field container for version 1.3 could not be found.", createdVersion);
			NodeGraphFieldContainer previousVersion = createdVersion.getPreviousVersion();
			assertNotNull("The graph field container for version 1.2 could not be found.", previousVersion);
			assertEquals("The previous version of 1.3 should be 1.2", "1.2", previousVersion.getVersion().toString());

			MicronodeGraphField previousMicronode = previousVersion.getMicronode("micronode");
			MicronodeGraphField nextMicronode = createdVersion.getMicronode("micronode");

			assertNotNull("Could not find the field within the previous version.", previousMicronode);
			assertNotNull("Could not find the expected field in the created version.", nextMicronode);
			assertEquals("Both fields should have the same uuid since both are referenced by the both versions.", nextMicronode.getMicronode()
				.getUuid(), previousMicronode.getMicronode().getUuid());

			StringGraphFieldList previousStringList = previousVersion.getStringList("stringList");
			StringGraphFieldList nextStringList = createdVersion.getStringList("stringList");

			assertNotNull("Could not find the field within the previous version.", previousStringList);
			assertNotNull("Could not find the expected field in the created version.", nextStringList);
			assertEquals("Both fields should have the same uuid since both are referenced by the both versions.", nextStringList.getUuid(),
				previousStringList.getUuid());

		}
	}

	private void deletingRequest() {
		try (Tx trx = tx()) {
			Node node = getTestNode();
			NodeParametersImpl parameters = new NodeParametersImpl();
			parameters.setLanguages("en", "de");
			NodeUpdateRequest request4 = prepareNameFieldUpdateRequest("1234", "1.2");
			request4.getFields().put("micronode", null);
			request4.getFields().put("stringList", null);
			NodeResponse restNode4 = call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), request4, parameters));
			assertThat(restNode4).hasVersion("1.4");
		}
		try (Tx trx = tx()) {
			Node node = getTestNode();
			NodeGraphFieldContainer createdVersion = node.findVersion("en", project().getLatestBranch().getUuid(), "1.4");
			assertNotNull("The graph field container for version 0.5 could not be found.", createdVersion);
			assertNull("The micronode should not exist in this version since we explicitly removed it.", createdVersion.getMicronode("micronode"));
			assertNull("The string list should not exist in this version since we explicitly removed it via a null update request.", createdVersion
				.getStringList("stringList"));
		}
	}

	private void updateSchema() {
		Node node = getTestNode();
		ListFieldSchema stringListFieldSchema = new ListFieldSchemaImpl();
		stringListFieldSchema.setName("stringList");
		stringListFieldSchema.setListType("string");

		MicronodeFieldSchema micronodeFieldSchema = new MicronodeFieldSchemaImpl();
		micronodeFieldSchema.setName("micronode");
		micronodeFieldSchema.setAllowedMicroSchemas("vcard");

		// Add the field schemas to the schema
		SchemaModel schema = node.getSchemaContainer().getLatestVersion().getSchema();
		schema.addField(stringListFieldSchema);
		schema.addField(micronodeFieldSchema);
		node.getSchemaContainer().getLatestVersion().setSchema(schema);
		MeshInternal.get().serverSchemaStorage().addSchema(schema);
	}

	/**
	 * Test whether a conflict with a micronode update is correctly detected and shown.
	 */
	@Test
	public void testConflictInMicronode() {
		try (Tx trx = tx()) {
			updateSchema();
			NodeGraphFieldContainer origContainer = getTestNode().getLatestDraftFieldContainer(english());
			assertEquals("Concorde_english_name", origContainer.getString("teaser").getString());
			assertEquals("Concorde english title", origContainer.getString("title").getString());
			trx.success();
		}

		// First request - Update 1.0 and add basic fields and complex fields
		initialRequest();

		// Modify 1.1 and update micronode
		Node node = getTestNode();
		try (Tx tx = tx()) {
			NodeParametersImpl parameters = new NodeParametersImpl();
			parameters.setLanguages("en", "de");
			NodeUpdateRequest request = prepareNameFieldUpdateRequest("1234", "1.1");

			// Add micronode / string list - This time only change the order
			request.getFields().put("stringList", FieldUtil.createStringListField("b", "c", "d"));
			request.getFields().put("micronode", FieldUtil.createMicronodeField("vcard", Tuple.tuple("firstName", FieldUtil.createStringField(
				"test-updated-firstname")), Tuple.tuple("lastName", FieldUtil.createStringField("test-updated-lastname"))));

			NodeResponse restNode = call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), request, parameters));
			assertThat(restNode).hasVersion("1.2");

			NodeGraphFieldContainer createdVersion = node.findVersion(Arrays.asList("en"), project().getLatestBranch().getUuid(),
				"1.2");
			assertNotNull("The graph field container for version 0.3 could not be found.", createdVersion);
		}

		// Another update request based on 1.1 which also updates the micronode - A conflict should be detected
		try (Tx tx = tx()) {
			NodeParametersImpl parameters = new NodeParametersImpl();
			parameters.setLanguages("en", "de");
			NodeUpdateRequest request = prepareNameFieldUpdateRequest("1234", "1.1");

			// Add micronode / string list - This time only change the order
			request.getFields().put("stringList", FieldUtil.createStringListField("b", "c", "d"));
			request.getFields().put("micronode", FieldUtil.createMicronodeField("vcard", Tuple.tuple("firstName", FieldUtil.createStringField(
				"test-updated-firstname")), Tuple.tuple("lastName", FieldUtil.createStringField("test-updated-lastname-also-modified"))));

			Throwable error = null;
			try {
				client().updateNode(PROJECT_NAME, node.getUuid(), request, parameters).blockingGet();
				fail("The node update should fail with a conflict error");
			} catch (RuntimeException e) {
				error = e.getCause();
			}
			assertThat(error).isNotNull().isInstanceOf(MeshRestClientMessageException.class);
			MeshRestClientMessageException conflictException = ((MeshRestClientMessageException) error);

			assertThat(((List) conflictException.getResponseMessage().getProperty("conflicts"))).hasSize(2).containsExactly("micronode.firstName",
				"micronode.lastName");
			assertThat(conflictException.getStatusCode()).isEqualTo(CONFLICT.code());
			assertThat(conflictException.getMessage()).isEqualTo("Error:409 in POST /api/v1/dummy/nodes/" + node.getUuid()
				+ "?lang=en,de : Conflict Info: " + I18NUtil.get(Locale.ENGLISH, "node_error_conflict_detected"));
			assertThat(conflictException.getResponseMessage().getProperty("oldVersion")).isEqualTo("1.1");
			assertThat(conflictException.getResponseMessage().getProperty("newVersion")).isEqualTo("1.2");
		}

	}

	@Test
	public void testBogusVersionNumber() {
		try (Tx trx = tx()) {

			Node node = getTestNode();
			NodeUpdateRequest request = prepareNameFieldUpdateRequest("1234", "42.1");
			NodeParametersImpl parameters = new NodeParametersImpl();
			parameters.setLanguages("en", "de");

			call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), request, parameters), BAD_REQUEST, "node_error_draft_not_found", "42.1",
				"en");
		}
	}
}
