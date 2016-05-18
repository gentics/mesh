package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.service.I18NUtil;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.error.NodeVersionConflictException;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.VersionReference;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.query.impl.NodeRequestParameter;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;
import com.gentics.mesh.util.FieldUtil;
import com.gentics.mesh.util.Tuple;

import io.vertx.core.Future;

public class NodeConflictVerticleTest extends AbstractIsolatedRestVerticleTest {

	@Autowired
	private NodeVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	private Node getTestNode() {
		Node node = content("concorde");
		return node;
	}

	private NodeUpdateRequest prepareNameFieldUpdateRequest(String nameFieldValue, String baseVersion) {
		NodeUpdateRequest request = new NodeUpdateRequest();
		SchemaReference schemaReference = new SchemaReference();
		schemaReference.setName("content");
		schemaReference.setUuid(schemaContainer("content").getUuid());
		request.setSchema(schemaReference);
		request.setLanguage("en");
		// Only update the name field
		request.getFields().put("name", FieldUtil.createStringField(nameFieldValue));
		VersionReference reference = new VersionReference();
		reference.setNumber(baseVersion);
		request.setVersion(reference);
		return request;
	}

	@Test
	public void testNoConflictUpdate() {

		try (Trx trx = db.trx()) {

			Node node = getTestNode();
			NodeUpdateRequest request = prepareNameFieldUpdateRequest("1234", "0.1");
			NodeRequestParameter parameters = new NodeRequestParameter();
			parameters.setLanguages("en", "de");

			// Invoke an initial update on the node
			NodeResponse restNode = call(() -> getClient().updateNode(PROJECT_NAME, node.getUuid(), request, parameters));
			assertThat(restNode).hasVersion("0.2");

			// Update The node again but don't change any data. Base the update on 0.1 thus a conflict check must be performed. No conflict should occur.
			restNode = call(() -> getClient().updateNode(PROJECT_NAME, node.getUuid(), request, parameters));
			assertThat(restNode).hasVersion("0.3");

		}
	}

	@Test
	public void testConflictDetection() {

		try (Trx trx = db.trx()) {

			// Invoke an initial update on the node - Update Version 0.1 Name  -> 0.2
			Node node = getTestNode();
			NodeUpdateRequest request1 = prepareNameFieldUpdateRequest("1234", "0.1");
			NodeRequestParameter parameters = new NodeRequestParameter();
			parameters.setLanguages("en", "de");
			NodeResponse restNode = call(() -> getClient().updateNode(PROJECT_NAME, node.getUuid(), request1, parameters));
			assertThat(restNode).hasVersion("0.2");

			// Invoke another update which just changes the title field - Update Title 0.2 -> 0.3 
			NodeUpdateRequest request2 = prepareNameFieldUpdateRequest("1234", "0.2");
			request2.getFields().put("title", FieldUtil.createStringField("updatedTitle"));
			restNode = call(() -> getClient().updateNode(PROJECT_NAME, node.getUuid(), request2, parameters));
			assertThat(restNode).hasVersion("0.3");

			// Update the node and change the name field. Base the update on 0.1 thus a conflict check must be performed. A conflict should be detected.
			NodeUpdateRequest request3 = prepareNameFieldUpdateRequest("1234", "0.1");
			request3.getFields().put("name", FieldUtil.createStringField("updatedField"));
			Future<NodeResponse> future = getClient().updateNode(PROJECT_NAME, node.getUuid(), request3, parameters);
			latchFor(future);
			assertTrue("The node update should fail with a conflict error", future.failed());
			Throwable error = future.cause();
			assertThat(error).isNotNull().isInstanceOf(NodeVersionConflictException.class);
			NodeVersionConflictException conflictException = ((NodeVersionConflictException) error);

			assertThat(conflictException.getConflicts()).hasSize(1).containsExactly("name");
			assertThat(conflictException.getStatus()).isEqualTo(CONFLICT);
			assertThat(conflictException.getMessage()).isEqualTo(I18NUtil.get(Locale.ENGLISH, "node_error_conflict_detected"));
			assertThat(conflictException.getOldVersion()).isEqualTo("0.1");
			assertThat(conflictException.getNewVersion()).isEqualTo("0.3");
		}
	}

