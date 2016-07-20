package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.PUBLISH_PERM;
import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.parameter.impl.PublishParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;

public class NodeTakeOfflineVerticleTest extends AbstractIsolatedRestVerticleTest {

	@Autowired
	private NodeVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	public void testTakeNodeOffline() {

		String nodeUuid = db.noTrx(() -> {
			Node node = folder("products");
			String uuid = node.getUuid();

			call(() -> getClient().takeNodeOffline(PROJECT_NAME, project().getBaseNode().getUuid(), new PublishParameters().setRecursive(true)));

			assertThat(call(() -> getClient().publishNode(PROJECT_NAME, uuid))).as("Publish Status").isPublished("en").isPublished("de");
			return uuid;
		});

		// assert that the containers have both webrootpath properties set
		try (NoTrx noTx1 = db.noTrx()) {
			for (String language : Arrays.asList("en", "de")) {
				for (String property : Arrays.asList(NodeGraphFieldContainerImpl.WEBROOT_PROPERTY_KEY,
						NodeGraphFieldContainerImpl.PUBLISHED_WEBROOT_PROPERTY_KEY)) {
					assertThat(folder("products").getGraphFieldContainer(language).getImpl().getProperty(property, String.class))
							.as("Property " + property + " for " + language).isNotNull();
				}
			}
		}

		assertThat(call(() -> getClient().takeNodeOffline(PROJECT_NAME, nodeUuid, new PublishParameters().setRecursive(true))))
				.as("Publish Status after take offline").isNotPublished("en").isNotPublished("de");

		// assert that the containers have only the draft webrootpath properties set
		try (NoTrx noTx2 = db.noTrx()) {
			for (String language : Arrays.asList("en", "de")) {
				String property = NodeGraphFieldContainerImpl.WEBROOT_PROPERTY_KEY;
				assertThat(folder("products").getGraphFieldContainer(language).getImpl().getProperty(property, String.class))
						.as("Property " + property + " for " + language).isNotNull();

				property = NodeGraphFieldContainerImpl.PUBLISHED_WEBROOT_PROPERTY_KEY;
				assertThat(folder("products").getGraphFieldContainer(language).getImpl().getProperty(property, String.class))
						.as("Property " + property + " for " + language).isNull();

			}
		}

	}

	@Test
	public void testTakeNodeLanguageOffline() {
		try (NoTrx noTx = db.noTrx()) {
			Node node = folder("products");
			String nodeUuid = node.getUuid();

			// 1. Take all nodes offline
			call(() -> getClient().takeNodeOffline(PROJECT_NAME, project().getBaseNode().getUuid(), new PublishParameters().setRecursive(true)));

			// 2. publish the test node
			assertThat(call(() -> getClient().publishNode(PROJECT_NAME, nodeUuid))).as("Publish Status").isPublished("en").isPublished("de");

			// 3. Take only en offline 
			assertThat(call(() -> getClient().takeNodeLanguageOffline(PROJECT_NAME, nodeUuid, "en"))).as("Status after taken en offline")
					.isNotPublished();

			// 4. Assert status
			assertThat(call(() -> getClient().getNodePublishStatus(PROJECT_NAME, nodeUuid))).as("Publish status").isNotPublished("en")
					.isPublished("de");

			// 5. Take also de offline
			assertThat(call(() -> getClient().takeNodeLanguageOffline(PROJECT_NAME, nodeUuid, "de"))).as("Status after taken en offline")
					.isNotPublished();

			// 6. Assert that both are offline
			assertThat(call(() -> getClient().getNodePublishStatus(PROJECT_NAME, nodeUuid))).as("Publish status").isNotPublished("en")
					.isNotPublished("de");
		}
	}

	@Test
	public void testTakeNodeOfflineNoPermission() {
		try (NoTrx noTx = db.noTrx()) {
			Node node = folder("products");
			String nodeUuid = node.getUuid();

			assertThat(call(() -> getClient().publishNode(PROJECT_NAME, nodeUuid))).as("Publish Status").isPublished("en").isPublished("de");

			db.trx(() -> {
				role().revokePermissions(node, PUBLISH_PERM);
				return null;
			});
			call(() -> getClient().takeNodeOffline(PROJECT_NAME, nodeUuid), FORBIDDEN, "error_missing_perm", nodeUuid);
		}
	}

	@Test
	public void testTakeNodeLanguageOfflineNoPermission() {
		try (NoTrx noTx = db.noTrx()) {
			Node node = folder("products");
			String nodeUuid = node.getUuid();

			assertThat(call(() -> getClient().publishNode(PROJECT_NAME, nodeUuid))).as("Publish Status").isPublished("en").isPublished("de");

			db.trx(() -> {
				role().revokePermissions(node, PUBLISH_PERM);
				return null;
			});
			call(() -> getClient().takeNodeLanguageOffline(PROJECT_NAME, nodeUuid, "en"), FORBIDDEN, "error_missing_perm", nodeUuid);
		}
	}

	@Test
	public void testTakeOfflineNodeOffline() {
		try (NoTrx noTx = db.noTrx()) {
			Node node = folder("products");
			String nodeUuid = node.getUuid();

			assertThat(call(() -> getClient().takeNodeOffline(PROJECT_NAME, nodeUuid, new PublishParameters().setRecursive(true))))
					.as("Publish Status").isNotPublished("en").isNotPublished("de");
			// The request should work fine if we call it again 
			assertThat(call(() -> getClient().takeNodeOffline(PROJECT_NAME, nodeUuid, new PublishParameters().setRecursive(true))))
					.as("Publish Status").isNotPublished("en").isNotPublished("de");
		}
	}

