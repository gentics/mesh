package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.data.relationship.GraphPermission.PUBLISH_PERM;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class NodeTakeOfflineEndpointTest extends AbstractMeshTest {
	@Before
	public void addAdminPerms() {
		// Grant admin perms. Otherwise we can't check the jobs
		tx(() -> group().addRole(roles().get("admin")));
	}

	@Test
	public void testTakeNodeOfflineManyChildren() {
		String baseNodeUuid = tx(() -> project().getBaseNode().getUuid());
		String schemaUuid = tx(() -> schemaContainer("content").getUuid());
		String parentNodeUuid = tx(() -> folder("news").getUuid());

		// Create a lot of test nodes
		for (int i = 0; i < 1000; i++) {
			NodeCreateRequest request = new NodeCreateRequest();
			SchemaReferenceImpl schemaReference = new SchemaReferenceImpl();
			schemaReference.setName("content");
			schemaReference.setUuid(schemaUuid);
			request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			request.getFields().put("slug", FieldUtil.createStringField("new-page" + i + ".html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setSchema(schemaReference);
			request.setLanguage("en");
			request.setParentNodeUuid(parentNodeUuid);
			NodeResponse response = call(() -> client().createNode(PROJECT_NAME, request));
			call(() -> client().publishNode(PROJECT_NAME, response.getUuid()));
		}

		call(() -> client().takeNodeOffline(PROJECT_NAME, baseNodeUuid, new PublishParametersImpl().setRecursive(true)));

	}

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
				NodeGraphFieldContainer container = folder("products").getGraphFieldContainer(language);
				GraphFieldContainerEdge draftEdge = container.getContainerEdge(DRAFT, initialBranchUuid()).next();
				assertThat(draftEdge.getSegmentInfo()).isNotNull();
				GraphFieldContainerEdge publishEdge = container.getContainerEdge(PUBLISHED, initialBranchUuid()).next();
				assertThat(publishEdge.getSegmentInfo()).isNotNull();
			}
		}

		call(() -> client().takeNodeOffline(PROJECT_NAME, nodeUuid, new PublishParametersImpl().setRecursive(true)));
		assertThat(call(() -> client().getNodePublishStatus(PROJECT_NAME, nodeUuid))).as("Publish status").isNotPublished("en").isNotPublished("de");

		// assert that the containers have only the draft webrootpath properties set
		try (Tx tx2 = tx()) {
			for (String language : Arrays.asList("en", "de")) {
				NodeGraphFieldContainer container = folder("products").getGraphFieldContainer(language);
				GraphFieldContainerEdge draftEdge = container.getContainerEdge(DRAFT, initialBranchUuid()).next();
				assertThat(draftEdge.getSegmentInfo()).isNotNull();
				assertFalse(container.getContainerEdge(PUBLISHED, initialBranchUuid()).hasNext());
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
			call(() -> client().takeNodeOffline(PROJECT_NAME, nodeUuid), FORBIDDEN, "error_missing_perm", nodeUuid, PUBLISH_PERM.getRestPerm().getName());
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
			call(() -> client().takeNodeLanguageOffline(PROJECT_NAME, nodeUuid, "en"), FORBIDDEN, "error_missing_perm", nodeUuid, PUBLISH_PERM.getRestPerm().getName());
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
	public void testTakeOfflineForBranch() {
		Node news = folder("news");
		Branch initialBranch = db().tx(() -> latestBranch());

		Branch newBranch = createBranch("newbranch", true);

		try (Tx tx = tx()) {
			// publish in initial and new branch
			call(() -> client().publishNode(PROJECT_NAME, news.getUuid(), new VersioningParametersImpl().setBranch(initialBranch.getName())));
			call(() -> client().publishNode(PROJECT_NAME, news.getUuid(), new VersioningParametersImpl().setBranch(newBranch.getName())));

			// take offline in initial branch
			call(() -> client().takeNodeOffline(PROJECT_NAME, news.getUuid(), new VersioningParametersImpl().setBranch(initialBranch.getName()),
				new PublishParametersImpl().setRecursive(true)));
		}

		try (Tx tx = tx()) {
			// check publish status
			assertThat(call(() -> client().getNodePublishStatus(PROJECT_NAME, news.getUuid(),
				new VersioningParametersImpl().setBranch(initialBranch.getName())))).as("Initial branch publish status").isNotPublished("en")
					.isNotPublished("de");
			assertThat(call(() -> client().getNodePublishStatus(PROJECT_NAME, news.getUuid(),
				new VersioningParametersImpl().setBranch(newBranch.getName())))).as("New branch publish status").isPublished("en").isPublished("de");

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
