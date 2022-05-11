package com.gentics.mesh.core.node;

import static com.gentics.mesh.MeshVersion.CURRENT_API_BASE_PATH;
import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.i18n.I18NUtil;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.list.HibStringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.Tuple;

@MeshTestSetting(testSize = FULL, startServer = true)
public class NodeConflictEndpointTest extends AbstractMeshTest {

	private HibNode getTestNode() {
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

			HibNode node = getTestNode();
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
			HibNode node = getTestNode();
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
			assertThat(conflictException.getMessage()).isEqualTo("Error:409 in POST " + CURRENT_API_BASE_PATH + "/dummy/nodes/" + node.getUuid()
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
		disableAutoPurge();

		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			updateSchema();
			HibNodeFieldContainer origContainer = contentDao.getLatestDraftFieldContainer(getTestNode(), english());
			assertEquals("Concorde_english_name", origContainer.getString("teaser").getString());
			assertEquals("Concorde english title", origContainer.getString("title").getString());
			tx.success();
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

		HibNode node = getTestNode();
		String nodeUuid = tx(() -> node.getUuid());

		HibNodeFieldContainer oldContainer = tx(() -> boot().contentDao().findVersion(node, "en", project().getLatestBranch().getUuid(), "1.0"));
		NodeUpdateRequest request = prepareNameFieldUpdateRequest("1234", "1.0");
		// Add micronode / string list
		request.getFields().put("stringList", FieldUtil.createStringListField("a", "b", "c"));
		request.getFields().put("micronode", FieldUtil.createMicronodeField("vcard", Tuple.tuple("firstName", FieldUtil.createStringField(
			"test-firstname")), Tuple.tuple("lastName", FieldUtil.createStringField("test-lastname"))));
		NodeParametersImpl parameters = new NodeParametersImpl();
		parameters.setLanguages("en", "de");

		tx(() -> {
			HibSchemaVersion latestVersion = getTestNode().getSchemaContainer().getLatestVersion();
			latestVersion.getSchema().addField(new ListFieldSchemaImpl().setListType("string").setName("stringList"));
			actions().updateSchemaVersion(latestVersion);
		});

		NodeResponse restNode = call(() -> client().updateNode(PROJECT_NAME, nodeUuid, request, parameters));
		assertThat(restNode).hasVersion("1.1");

		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			assertNotNull("The old version should have a new version 1.1", contentDao.getNextVersions(oldContainer).iterator().next());
			HibNodeFieldContainer newContainer = contentDao.findVersion(node, "en", project().getLatestBranch().getUuid(), "1.1");
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
			HibNode node = getTestNode();
			NodeParametersImpl parameters = new NodeParametersImpl();
			parameters.setLanguages("en", "de");
			NodeUpdateRequest request = prepareNameFieldUpdateRequest("1234", "1.1");

			// Add micronode / string list - This time only change the order
			request.getFields().put("stringList", FieldUtil.createStringListField("b", "c", "d"));
			request.getFields().put("micronode", FieldUtil.createMicronodeField("vcard", Tuple.tuple("firstName", FieldUtil.createStringField(
				"test-updated-firstname")), Tuple.tuple("lastName", FieldUtil.createStringField("test-updated-lastname"))));

			NodeResponse restNode = call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), request, parameters));
			assertThat(restNode).hasVersion("1.2");

			HibNodeFieldContainer createdVersion = trx.contentDao().findVersion(node, Arrays.asList("en"), project().getLatestBranch().getUuid(),
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
			HibNode node = getTestNode();
			NodeParametersImpl parameters = new NodeParametersImpl();
			parameters.setLanguages("en", "de");
			// Add another field to the request in order to invoke an update. Otherwise no update would occure and no 1.3 would be created.
			request.getFields().put("content", FieldUtil.createHtmlField("changed"));
			NodeResponse restNode = call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), request, parameters));
			assertThat(restNode).hasVersion("1.3");
		}

		try (Tx trx = tx()) {
			HibNode node = getTestNode();
			HibNodeFieldContainer createdVersion = trx.contentDao().findVersion(node, Arrays.asList("en"), project().getLatestBranch().getUuid(),
				"1.3");
			assertNotNull("The graph field container for version 1.3 could not be found.", createdVersion);
			HibNodeFieldContainer previousVersion = createdVersion.getPreviousVersion();
			assertNotNull("The graph field container for version 1.2 could not be found.", previousVersion);
			assertEquals("The previous version of 1.3 should be 1.2", "1.2", previousVersion.getVersion().toString());

			HibMicronodeField previousMicronode = previousVersion.getMicronode("micronode");
			HibMicronodeField nextMicronode = createdVersion.getMicronode("micronode");

			assertNotNull("Could not find the field within the previous version.", previousMicronode);
			assertNotNull("Could not find the expected field in the created version.", nextMicronode);
			assertEquals("Both fields should have the same uuid since both are referenced by the both versions.", nextMicronode.getMicronode()
				.getUuid(), previousMicronode.getMicronode().getUuid());

			HibStringFieldList previousStringList = previousVersion.getStringList("stringList");
			HibStringFieldList nextStringList = createdVersion.getStringList("stringList");

			assertNotNull("Could not find the field within the previous version.", previousStringList);
			assertNotNull("Could not find the expected field in the created version.", nextStringList);
			assertEquals("Both fields should have the same uuid since both are referenced by the both versions.", nextStringList.getUuid(),
				previousStringList.getUuid());

		}
	}

	private void deletingRequest() {
		try (Tx trx = tx()) {
			HibNode node = getTestNode();
			NodeParametersImpl parameters = new NodeParametersImpl();
			parameters.setLanguages("en", "de");
			NodeUpdateRequest request4 = prepareNameFieldUpdateRequest("1234", "1.2");
			request4.getFields().put("micronode", null);
			request4.getFields().put("stringList", null);
			NodeResponse restNode4 = call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), request4, parameters));
			assertThat(restNode4).hasVersion("1.4");
		}
		try (Tx trx = tx()) {
			HibNode node = getTestNode();
			HibNodeFieldContainer createdVersion = trx.contentDao().findVersion(node, "en", project().getLatestBranch().getUuid(), "1.4");
			assertNotNull("The graph field container for version 0.5 could not be found.", createdVersion);
			assertNull("The micronode should not exist in this version since we explicitly removed it.", createdVersion.getMicronode("micronode"));
			assertNull("The string list should not exist in this version since we explicitly removed it via a null update request.", createdVersion
				.getStringList("stringList"));
		}
	}

	private void updateSchema() {
		HibNode node = getTestNode();
		ListFieldSchema stringListFieldSchema = new ListFieldSchemaImpl();
		stringListFieldSchema.setName("stringList");
		stringListFieldSchema.setListType("string");

		MicronodeFieldSchema micronodeFieldSchema = new MicronodeFieldSchemaImpl();
		micronodeFieldSchema.setName("micronode");
		micronodeFieldSchema.setAllowedMicroSchemas("vcard");

		// Add the field schemas to the schema
		SchemaVersionModel schema = node.getSchemaContainer().getLatestVersion().getSchema();
		schema.addField(stringListFieldSchema);
		schema.addField(micronodeFieldSchema);
		node.getSchemaContainer().getLatestVersion().setSchema(schema);
		mesh().serverSchemaStorage().addSchema(schema);
	}

	/**
	 * Test whether a conflict with a micronode update is correctly detected and shown.
	 */
	@Test
	public void testConflictInMicronode() {
		disableAutoPurge();

		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			updateSchema();
			HibNodeFieldContainer origContainer = contentDao.getLatestDraftFieldContainer(getTestNode(), english());
			assertEquals("Concorde_english_name", origContainer.getString("teaser").getString());
			assertEquals("Concorde english title", origContainer.getString("title").getString());
			tx.success();
		}

		// First request - Update 1.0 and add basic fields and complex fields
		initialRequest();

		// Modify 1.1 and update micronode
		HibNode node = getTestNode();
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

			HibNodeFieldContainer createdVersion = tx.contentDao().findVersion(node, Arrays.asList("en"), project().getLatestBranch().getUuid(),
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
			assertThat(conflictException.getMessage()).isEqualTo("Error:409 in POST " + CURRENT_API_BASE_PATH + "/dummy/nodes/" + node.getUuid()
				+ "?lang=en,de : Conflict Info: " + I18NUtil.get(Locale.ENGLISH, "node_error_conflict_detected"));
			assertThat(conflictException.getResponseMessage().getProperty("oldVersion")).isEqualTo("1.1");
			assertThat(conflictException.getResponseMessage().getProperty("newVersion")).isEqualTo("1.2");
		}

	}

	@Test
	public void testBogusVersionNumber() {
		try (Tx trx = tx()) {

			HibNode node = getTestNode();
			NodeUpdateRequest request = prepareNameFieldUpdateRequest("1234", "42.1");
			NodeParametersImpl parameters = new NodeParametersImpl();
			parameters.setLanguages("en", "de");

			call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), request, parameters), BAD_REQUEST, "node_error_draft_not_found", "42.1",
				"en");
		}
	}
}
