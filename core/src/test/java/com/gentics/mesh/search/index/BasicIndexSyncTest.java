package com.gentics.mesh.search.index;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.MeshEvent.INDEX_SYNC_FINISHED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.gentics.mesh.context.impl.BulkActionContextImpl;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.search.EntityMetrics;
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
		grantAdmin();
		tx(tx -> {
			for (int i = 0; i < 900; i++) {
				tx.data().groupDao().create("group_" + i, user(), null);
			}
		});
		waitForEvent(INDEX_SYNC_FINISHED, () -> {
			call(() -> client().invokeIndexSync());
			call(() -> client().invokeIndexSync(), SERVICE_UNAVAILABLE, "search_admin_index_sync_already_in_progress");
		});
	}

	@Test
	public void testNoPermSync() {
		revokeAdmin();
		call(() -> client().invokeIndexSync(), FORBIDDEN, "error_admin_permission_required");
	}

	@Test
	public void testResync() throws Exception {
		// Add the user to the admin group - this way the user is in fact an admin.
		grantAdmin();
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
				boot().userDao().create("user_" + i, user(), null);
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
			boot().userDao().findByName("user_3").remove();;
		});
		syncIndex();
		assertMetrics("user", 0, 0, 1);
	}

	@Test
	public void testGroupSync() throws Exception {
		// Assert insert
		tx(tx -> {
			for (int i = 0; i < 400; i++) {
				tx.data().groupDao().create("group_" + i, user(), null);
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
			boot().groupDao().findByName("group_3").removeElement();
		});
		syncIndex();
		assertMetrics("group", 0, 0, 1);
	}

	@Test
	public void testRoleSync() throws Exception {
		// Assert insert
		tx(() -> {
			for (int i = 0; i < 400; i++) {
				boot().roleDao().create("role_" + i, user(), null);
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
			boot().roleDao().findByName("role_3").removeElement();
		});
		syncIndex();
		assertMetrics("role", 0, 0, 1);
	}

	@Test
	@Ignore("Currently fails due to https://github.com/gentics/mesh/issues/606")
	public void testTagSync() throws Exception {
		// Assert insert
		tx(tx -> {
			TagDaoWrapper tagDao = tx.data().tagDao();
			for (int i = 0; i < 400; i++) {
				tagDao.create(tagFamily("colors"), "tag_" + i, project(), user());
			}
		});
		syncIndex();
		// 400: The additional tags needs to be added to the index
		// 3: Tag family needs to be updated and the two color tags
		assertMetrics("tag", 400, 3, 0);

		// Assert update
		tx(tx -> {
			tag("red").setName("updated");
		});
		syncIndex();
		assertMetrics("tag", 0, 1, 0);

		// Assert deletion
		tx(tx -> {
			HibTagFamily tagFamily = tagFamily("colors");
			boot().tagDao().findByName(tagFamily, "tag_3").deleteElement();
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
		tx(tx -> {
			boot().tagFamilyDao().findByName(project(), "tagfamily_3").deleteElement();
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
		tx(tx -> {
			HibProject project = tx.data().projectDao().findByName("project_2");
			BulkActionContextImpl context = Mockito.mock(BulkActionContextImpl.class);
			Mockito.when(context.batch()).thenReturn(Mockito.mock(EventQueueBatch.class));
			tx.data().projectDao().delete(project, context);
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
		tx(tx -> {
			ContentDaoWrapper contentDao = tx.data().contentDao();
			NodeGraphFieldContainer draft = contentDao.getGraphFieldContainer(content(), english(), latestBranch(), ContainerType.DRAFT);
			draft.getString("slug").setString("updated");
		});
		syncIndex();
		assertMetrics("node", 0, 2, 0);

		// Assert deletion
		tx(tx -> {
			ContentDaoWrapper contentDao = tx.data().contentDao();
			NodeGraphFieldContainer draft = contentDao.getGraphFieldContainer(folder("2015"), german(), latestBranch(), ContainerType.DRAFT);
			draft.remove();
		});
		syncIndex();
		assertMetrics("node", 0, 2, 1);
	}

	@Test
	public void testSchemaSync() throws Exception {
		// Assert insert
		tx(tx -> {
			SchemaDaoWrapper schemaDao = tx.data().schemaDao();
			for (int i = 0; i < 400; i++) {
				SchemaVersionModel model = new SchemaModelImpl();
				model.setName("schema_" + i);
				schemaDao.create(model, user());
			}
		});
		syncIndex();
		assertMetrics("schema", 400, 0, 0);

		// Assert update
		SchemaResponse response = call(() -> client().createSchema(new SchemaCreateRequest().setName("dummy")));
		waitForSearchIdleEvent();
		tx(() -> {
			boot().schemaDao().findByUuid(response.getUuid()).setName("updated");
		});
		syncIndex();
		assertMetrics("schema", 0, 1, 0);

		// Assert deletion
		tx(tx -> {
			SchemaDaoWrapper schemaDao = tx.data().schemaDao();
			Schema schema = schemaDao.findByName("schema_3");
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
				MicroschemaVersionModel model = new MicroschemaModelImpl();
				model.setName("microschema_" + i);
				createMicroschema(model);
			}
		});
		syncIndex();
		assertMetrics("microschema", 400, 0, 0);

		// Assert update
		tx(() -> {
			boot().microschemaDao().findByName("microschema_100").setName("updated");
		});
		syncIndex();
		assertMetrics("microschema", 0, 1, 0);

		// Assert deletion
		tx(tx -> {
			MicroschemaDaoWrapper microschemaDao = tx.data().microschemaDao();
			Microschema microschema = microschemaDao.findByName("microschema_101");
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
