package com.gentics.mesh.search.index;

import static com.gentics.mesh.MeshEvent.INDEX_SYNC;
import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.gentics.mesh.context.impl.BulkActionContextImpl;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.EventQueueBatch;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.microschema.MicroschemaModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.search.SearchStatusResponse;
import com.gentics.mesh.search.verticle.ElasticsearchSyncVerticle;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

/**
 * Test differential sync of elasticsearch.
 */
@MeshTestSetting(useElasticsearch = true, testSize = TestSize.FULL, startServer = true)
public class BasicIndexSyncTest extends AbstractMeshTest {

	@Before
	public void setup() throws Exception {
		getProvider().clear().blockingAwait();
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
	}

	@Test
	@Ignore("Fails on CI pipeline. See https://github.com/gentics/mesh/issues/608")
	public void testIndexSyncLock() throws Exception {
		grantAdminRole();
		tx(() -> {
			for (int i = 0; i < 900; i++) {
				boot().groupRoot().create("group_" + i, user(), null);
			}
		});
		waitForEvent(INDEX_SYNC, () -> {
			call(() -> client().invokeIndexSync());
			call(() -> client().invokeIndexSync(), SERVICE_UNAVAILABLE, "search_admin_index_sync_already_in_progress");
		});
	}

	@Test
	public void testNoPermSync() {
		revokeAdminRole();
		call(() -> client().invokeIndexSync(), FORBIDDEN, "error_admin_permission_required");
	}

	@Test
	public void testResync() throws Exception {
		// Add the user to the admin group - this way the user is in fact an admin.
		grantAdminRole();
		searchProvider().refreshIndex().blockingAwait();

		waitForEvent(INDEX_SYNC, () -> {
			GenericMessageResponse message = call(() -> client().invokeIndexSync());
			assertThat(message).matches("search_admin_index_sync_invoked");
		});

	}

	@Test
	@Ignore("Currently fails due to https://github.com/gentics/mesh/issues/606")
	public void testUserSync() throws Exception {
		// Assert insert
		tx(() -> {
			for (int i = 0; i < 400; i++) {
				boot().userRoot().create("user_" + i, user(), null);
			}
		});
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("user", 400, 0, 0);

		// Assert update
		tx(() -> {
			user().setUsername("updated");
		});
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("user", 0, 1, 0);

		// Assert deletion
		tx(() -> {
			boot().userRoot().findByName("user_3").getElement().remove();
		});
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("user", 0, 0, 1);
	}

	@Test
	public void testGroupSync() throws Exception {
		// Assert insert
		tx(() -> {
			for (int i = 0; i < 400; i++) {
				boot().groupRoot().create("group_" + i, user(), null);
			}
		});
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("group", 400, 0, 0);

		// Assert update
		tx(() -> {
			group().setName("updated");
		});
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("group", 0, 1, 0);

		// Assert deletion
		tx(() -> {
			boot().groupRoot().findByName("group_3").getElement().remove();
		});
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("group", 0, 0, 1);
	}

	@Test
	public void testRoleSync() throws Exception {
		// Assert insert
		tx(() -> {
			for (int i = 0; i < 400; i++) {
				boot().roleRoot().create("role_" + i, user(), null);
			}
		});
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("role", 400, 0, 0);

		// Assert update
		tx(() -> {
			role().setName("updated");
		});
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("role", 0, 1, 0);

		// Assert deletion
		tx(() -> {
			boot().roleRoot().findByName("role_3").getElement().remove();
		});
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("role", 0, 0, 1);
	}

	@Test
	@Ignore("Currently fails due to https://github.com/gentics/mesh/issues/606")
	public void testTagSync() throws Exception {
		// Assert insert
		tx(() -> {
			for (int i = 0; i < 400; i++) {
				tagFamily("colors").create("tag_" + i, project(), user());
			}
		});
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		// 400: The additional tags needs to be added to the index
		// 3: Tag family needs to be updated and the two color tags
		assertMetrics("tag", 400, 3, 0);

		// Assert update
		tx(() -> {
			tag("red").setName("updated");
		});
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("tag", 0, 1, 0);

		// Assert deletion
		tx(() -> {
			boot().tagRoot().findByName("tag_3").getElement().remove();
		});
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("tag", 0, 0, 1);
	}

	@Test
	public void testTagFamilySync() throws Exception {
		// Assert insert
		tx(() -> {
			for (int i = 0; i < 400; i++) {
				project().getTagFamilyRoot().create("tagfamily_" + i, user());
			}
		});
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		// 400: The additional tag families need to be added to the index
		assertMetrics("tagfamily", 400, 0, 0);
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("tagfamily", 0, 0, 0);

		// Assert update
		tx(() -> {
			tagFamily("colors").setName("updated");
		});
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("tagfamily", 0, 1, 0);

		// Assert deletion
		tx(() -> {
			boot().tagFamilyRoot().findByName("tagfamily_3").getElement().remove();
		});
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("tagfamily", 0, 0, 1);
	}