	@Test
	public void testTakeOfflineNodeLanguageOffline() {
		try (NoTrx noTx = db.noTrx()) {
			Node node = folder("products");
			String nodeUuid = node.getUuid();

			assertThat(call(() -> getClient().publishNodeLanguage(PROJECT_NAME, nodeUuid, "en"))).as("Initial publish status").isPublished();

			// All nodes are initially published so lets take german offline  
			call(() -> getClient().takeNodeLanguageOffline(PROJECT_NAME, nodeUuid, "de"));

			// Another take offline call should fail since there is no german page online anymore.
			call(() -> getClient().takeNodeLanguageOffline(PROJECT_NAME, nodeUuid, "de"), NOT_FOUND, "error_language_not_found", "de");
		}
	}

	@Test
	public void testTakeOfflineBogusUuid() {
		call(() -> getClient().takeNodeOffline(PROJECT_NAME, "bogus"), NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	public void testTakeOfflineEmptyLanguage() {
		try (NoTrx noTx = db.noTrx()) {
			Node node = folder("products");
			String nodeUuid = node.getUuid();

			call(() -> getClient().takeNodeLanguageOffline(PROJECT_NAME, nodeUuid, "fr"), NOT_FOUND, "error_language_not_found", "fr");
		}
	}

	@Test
	public void testTakeOfflineWithOnlineChild() {
		try (NoTrx noTx = db.noTrx()) {
			Node news = folder("news");
			Node news2015 = folder("2015");

			call(() -> getClient().publishNode(PROJECT_NAME, news.getUuid()));
			call(() -> getClient().publishNode(PROJECT_NAME, news2015.getUuid()));

			call(() -> getClient().takeNodeOffline(PROJECT_NAME, news.getUuid()), BAD_REQUEST, "node_error_children_containers_still_published");
		}
	}

	@Test
	public void testTakeOfflineLastLanguageWithOnlineChild() {
		String newsUuid = db.noTrx(() -> folder("news").getUuid());
		String news2015Uuid = db.noTrx(() -> folder("2015").getUuid());

		call(() -> getClient().publishNode(PROJECT_NAME, newsUuid));
		call(() -> getClient().publishNode(PROJECT_NAME, news2015Uuid));

		call(() -> getClient().takeNodeLanguageOffline(PROJECT_NAME, newsUuid, "de"));

		call(() -> getClient().takeNodeLanguageOffline(PROJECT_NAME, newsUuid, "en"), BAD_REQUEST, "node_error_children_containers_still_published",
				news2015Uuid);

	}

	@Test
	public void testTakeOfflineForRelease() {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			Release initialRelease = project.getInitialRelease();
			Release newRelease = project.getReleaseRoot().create("newrelease", user());
			Node news = folder("news");

			// save the folder in new release
			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("en");
			update.getFields().put("name", FieldUtil.createStringField("News"));
			call(() -> getClient().updateNode(PROJECT_NAME, news.getUuid(), update, new VersioningParameters().setRelease(newRelease.getName())));

			// publish in initial and new release
			call(() -> getClient().publishNode(PROJECT_NAME, news.getUuid(), new VersioningParameters().setRelease(initialRelease.getName())));
			call(() -> getClient().publishNode(PROJECT_NAME, news.getUuid(), new VersioningParameters().setRelease(newRelease.getName())));

			// take offline in initial release
			call(() -> getClient().takeNodeOffline(PROJECT_NAME, news.getUuid(), new VersioningParameters().setRelease(initialRelease.getName()),
					new PublishParameters().setRecursive(true)));

			// check publish status
			assertThat(call(() -> getClient().getNodePublishStatus(PROJECT_NAME, news.getUuid(),
					new VersioningParameters().setRelease(initialRelease.getName())))).as("Initial release publish status").isNotPublished("en")
							.isNotPublished("de");
			assertThat(call(() -> getClient().getNodePublishStatus(PROJECT_NAME, news.getUuid(),
					new VersioningParameters().setRelease(newRelease.getName())))).as("New release publish status").isPublished("en")
							.doesNotContain("de");

		}
	}

	/**
	 * Verify that the takeOffline action fails if the node still has published children.
	 */
	@Test
	public void testTakeNodeOfflineConsistency() {

		//1. Publish /news  & /news/2015
		db.noTrx(() -> {
			System.out.println(project().getBaseNode().getUuid());
			System.out.println(folder("news").getUuid());
			System.out.println(folder("2015").getUuid());
			return null;
		});

		// 2. Take folder /news offline - This should fail since folder /news/2015 is still published
		db.noTrx(() -> {
			// 1. Take folder offline
			Node node = folder("news");
			call(() -> getClient().takeNodeOffline(PROJECT_NAME, node.getUuid()), BAD_REQUEST, "node_error_children_containers_still_published");
			return null;
		});

		//3. Take sub nodes offline
		db.noTrx(() -> {
			call(() -> getClient().takeNodeOffline(PROJECT_NAME, content("news overview").getUuid(), new PublishParameters().setRecursive(false)));
			call(() -> getClient().takeNodeOffline(PROJECT_NAME, folder("2015").getUuid(), new PublishParameters().setRecursive(true)));
			call(() -> getClient().takeNodeOffline(PROJECT_NAME, folder("2014").getUuid(), new PublishParameters().setRecursive(true)));
			return null;
		});

		// 4. Take folder /news offline - It should work since all child nodes have been taken offline
		db.noTrx(() -> {
			// 1. Take folder offline
			Node node = folder("news");
			call(() -> getClient().takeNodeOffline(PROJECT_NAME, node.getUuid()));
			return null;
		});

	}

}
