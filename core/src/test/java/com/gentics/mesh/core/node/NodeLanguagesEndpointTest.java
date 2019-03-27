package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.TRACKING;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.gentics.mesh.test.context.ElasticsearchTestMode;
import org.junit.Test;

import com.syncleus.ferma.tx.Tx;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.parameter.impl.DeleteParametersImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class NodeLanguagesEndpointTest extends AbstractMeshTest {

	@Test
	public void testDeleteLanguage() {
		Node node = content();
		int nLanguagesBefore;
		try (Tx tx = tx()) {
			nLanguagesBefore = node.getAvailableLanguageNames().size();
			assertThat(node.getAvailableLanguageNames()).contains("en", "de");
		}

		// Delete the english version
		call(() -> client().deleteNode(PROJECT_NAME, contentUuid(), "en"));

		// Loading is still be possible but the node will contain no fields
		NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, contentUuid(), new NodeParametersImpl().setLanguages("en")));
		assertThat(response.getAvailableLanguages().keySet()).contains("de");
		assertThat(response.getFields()).isEmpty();

		response = call(() -> client().findNodeByUuid(PROJECT_NAME, contentUuid(), new NodeParametersImpl().setLanguages("de")));

		// Delete the english version again
		call(() -> client().deleteNode(PROJECT_NAME, contentUuid(), "en"), NOT_FOUND, "node_no_language_found", "en");

		try (Tx tx = tx()) {
			// Check the deletion
			assertThat(trackingSearchProvider()).recordedDeleteEvents(2);
			assertFalse(node.getAvailableLanguageNames().contains("en"));
			assertEquals(nLanguagesBefore - 1, node.getAvailableLanguageNames().size());
		}

		// Now delete the remaining german version
		call(() -> client().deleteNode(PROJECT_NAME, contentUuid(), "de", new DeleteParametersImpl().setRecursive(true)));
		assertThat(trackingSearchProvider()).recordedDeleteEvents(2 + 2);
		// The node was removed since the node only existed in a single branch and had no other languages
		call(() -> client().findNodeByUuid(PROJECT_NAME, contentUuid(), new VersioningParametersImpl().published()), NOT_FOUND,
				"object_not_found_for_uuid", contentUuid());

	}

	@Test
	public void testDeleteBogusLanguage() {
		call(() -> client().deleteNode(PROJECT_NAME, contentUuid(), "blub"), NOT_FOUND, "error_language_not_found", "blub");
	}

	@Test
	public void testDeleteLanguageNoPerm() {
		try (Tx tx = tx()) {
			role().revokePermissions(content(), DELETE_PERM);
			tx.success();
		}
		call(() -> client().deleteNode(PROJECT_NAME, contentUuid(), "en"), FORBIDDEN, "error_missing_perm", contentUuid(), DELETE_PERM.getRestPerm().getName());
	}
}