	@Test
	public void testProjectSync() throws Exception {
		// Assert insert
		tx(() -> {
			for (int i = 0; i < 3; i++) {
				final int e = i;
				call(() -> client().createProject(new ProjectCreateRequest().setName("project_" + e).setSchemaRef("folder")));
			}
		});
		getProvider().clear().blockingAwait();
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("project", 4, 0, 0);

		// Assert update
		tx(() -> {
			project().setName("updated");
		});
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("project", 0, 1, 0);

		// Now manually delete the project
		tx(() -> {
			Project project = boot().projectRoot().findByName("project_2");
			BulkActionContextImpl context = Mockito.mock(BulkActionContextImpl.class);
			Mockito.when(context.batch()).thenReturn(Mockito.mock(EventQueueBatch.class));
			project.delete(context);
		});
		// Assert that the deletion was detected
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("project", 0, 0, 1);
	}

	@Test
	public void testNodeSync() throws Exception {
		// Assert insert
		tx(() -> {
			Node node = folder("2015");
			node.createGraphFieldContainer(german(), initialBranch(), user());
		});
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("node", 1, 2, 0);
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("node", 0, 0, 0);

		// Assert update
		tx(() -> {
			NodeGraphFieldContainer draft = content().getGraphFieldContainer(english(), latestBranch(), ContainerType.DRAFT);
			draft.getString("slug").setString("updated");
		});
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("node", 0, 2, 0);

		// Assert deletion
		tx(() -> {
			NodeGraphFieldContainer draft = folder("2015").getGraphFieldContainer(german(), latestBranch(), ContainerType.DRAFT);
			draft.remove();
		});
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("node", 0, 2, 1);
	}

	@Test
	public void testSchemaSync() throws Exception {
		// Assert insert
		tx(() -> {
			for (int i = 0; i < 400; i++) {
				SchemaModel model = new SchemaModelImpl();
				model.setName("schema_" + i);
				boot().schemaContainerRoot().create(model, user());
			}
		});
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("schema", 400, 0, 0);

		// Assert update
		SchemaResponse response = call(() -> client().createSchema(new SchemaCreateRequest().setName("dummy")));
		tx(() -> {
			boot().schemaContainerRoot().findByUuid(response.getUuid()).setName("updated");
		});
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("schema", 0, 1, 0);

		// Assert deletion
		tx(() -> {
			SchemaContainer schema = boot().schemaContainerRoot().findByName("schema_3");
			schema.getLatestVersion().remove();
			schema.remove();
		});
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("schema", 0, 0, 1);
	}

	@Test
	public void testMicroschemaSync() throws Exception {
		// Assert insert
		tx(() -> {
			for (int i = 0; i < 400; i++) {
				MicroschemaModel model = new MicroschemaModelImpl();
				model.setName("microschema_" + i);
				boot().microschemaContainerRoot().create(model, user());
			}
		});
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("microschema", 400, 0, 0);

		// Assert update
		tx(() -> {
			boot().microschemaContainerRoot().findByName("microschema_100").setName("updated");
		});
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("microschema", 0, 1, 0);

		// Assert deletion
		tx(() -> {
			MicroschemaContainer microschema = boot().microschemaContainerRoot().findByName("microschema_101");
			microschema.getLatestVersion().remove();
			microschema.remove();
		});
		waitForEvent(INDEX_SYNC, ElasticsearchSyncVerticle::invokeSync);
		assertMetrics("microschema", 0, 0, 1);
	}

	private void assertMetrics(String type, int inserted, int updated, int deleted) {
		assertMetrics(type, "insert", inserted);
		assertMetrics(type, "update", updated);
		assertMetrics(type, "delete", deleted);
	}

	private void assertMetrics(String type, String subType, long expectedCount) {
		MetricRegistry registry = SharedMetricRegistries.getOrCreate("mesh");
		assertEquals("The expected total count did not match for type {" + type + "} / {" + subType + "}", expectedCount,
			registry.getCounters().get("index.sync." + type + "." + subType + ".total").getCount());
		assertEquals("The pending items should be zero.", 0,
			registry.getCounters().get("index.sync." + type + "." + subType + ".pending").getCount());

		SearchStatusResponse status = call(() -> client().searchStatus());
		Map<String, Integer> infos = (Map<String, Integer>) status.getMetrics().get(type);
		assertEquals(expectedCount, infos.get(subType + ".total").intValue());
		assertEquals(0, infos.get(subType + ".pending").intValue());
	}

}
