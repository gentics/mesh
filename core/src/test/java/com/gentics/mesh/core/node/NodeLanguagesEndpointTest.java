package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.call;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class NodeLanguagesEndpointTest extends AbstractMeshTest {

	@Test
	public void testDeleteLanguage() {
		try (NoTx noTx = db().noTx()) {
			Node node = content();
			String uuid = node.getUuid();
			int nLanguagesBefore = node.getAvailableLanguageNames()
					.size();
			assertThat(node.getAvailableLanguageNames()).contains("en", "de");

			// Delete the english version
			call(() -> client().deleteNode(PROJECT_NAME, node.getUuid(), "en"));

			// Loading is still be possible but the node will contain no fields
			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new NodeParametersImpl().setLanguages("en")));
			assertThat(response.getAvailableLanguages()).contains("de");
			assertThat(response.getFields()).isEmpty();

			response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new NodeParametersImpl().setLanguages("de")));

			// Delete the english version again
			call(() -> client().deleteNode(PROJECT_NAME, node.getUuid(), "en"), NOT_FOUND, "node_no_language_found", "en");

			// Check the deletion
			node.reload();
			assertThat(dummySearchProvider()).recordedDeleteEvents(2);
			assertFalse(node.getAvailableLanguageNames()
					.contains("en"));
			assertEquals(nLanguagesBefore - 1, node.getAvailableLanguageNames()
					.size());

			// Now delete the remaining german version
			call(() -> client().deleteNode(PROJECT_NAME, node.getUuid(), "de"));
			assertThat(dummySearchProvider()).recordedDeleteEvents(2 + 2);
			call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().published()), NOT_FOUND,
					"node_error_published_not_found_for_uuid_release_language", uuid, "en", release().getUuid());
		}

	}

	@Test
	public void testDeleteBogusLanguage() {
		try (NoTx noTx = db().noTx()) {
			Node node = content();
			call(() -> client().deleteNode(PROJECT_NAME, node.getUuid(), "blub"), NOT_FOUND, "error_language_not_found", "blub");
		}
	}

	@Test
	public void testDeleteLanguageNoPerm() {
		try (NoTx noTx = db().noTx()) {
			Node node = content();
			role().revokePermissions(node, DELETE_PERM);
			call(() -> client().deleteNode(PROJECT_NAME, node.getUuid(), "en"), FORBIDDEN, "error_missing_perm", node.getUuid());
		}
	}
}
