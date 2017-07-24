package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.PUBLISH_PERM;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.Test;

import com.gentics.ferma.Tx;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class NodeTakeOfflineEndpointTest extends AbstractMeshTest {

	@Test
	public void testTakeNodeOffline() {

		String nodeUuid = db().tx(() -> {
			Node node = folder("products");
			String uuid = node.getUuid();

			call(() -> client().takeNodeOffline(PROJECT_NAME, project().getBaseNode().getUuid(), new PublishParametersImpl().setRecursive(true)));

			assertThat(call(() -> client().publishNode(PROJECT_NAME, uuid))).as("Publish Status").isPublished("en").isPublished("de");
			return uuid;
		});

		// assert that the containers have both webrootpath properties set
		try (Tx tx1 = tx()) {
			for (String language : Arrays.asList("en", "de")) {
				for (String property : Arrays.asList(NodeGraphFieldContainerImpl.WEBROOT_PROPERTY_KEY,
						NodeGraphFieldContainerImpl.PUBLISHED_WEBROOT_PROPERTY_KEY)) {
					assertThat(folder("products").getGraphFieldContainer(language).getProperty(property, String.class))
							.as("Property " + property + " for " + language).isNotNull();
				}
			}
		}

		call(() -> client().takeNodeOffline(PROJECT_NAME, nodeUuid, new PublishParametersImpl().setRecursive(true)));
		assertThat(call(() -> client().getNodePublishStatus(PROJECT_NAME, nodeUuid))).as("Publish status").isNotPublished("en").isNotPublished("de");

		// assert that the containers have only the draft webrootpath properties set
		try (Tx tx2 = tx()) {
			for (String language : Arrays.asList("en", "de")) {
				String property = NodeGraphFieldContainerImpl.WEBROOT_PROPERTY_KEY;
				assertThat(folder("products").getGraphFieldContainer(language).getProperty(property, String.class))
						.as("Property " + property + " for " + language).isNotNull();

				property = NodeGraphFieldContainerImpl.PUBLISHED_WEBROOT_PROPERTY_KEY;
				assertThat(folder("products").getGraphFieldContainer(language).getProperty(property, String.class))
						.as("Property " + property + " for " + language).isNull();

			}
		}

	}

	@Test
	public void testTakeNodeLanguageOffline() {
		try (Tx tx = tx()) {
			Node node = folder("products");
			String nodeUuid = node.getUuid();

			// 1. Take all nodes offline
			call(() -> client().takeNodeOffline(PROJECT_NAME, project().getBaseNode().getUuid(), new PublishParametersImpl().setRecursive(true)));

			// 2. publish the test node
			assertThat(call(() -> client().publishNode(PROJECT_NAME, nodeUuid))).as("Publish Status").isPublished("en").isPublished("de");

			// 3. Take only en offline
			call(() -> client().takeNodeLanguageOffline(PROJECT_NAME, nodeUuid, "en"));
			assertThat(call(() -> client().getNodePublishStatus(PROJECT_NAME, nodeUuid))).as("Publish status").isNotPublished("en");

			// 4. Assert status
			assertThat(call(() -> client().getNodePublishStatus(PROJECT_NAME, nodeUuid))).as("Publish status").isNotPublished("en").isPublished("de");

			// 5. Take also de offline
			call(() -> client().takeNodeLanguageOffline(PROJECT_NAME, nodeUuid, "de"));
			assertThat(call(() -> client().getNodePublishStatus(PROJECT_NAME, nodeUuid))).as("Publish status").isNotPublished("de");

			// 6. Assert that both are offline
			assertThat(call(() -> client().getNodePublishStatus(PROJECT_NAME, nodeUuid))).as("Publish status").isNotPublished("en")
					.isNotPublished("de");
		}
	}

	@Test
	public void testTakeNodeOfflineNoPermission() {
		try (Tx tx = tx()) {
			Node node = folder("products");
			String nodeUuid = node.getUuid();

			assertThat(call(() -> client().publishNode(PROJECT_NAME, nodeUuid))).as("Publish Status").isPublished("en").isPublished("de");

			db().tx(() -> {
				role().revokePermissions(node, PUBLISH_PERM);
				return null;
			});
			call(() -> client().takeNodeOffline(PROJECT_NAME, nodeUuid), FORBIDDEN, "error_missing_perm", nodeUuid);
		}
	}

	@Test
	public void testTakeNodeLanguageOfflineNoPermission() {
		try (Tx tx = tx()) {
			Node node = folder("products");
			String nodeUuid = node.getUuid();

			assertThat(call(() -> client().publishNode(PROJECT_NAME, nodeUuid))).as("Publish Status").isPublished("en").isPublished("de");

			db().tx(() -> {
				role().revokePermissions(node, PUBLISH_PERM);
				return null;
			});
			call(() -> client().takeNodeLanguageOffline(PROJECT_NAME, nodeUuid, "en"), FORBIDDEN, "error_missing_perm", nodeUuid);
		}
	}

	@Test
	public void testTakeOfflineNodeOffline() {
		try (Tx tx = tx()) {
			Node node = folder("products");
			String nodeUuid = node.getUuid();

			call(() -> client().takeNodeOffline(PROJECT_NAME, nodeUuid, new PublishParametersImpl().setRecursive(true)));
			assertThat(call(() -> client().getNodePublishStatus(PROJECT_NAME, nodeUuid))).as("Publish status").isNotPublished("en")
					.isNotPublished("de");
			// The request should work fine if we call it again
			call(() -> client().takeNodeOffline(PROJECT_NAME, nodeUuid, new PublishParametersImpl().setRecursive(true)));
			assertThat(call(() -> client().getNodePublishStatus(PROJECT_NAME, nodeUuid))).as("Publish status").isNotPublished("en")
					.isNotPublished("de");
		}
	}

	@Test
	public void testTakeOfflineNodeLanguageOffline() {
		try (Tx tx = tx()) {
			Node node = folder("products");
			String nodeUuid = node.getUuid();

			assertThat(call(() -> client().publishNodeLanguage(PROJECT_NAME, nodeUuid, "en"))).as("Initial publish status").isPublished();

			// All nodes are initially published so lets take german offline
			call(() -> client().takeNodeLanguageOffline(PROJECT_NAME, nodeUuid, "de"));

			// Another take offline call should fail since there is no german page online anymore.
			call(() -> client().takeNodeLanguageOffline(PROJECT_NAME, nodeUuid, "de"), NOT_FOUND, "error_language_not_found", "de");
		}
	}

	@Test
	public void testTakeOfflineBogusUuid() {
		call(() -> client().takeNodeOffline(PROJECT_NAME, "bogus"), NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	public void testTakeOfflineEmptyLanguage() {
		try (Tx tx = tx()) {
			Node node = folder("products");
			String nodeUuid = node.getUuid();

			call(() -> client().takeNodeLanguageOffline(PROJECT_NAME, nodeUuid, "fr"), NOT_FOUND, "error_language_not_found", "fr");
		}
	}

	@Test
	public void testTakeOfflineWithOnlineChild() {
		try (Tx tx = tx()) {
			Node news = folder("news");
			Node news2015 = folder("2015");

			call(() -> client().publishNode(PROJECT_NAME, news.getUuid()));
			call(() -> client().publishNode(PROJECT_NAME, news2015.getUuid()));

			call(() -> client().takeNodeOffline(PROJECT_NAME, news.getUuid()), BAD_REQUEST, "node_error_children_containers_still_published");
		}
	}

	@Test
	public void testTakeOfflineLastLanguageWithOnlineChild() {
		String newsUuid = db().tx(() -> folder("news").getUuid());
		String news2015Uuid = db().tx(() -> folder("2015").getUuid());

		call(() -> client().publishNode(PROJECT_NAME, newsUuid));
		call(() -> client().publishNode(PROJECT_NAME, news2015Uuid));

		call(() -> client().takeNodeLanguageOffline(PROJECT_NAME, newsUuid, "de"));

		call(() -> client().takeNodeLanguageOffline(PROJECT_NAME, newsUuid, "en"), BAD_REQUEST, "node_error_children_containers_still_published",
				news2015Uuid);

	}

	@Test
	public void testTakeOfflineForRelease() {
		Node news = folder("news");
		Release newRelease;
		Release initialRelease = db().tx(() -> latestRelease());

		try (Tx tx = tx()) {
			Project project = project();
			newRelease = project.getReleaseRoot().create("newrelease", user());
			tx.success();
		}

		try (Tx tx = tx()) {
			// save the folder in new release
			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("en");
			update.getFields().put("name", FieldUtil.createStringField("News"));
			call(() -> client().updateNode(PROJECT_NAME, news.getUuid(), update, new VersioningParametersImpl().setRelease(newRelease.getName())));

			// publish in initial and new release
			call(() -> client().publishNode(PROJECT_NAME, news.getUuid(), new VersioningParametersImpl().setRelease(initialRelease.getName())));
			call(() -> client().publishNode(PROJECT_NAME, news.getUuid(), new VersioningParametersImpl().setRelease(newRelease.getName())));

			// take offline in initial release
			call(() -> client().takeNodeOffline(PROJECT_NAME, news.getUuid(), new VersioningParametersImpl().setRelease(initialRelease.getName()),
					new PublishParametersImpl().setRecursive(true)));
		}

		try (Tx tx = tx()) {
			// check publish status
			assertThat(call(() -> client().getNodePublishStatus(PROJECT_NAME, news.getUuid(),
					new VersioningParametersImpl().setRelease(initialRelease.getName())))).as("Initial release publish status").isNotPublished("en")
							.isNotPublished("de");
			assertThat(call(() -> client().getNodePublishStatus(PROJECT_NAME, news.getUuid(),
					new VersioningParametersImpl().setRelease(newRelease.getName())))).as("New release publish status").isPublished("en")
							.doesNotContain("de");

		}

	}

	/**
	 * Verify that the takeOffline action fails if the node still has published children.
	 */
	@Test
	public void testTakeNodeOfflineConsistency() {

		// 1. Publish /news & /news/2015
		db().tx(() -> {
			System.out.println(project().getBaseNode().getUuid());
			System.out.println(folder("news").getUuid());
			System.out.println(folder("2015").getUuid());
			return null;
		});

		// 2. Take folder /news offline - This should fail since folder /news/2015 is still published
		db().tx(() -> {
			// 1. Take folder offline
			Node node = folder("news");
			call(() -> client().takeNodeOffline(PROJECT_NAME, node.getUuid()), BAD_REQUEST, "node_error_children_containers_still_published");
			return null;
		});

		// 3. Take sub nodes offline
		db().tx(() -> {
			call(() -> client().takeNodeOffline(PROJECT_NAME, content("news overview").getUuid(), new PublishParametersImpl().setRecursive(false)));
			call(() -> client().takeNodeOffline(PROJECT_NAME, folder("2015").getUuid(), new PublishParametersImpl().setRecursive(true)));
			call(() -> client().takeNodeOffline(PROJECT_NAME, folder("2014").getUuid(), new PublishParametersImpl().setRecursive(true)));
			return null;
		});

		// 4. Take folder /news offline - It should work since all child nodes have been taken offline
		db().tx(() -> {
			// 1. Take folder offline
			Node node = folder("news");
			call(() -> client().takeNodeOffline(PROJECT_NAME, node.getUuid()));
			return null;
		});

	}

}
