package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.service.I18NUtil;
import com.gentics.mesh.core.rest.error.NodeVersionConflictException;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.VersionReference;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.query.impl.NodeRequestParameter;
import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.util.FieldUtil;

import io.vertx.core.Future;

public class NodeConflictVerticleTest extends AbstractRestVerticleTest {

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
		NodeGraphFieldContainer origContainer = node.getGraphFieldContainer(english());
		assertEquals("Concorde_english_name", origContainer.getString("name").getString());
		assertEquals("Concorde english title", origContainer.getString("title").getString());
		return node;
	}

	private NodeUpdateRequest prepareRequest(String nameFieldValue, String baseVersion) {
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

		Node node = getTestNode();
		NodeUpdateRequest request = prepareRequest("1234", "0.1");
		NodeRequestParameter parameters = new NodeRequestParameter();
		parameters.setLanguages("en", "de");

		// Invoke an initial update on the node
		NodeResponse restNode = call(() -> getClient().updateNode(PROJECT_NAME, node.getUuid(), request, parameters));
		assertThat(restNode).hasVersion("0.2");

		// Update The node again but don't change any data. Base the update on 0.1 thus a conflict check must be performed. No conflict should occur.
		restNode = call(() -> getClient().updateNode(PROJECT_NAME, node.getUuid(), request, parameters));
		assertThat(restNode).hasVersion("0.3");

	}

	@Test
	public void testConflictDetection() {

		// Invoke an initial update on the node - Update Version 0.1 Name  -> 0.2
		Node node = getTestNode();
		NodeUpdateRequest request1 = prepareRequest("1234", "0.1");
		NodeRequestParameter parameters = new NodeRequestParameter();
		parameters.setLanguages("en", "de");
		NodeResponse restNode = call(() -> getClient().updateNode(PROJECT_NAME, node.getUuid(), request1, parameters));
		assertThat(restNode).hasVersion("0.2");

		// Invoke another update which just changes the title field - Update Title 0.2 -> 0.3 
		NodeUpdateRequest request2 = prepareRequest("1234", "0.2");
		request2.getFields().put("title", FieldUtil.createStringField("updatedTitle"));
		restNode = call(() -> getClient().updateNode(PROJECT_NAME, node.getUuid(), request2, parameters));
		assertThat(restNode).hasVersion("0.3");

		// Update the node and change the name field. Base the update on 0.1 thus a conflict check must be performed. A conflict should be detected.
		NodeUpdateRequest request3 = prepareRequest("1234", "0.1");
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

	/**
	 * Test whether a conflict with a micronode update is correctly detected and shown.
	 */
	@Test
	public void testConflictInMicronode() {
		fail("implement me");
	}

	@Test
	public void testBogusVersionNumber() {
		Node node = getTestNode();
		NodeUpdateRequest request = prepareRequest("1234", "42.1");
		NodeRequestParameter parameters = new NodeRequestParameter();
		parameters.setLanguages("en", "de");

		call(() -> getClient().updateNode(PROJECT_NAME, node.getUuid(), request, parameters), BAD_REQUEST, "node_error_draft_not_found", "42.1",
				"en");

	}
}
