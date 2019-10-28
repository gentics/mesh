package com.gentics.mesh.search.index;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.MeshEvent.INDEX_SYNC_FINISHED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.gentics.mesh.context.impl.BulkActionContextImpl;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.microschema.MicroschemaModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.search.EntityMetrics;
import com.gentics.mesh.core.rest.search.SearchStatusResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.search.verticle.eventhandler.SyncEventHandler;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
/**
 * Test differential sync of elasticsearch.
 */
@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.FULL, startServer = true)
public class BasicIndexSyncTest extends AbstractMeshTest {

	@Before
	public void setup() throws Exception {
		getProvider().clear().blockingAwait();
		syncIndex();
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
		waitForEvent(INDEX_SYNC_FINISHED, () -> {
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

		waitForEvent(INDEX_SYNC_FINISHED, () -> {
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
		syncIndex();
		assertMetrics("user", 400, 0, 0);

		// Assert update
		tx(() -> {
			user().setUsername("updated");
		});
		syncIndex();
		assertMetrics("user", 0, 1, 0);

		// Assert deletion
		tx(() -> {
			boot().userRoot().findByName("user_3").getElement().remove();
		});
		syncIndex();
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
		syncIndex();
		assertMetrics("group", 400, 0, 0);

		// Assert update
		tx(() -> {
			group().setName("updated");
		});
		syncIndex();
		assertMetrics("group", 0, 1, 0);

		// Assert deletion
		tx(() -> {
			boot().groupRoot().findByName("group_3").getElement().remove();
		});
		syncIndex();
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
		syncIndex();
		assertMetrics("role", 400, 0, 0);

		// Assert update
		tx(() -> {
			role().setName("updated");
		});
		syncIndex();
		assertMetrics("role", 0, 1, 0);

		// Assert deletion
		tx(() -> {
			boot().roleRoot().findByName("role_3").getElement().remove();
		});
		syncIndex();
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
		syncIndex();
		// 400: The additional tags needs to be added to the index
		// 3: Tag family needs to be updated and the two color tags
		assertMetrics("tag", 400, 3, 0);

		// Assert update
		tx(() -> {
			tag("red").setName("updated");
		});
		syncIndex();
		assertMetrics("tag", 0, 1, 0);

		// Assert deletion
		tx(() -> {
			boot().tagRoot().findByName("tag_3").getElement().remove();
		});
		syncIndex();
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
		syncIndex();
		// 400: The additional tag families need to be added to the index
		assertMetrics("tagfamily", 400, 0, 0);
		syncIndex();
		assertMetrics("tagfamily", 0, 0, 0);

		// Assert update
		tx(() -> {
			tagFamily("colors").setName("updated");
		});
		syncIndex();
		assertMetrics("tagfamily", 0, 1, 0);

		// Assert deletion
		tx(() -> {
			boot().tagFamilyRoot().findByName("tagfamily_3").getElement().remove();
		});
		syncIndex();
		assertMetrics("tagfamily", 0, 0, 1);
	}

	@Test
	public void testProjectSync() throws Exception {
		// Assert insert
		for (int i = 0; i < 3; i++) {
			final int e = i;
			call(() -> client().createProject(new ProjectCreateRequest().setName("project_" + e).setSchemaRef("folder")));
		}
		waitForSearchIdleEvent();
		getProvider().clear().blockingAwait();
		syncIndex();
		assertMetrics("project", 4, 0, 0);

		// Assert update
		tx(() -> {
			project().setName("updated");
		});
		boot().globalCacheClear();
		syncIndex();
		assertMetrics("project", 0, 1, 0);

		// Now manually delete the project
		tx(() -> {
			Project project = boot().projectRoot().findByName("project_2");
			BulkActionContextImpl context = Mockito.mock(BulkActionContextImpl.class);
			Mockito.when(context.batch()).thenReturn(Mockito.mock(EventQueueBatch.class));
			project.delete(context);
		});
		boot().globalCacheClear();
		// Assert that the deletion was detected
		syncIndex();
		assertMetrics("project", 0, 0, 1);
	}

	@Test
	public void testNodeSync() throws Exception {
		// Assert insert
		tx(() -> {
			Node node = folder("2015");
			node.createGraphFieldContainer(german(), initialBranch(), user());
		});
		syncIndex();
		assertMetrics("node", 1, 2, 0);
		syncIndex();
		assertMetrics("node", 0, 0, 0);

		// Assert update
		tx(() -> {
			NodeGraphFieldContainer draft = content().getGraphFieldContainer(english(), latestBranch(), ContainerType.DRAFT);
			draft.getString("slug").setString("updated");
		});
		syncIndex();
		assertMetrics("node", 0, 2, 0);

		// Assert deletion
		tx(() -> {
			NodeGraphFieldContainer draft = folder("2015").getGraphFieldContainer(german(), latestBranch(), ContainerType.DRAFT);
			draft.remove();
		});
		syncIndex();
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
		syncIndex();
		assertMetrics("schema", 400, 0, 0);

		// Assert update
		SchemaResponse response = call(() -> client().createSchema(new SchemaCreateRequest().setName("dummy")));
		waitForSearchIdleEvent();
		tx(() -> {
			boot().schemaContainerRoot().findByUuid(response.getUuid()).setName("updated");
		});
		syncIndex();
		assertMetrics("schema", 0, 1, 0);

		// Assert deletion
		tx(() -> {
			SchemaContainer schema = boot().schemaContainerRoot().findByName("schema_3");
			schema.getLatestVersion().remove();
			schema.remove();
		});
		syncIndex();
		assertMetrics("schema", 0, 0, 1);
	}

	@Test
	public void testMicroschemaSync() throws Exception {
		// Assert insert
		tx(() -> {
			for (int i = 0; i < 400; i++) {
				MicroschemaModel model = new MicroschemaModelImpl();
				model.setName("microschema_" + i);
				createMicroschema(model);
			}
		});
		syncIndex();
		assertMetrics("microschema", 400, 0, 0);

		// Assert update
		tx(() -> {
			boot().microschemaContainerRoot().findByName("microschema_100").setName("updated");
		});
		syncIndex();
		assertMetrics("microschema", 0, 1, 0);

		// Assert deletion
		tx(() -> {
			MicroschemaContainer microschema = boot().microschemaContainerRoot().findByName("microschema_101");
			microschema.getLatestVersion().remove();
			microschema.remove();
		});
		syncIndex();
		assertMetrics("microschema", 0, 0, 1);
	}

	private void assertMetrics(String type, long inserted, long updated, long deleted) {
		EntityMetrics entityMetrics = call(() -> client().searchStatus()).getMetrics().get(type);
		assertEquals(inserted, entityMetrics.getInsert().getSynced().longValue());
		assertEquals(updated, entityMetrics.getUpdate().getSynced().longValue());
		assertEquals(deleted, entityMetrics.getDelete().getSynced().longValue());

		assertEquals(0, entityMetrics.getInsert().getPending().longValue());
		assertEquals(0, entityMetrics.getUpdate().getPending().longValue());
		assertEquals(0, entityMetrics.getDelete().getPending().longValue());
	}

	private void syncIndex() {
		waitForEvent(INDEX_SYNC_FINISHED, () -> SyncEventHandler.invokeSync(vertx()));
		refreshIndices();
	}

}
