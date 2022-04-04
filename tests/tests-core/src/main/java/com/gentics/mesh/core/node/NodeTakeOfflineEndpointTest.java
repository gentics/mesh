package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.perm.InternalPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_CONTENT_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_PUBLISHED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UNPUBLISHED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = FULL, startServer = true)
public class NodeTakeOfflineEndpointTest extends AbstractMeshTest {

	@Test
	public void testTakeNodeOfflineManyChildren() throws Exception {
		String baseNodeUuid = tx(() -> project().getBaseNode().getUuid());
		String schemaUuid = tx(() -> schemaContainer("content").getUuid());
		String folderSchemaUuid = tx(() -> schemaContainer("folder").getUuid());
		String contentSchemaUuid = tx(() -> schemaContainer("content").getUuid());
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
			request.setLanguage("en");
			request.setSchema(schemaReference);
			request.setParentNodeUuid(parentNodeUuid);
			NodeResponse response = call(() -> client().createNode(PROJECT_NAME, request));
			call(() -> client().publishNode(PROJECT_NAME, response.getUuid()));
		}

		expect(NODE_UNPUBLISHED).match(1029, NodeMeshEventModel.class, event -> {
			assertThat(event)
				.uuidNotNull()
				.hasBranchUuid(initialBranchUuid());
			assertThat(event.getSchema().getUuid()).matches(contentSchemaUuid + "|" + folderSchemaUuid);
			assertThat(event.getLanguageTag()).matches("en|de");
			assertThat(event.getSchema().getName()).matches("folder|content");
		}).total(1029);

		expect(NODE_DELETED).none();
		expect(NODE_CONTENT_DELETED).none();

		call(() -> client().takeNodeOffline(PROJECT_NAME, baseNodeUuid, new PublishParametersImpl().setRecursive(true)));
		awaitEvents();

	}

	@Test
	public void testTakeNodeOffline() {
		HibNode node = folder("products");
		String nodeUuid = tx(() -> node.getUuid());
		String schemaUuid = tx(() -> schemaContainer("folder").getUuid());
		String baseNodeUuid = tx(() -> project().getBaseNode().getUuid());

		expect(NODE_UNPUBLISHED).match(1, NodeMeshEventModel.class, event -> {
			assertThat(event)
				.hasUuid(baseNodeUuid)
				.hasSchema("folder", schemaUuid)
				.hasBranchUuid(initialBranchUuid())
				.hasLanguage("en")
				.hasProject(PROJECT_NAME, projectUuid());
		});
		expect(NODE_UNPUBLISHED).total(29);
		call(() -> client().takeNodeOffline(PROJECT_NAME, baseNodeUuid, new PublishParametersImpl().setRecursive(true)));
		awaitEvents();

		expect(NODE_PUBLISHED).match(1, NodeMeshEventModel.class, event -> {
			assertThat(event)
				.hasUuid(nodeUuid)
				.hasSchema("folder", schemaUuid)
				.hasBranchUuid(initialBranchUuid())
				.hasLanguage("de")
				.hasProject(PROJECT_NAME, projectUuid());
		});
		expect(NODE_PUBLISHED).match(1, NodeMeshEventModel.class, event -> {
			assertThat(event)
				.hasUuid(nodeUuid)
				.hasSchema("folder", schemaUuid)
				.hasBranchUuid(initialBranchUuid())
				.hasLanguage("en")
				.hasProject(PROJECT_NAME, projectUuid());
		});
		expect(NODE_PUBLISHED).total(2);
		assertThat(call(() -> client().publishNode(PROJECT_NAME, nodeUuid))).as("Publish Status").isPublished("en").isPublished("de");
		awaitEvents();

		call(() -> client().takeNodeOffline(PROJECT_NAME, nodeUuid, new PublishParametersImpl().setRecursive(true)));
		assertThat(call(() -> client().getNodePublishStatus(PROJECT_NAME, nodeUuid))).as("Publish status").isNotPublished("en").isNotPublished("de");
	}

	@Test
	public void testTakeNodeLanguageOffline() {
		try (Tx tx = tx()) {
			HibNode node = folder("products");
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
		HibNode node;
		String nodeUuid;
		try (Tx tx = tx()) {
			node = folder("products");
			nodeUuid = node.getUuid();

			assertThat(call(() -> client().publishNode(PROJECT_NAME, nodeUuid))).as("Publish Status").isPublished("en").isPublished("de");
		}

		tx((tx) -> {
			RoleDao roleDao = tx.roleDao();
			roleDao.revokePermissions(role(), node, PUBLISH_PERM);
			return null;
		});

		tx(() -> {
			call(() -> client().takeNodeOffline(PROJECT_NAME, nodeUuid), FORBIDDEN, "error_missing_perm", nodeUuid,
				PUBLISH_PERM.getRestPerm().getName());
		});
	}

	@Test
	public void testTakeNodeLanguageOfflineNoPermission() {
		HibNode node;
		String nodeUuid;

		try (Tx tx = tx()) {
			node = folder("products");
			nodeUuid = node.getUuid();
		}
		assertThat(call(() -> client().publishNode(PROJECT_NAME, nodeUuid))).as("Publish Status").isPublished("en").isPublished("de");

		tx((tx) -> {
			RoleDao roleDao = tx.roleDao();
			roleDao.revokePermissions(role(), node, PUBLISH_PERM);
		});

		call(() -> client().takeNodeLanguageOffline(PROJECT_NAME, nodeUuid, "en"), FORBIDDEN, "error_missing_perm", nodeUuid,
				PUBLISH_PERM.getRestPerm().getName());
	}

	@Test
	public void testTakeOfflineNodeOffline() {
		try (Tx tx = tx()) {
			HibNode node = folder("products");
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
			HibNode node = folder("products");
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
			HibNode node = folder("products");
			String nodeUuid = node.getUuid();

			call(() -> client().takeNodeLanguageOffline(PROJECT_NAME, nodeUuid, "fr"), NOT_FOUND, "error_language_not_found", "fr");
		}
	}

	@Test
	public void testTakeOfflineWithOnlineChild() {
		try (Tx tx = tx()) {
			HibNode news = folder("news");
			HibNode news2015 = folder("2015");

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
		HibNode news = folder("news");
		HibBranch initialBranch = db().tx(() -> latestBranch());

		HibBranch newBranch = createBranch("newbranch", true);

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
			HibNode node = folder("news");
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
			HibNode node = folder("news");
			call(() -> client().takeNodeOffline(PROJECT_NAME, node.getUuid()));
			return null;
		});

	}

}