	/**
	 * Update a node and verify that only modified fields are bound to the new node version. Other fields should be referenced from the previous node version.
	 */
	@Test
	public void testDeduplicationDuringUpdate() {

		try (Trx trx = db.trx()) {
			updateSchema();
			NodeGraphFieldContainer origContainer = getTestNode().getGraphFieldContainer(english());
			assertEquals("Concorde_english_name", origContainer.getString("name").getString());
			assertEquals("Concorde english title", origContainer.getString("title").getString());
			trx.success();
		}

		// First request - Update 0.1 and add basic fields and complex fields
		initialRequest();

		// Second request - Modify referenced elements (stringList, micronode)
		NodeUpdateRequest request = modifingRequest();

		// Third request - Update the node again and don't change anything
		repeatRequest(request);

		// Fourth request - Remove referenced elements (stringList, micronode)
		deletingRequest();
	}

	private void initialRequest() {

		try (Trx trx = db.trx()) {
			Node node = getTestNode();
			NodeGraphFieldContainer oldContainer = node.findNextMatchingFieldContainer(Arrays.asList("en"), project().getLatestRelease().getUuid(),
					"0.1");

			NodeUpdateRequest request = prepareNameFieldUpdateRequest("1234", "0.1");

			//Add micronode / string list
			request.getFields().put("stringList", FieldUtil.createStringListField("a", "b", "c"));
			request.getFields().put("micronode",
					FieldUtil.createMicronodeField("vcard", Tuple.tuple("firstName", FieldUtil.createStringField("test-firstname")),
							Tuple.tuple("lastName", FieldUtil.createStringField("test-lastname"))));

			NodeRequestParameter parameters = new NodeRequestParameter();
			parameters.setLanguages("en", "de");
			NodeResponse restNode = call(() -> getClient().updateNode(PROJECT_NAME, node.getUuid(), request, parameters));
			assertThat(restNode).hasVersion("0.2");

			oldContainer.reload();
			assertNotNull("The old version should have a new version 0.2", oldContainer.getNextVersion());
			node.reload();
			NodeGraphFieldContainer newContainer = node.findNextMatchingFieldContainer(Arrays.asList("en"), project().getLatestRelease().getUuid(),
					"0.2");
			assertEquals("The name field value of the old container version should not have been changed.", "Concorde_english_name",
					oldContainer.getString("name").getString());
			assertEquals("The name field value of the new container version should contain the expected value.", "1234",
					newContainer.getString("name").getString());
			assertNotNull("The new container should also contain the title since basic field types are not deduplicated",
					newContainer.getString("title"));
			assertNotNull("The container for version 0.1 should contain the title value.", oldContainer.getString("title"));
		}

	}

