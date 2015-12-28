package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NodeLanguagesVerticleTest extends AbstractRestVerticleTest {

	private static final Logger log = LoggerFactory.getLogger(NodeLanguagesVerticleTest.class);

	@Autowired
	private NodeVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	public void testDeleteLanguage() {
		Node node = content();
		int nLanguagesBefore = node.getAvailableLanguageNames().size();
		assertTrue(node.getAvailableLanguageNames().contains("en"));
		Future<GenericMessageResponse> future = getClient().deleteNode(PROJECT_NAME, node.getUuid(), "en");
		latchFor(future);

		assertSuccess(future);
		node.reload();
		expectMessageResponse("node_deleted_language", future, node.getUuid(), "en");
		assertThat(searchProvider).recordedDeleteEvents(1);
		assertFalse(node.getAvailableLanguageNames().contains("en"));
		assertEquals(nLanguagesBefore - 1, node.getAvailableLanguageNames().size());
	}

	@Test
	public void testDeleteLanguageNoPerm() {
		Node node = content();
		role().revokePermissions(node, DELETE_PERM);
		Future<GenericMessageResponse> future = getClient().deleteNode(PROJECT_NAME, node.getUuid(), "en");
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", node.getUuid());
	}
}