	private NodeUpdateRequest modifingRequest() {
		try (Trx trx = db.trx()) {
			Node node = getTestNode();
			NodeRequestParameter parameters = new NodeRequestParameter();
			parameters.setLanguages("en", "de");
			NodeUpdateRequest request = prepareNameFieldUpdateRequest("1234", "0.2");

			//Add micronode / string list - This time only change the order
			request.getFields().put("stringList", FieldUtil.createStringListField("b", "c", "d"));
			request.getFields().put("micronode",
					FieldUtil.createMicronodeField("vcard", Tuple.tuple("firstName", FieldUtil.createStringField("test-updated-firstname")),
							Tuple.tuple("lastName", FieldUtil.createStringField("test-updated-lastname"))));

			NodeResponse restNode = call(() -> getClient().updateNode(PROJECT_NAME, node.getUuid(), request, parameters));
			assertThat(restNode).hasVersion("0.3");

			node.reload();
			NodeGraphFieldContainer createdVersion = node.findNextMatchingFieldContainer(Arrays.asList("en"), project().getLatestRelease().getUuid(),
					"0.3");
			assertNotNull("The graph field container for version 0.3 could not be found.", createdVersion);
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
		System.out.println("Dedup request");
		try (Trx trx = db.trx()) {
			Node node = getTestNode();
			NodeRequestParameter parameters = new NodeRequestParameter();
			parameters.setLanguages("en", "de");
			NodeResponse restNode = call(() -> getClient().updateNode(PROJECT_NAME, node.getUuid(), request, parameters));
			assertThat(restNode).hasVersion("0.4");
		}

		try (Trx trx = db.trx()) {
			Node node = getTestNode();
			NodeGraphFieldContainer createdVersion = node.findNextMatchingFieldContainer(Arrays.asList("en"), project().getLatestRelease().getUuid(),
					"0.4");
			assertNotNull("The graph field container for version 0.4 could not be found.", createdVersion);
			NodeGraphFieldContainer previousVersion = createdVersion.getPreviousVersion();
			assertNotNull("The graph field container for version 0.3 could not be found.", previousVersion);
			assertEquals("The previous version of 0.4 should be 0.3", "0.3", previousVersion.getVersion().toString());

			MicronodeGraphField previousMicronode = previousVersion.getMicronode("micronode");
			MicronodeGraphField nextMicronode = createdVersion.getMicronode("micronode");

			assertNotNull("Could not find the field within the previous version.", previousMicronode);
			assertNotNull("Could not find the expected field in the created version.", nextMicronode);
			assertEquals("Both fields should have the same uuid since both are referenced by the both versions.",
					nextMicronode.getMicronode().getUuid(), previousMicronode.getMicronode().getUuid());

			StringGraphFieldList previousStringList = previousVersion.getStringList("stringList");
			StringGraphFieldList nextStringList = createdVersion.getStringList("stringList");

			assertNotNull("Could not find the field within the previous version.", previousStringList);
			assertNotNull("Could not find the expected field in the created version.", nextStringList);
			assertEquals("Both fields should have the same uuid since both are referenced by the both versions.", nextStringList.getUuid(),
					previousStringList.getUuid());

		}
	}

	private void deletingRequest() {
		try (Trx trx = db.trx()) {
			Node node = getTestNode();
			NodeRequestParameter parameters = new NodeRequestParameter();
			parameters.setLanguages("en", "de");
			NodeUpdateRequest request4 = prepareNameFieldUpdateRequest("1234", "0.3");
			request4.getFields().put("micronode", null);
			request4.getFields().put("stringList", null);
			NodeResponse restNode4 = call(() -> getClient().updateNode(PROJECT_NAME, node.getUuid(), request4, parameters));
			assertThat(restNode4).hasVersion("0.5");
		}
		try (Trx trx = db.trx()) {
			Node node = getTestNode();
			NodeGraphFieldContainer createdVersion = node.findNextMatchingFieldContainer(Arrays.asList("en"), project().getLatestRelease().getUuid(),
					"0.5");
			assertNotNull("The graph field container for version 0.5 could not be found.", createdVersion);
			assertNull("The micronode should not exist in this version since we explicitly removed it.", createdVersion.getMicronode("micronode"));
			assertNull("The string list should not exist in this version since we explicitly removed it.",
					createdVersion.getStringList("stringList"));
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
		Schema schema = node.getSchemaContainer().getLatestVersion().getSchema();
		schema.addField(stringListFieldSchema);
		schema.addField(micronodeFieldSchema);
		node.getSchemaContainer().getLatestVersion().setSchema(schema);
		ServerSchemaStorage.getInstance().addSchema(schema);
	}

	/**
	 * Test whether a conflict with a micronode update is correctly detected and shown.
	 */
	@Test
	public void testConflictInMicronode() {
		fail("implement me");
	}

	@Test
	public void testBogusVersionNumber() {
		try (Trx trx = db.trx()) {

			Node node = getTestNode();
			NodeUpdateRequest request = prepareNameFieldUpdateRequest("1234", "42.1");
			NodeRequestParameter parameters = new NodeRequestParameter();
			parameters.setLanguages("en", "de");

			call(() -> getClient().updateNode(PROJECT_NAME, node.getUuid(), request, parameters), BAD_REQUEST, "node_error_draft_not_found", "42.1",
					"en");
		}
	}
}
