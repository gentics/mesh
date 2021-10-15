package com.gentics.mesh.search.index;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.MeshEvent.INDEX_SYNC_FINISHED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.gentics.elasticsearch.client.ElasticsearchClient;
import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.mesh.context.impl.BulkActionContextImpl;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.microschema.MicroschemaModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.search.EntityMetrics;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.context.helper.ExpectedEvent;
import com.gentics.mesh.test.context.helper.UnexpectedEvent;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Flowable;
import io.vertx.core.json.JsonObject;
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

	/**
	 * Test that the check for index existence and correctness will not change anything if not necessary
	 * @throws Exception
	 */
	@Test
	public void testIndexSyncCheckNoChange() throws Exception {
		int timeoutMs = 10_000;
		grantAdmin();
		searchProvider().refreshIndex().blockingAwait();

		// trigger the check by publishing the event, and expect the "check finished" but not the "sync finished" events to be thrown
		try (ExpectedEvent finished = expectEvent(MeshEvent.INDEX_CHECK_FINISHED, timeoutMs);
				UnexpectedEvent syncFinished = notExpectEvent(MeshEvent.INDEX_SYNC_FINISHED, timeoutMs)) {
			vertx().eventBus().publish(MeshEvent.INDEX_CHECK_REQUEST.address, null);
		}
	}

	/**
	 * Test that the check for index mapping will recreate and repopulate a dropped index
	 * @throws Exception
	 */
	@Test
	public void testAutoIndexRecreation() throws Exception {
		int timeoutMs = 10_000;

		// name of the index (without installation prefix)
		String meshIndexName = "project";
		String esIndexName = options().getSearchOptions().getPrefix() + meshIndexName;

		ElasticsearchClient<JsonObject> client = searchProvider().getClient();
		grantAdmin();
		searchProvider().refreshIndex().blockingAwait();

		// read all project uuids
		Set<String> projectUuids = call(() -> client().findProjects()).getData().stream().map(ProjectResponse::getUuid)
				.collect(Collectors.toSet());
		assertThat(projectUuids).isNotEmpty();

		// read the (correct) mapping for comparing later
		JsonObject mappings = getIndexMappings(esIndexName);

		// drop the index
		client.deleteIndex(esIndexName).sync();

		try {
			client.readIndex(esIndexName).sync();
			fail("Index " + esIndexName + " should have been deleted");
		} catch (HttpErrorException e) {
			// everything else than the expected NOT FOUND is re-thrown
			if (e.statusCode != HttpResponseStatus.NOT_FOUND.code()) {
				throw e;
			}
		}

		// trigger the check by publishing the event, and expect the "check finished" and the "sync finished" events to be thrown
		try (ExpectedEvent finished = expectEvent(MeshEvent.INDEX_CHECK_FINISHED, timeoutMs);
				ExpectedEvent syncFinished = expectEvent(MeshEvent.INDEX_SYNC_FINISHED, timeoutMs)) {
			vertx().eventBus().publish(MeshEvent.INDEX_CHECK_REQUEST.address, null);
		}

		// read the index and compare the mappings with the original mappings
		assertThat(getIndexMappings(esIndexName)).isEqualTo(mappings);

		// read the documents from the index
		Flowable.fromIterable(projectUuids).flatMapSingle(uuid -> {
			return client.readDocument(esIndexName, uuid).async();
		}).blockingSubscribe();
	}

	/**
	 * Test that the check for index existence and correctness will drop, recreate and repopulate an incorrect index
	 * @throws Exception
	 */
	@Test
	public void testAutoIndexFix() throws Exception {
		int timeoutMs = 10_000;

		// name of the index (without installation prefix)
		String meshIndexName = "user";
		String esIndexName = options().getSearchOptions().getPrefix() + meshIndexName;

		ElasticsearchClient<JsonObject> client = searchProvider().getClient();
		grantAdmin();
		searchProvider().refreshIndex().blockingAwait();

		// read all user uuids
		Set<String> userUuids = call(() -> client().findUsers()).getData().stream().map(UserResponse::getUuid)
				.collect(Collectors.toSet());
		assertThat(userUuids).isNotEmpty();

		// read the (correct) mapping for comparing later
		JsonObject mappings = getIndexMappings(esIndexName);

		// drop the index for "project"
		client.deleteIndex(esIndexName).sync();
		// create with default mappings by storing a dummy document
		client.storeDocument(esIndexName, "dummy", new JsonObject("{\"name\": \"dummy\"}")).sync();
		// index mappings should now be different
		assertThat(getIndexMappings(esIndexName)).isNotEqualTo(mappings);

		// trigger the check by publishing the event, and expect the "check finished" and the "sync finished" events to be thrown
		try (ExpectedEvent finished = expectEvent(MeshEvent.INDEX_CHECK_FINISHED, timeoutMs);
				ExpectedEvent syncFinished = expectEvent(MeshEvent.INDEX_SYNC_FINISHED, timeoutMs)) {
			vertx().eventBus().publish(MeshEvent.INDEX_CHECK_REQUEST.address, null);
		}

		// read the index and compare the mappings with the original mappings
		assertThat(getIndexMappings(esIndexName)).isEqualTo(mappings);

		// read the documents from the index
		Flowable.fromIterable(userUuids).flatMapSingle(uuid -> {
			return client.readDocument(esIndexName, uuid).async();
		}).blockingSubscribe();

		// trigger the check by publishing the event, and expect the "check finished" but not the "sync finished" events to be thrown
		try (ExpectedEvent finished = expectEvent(MeshEvent.INDEX_CHECK_FINISHED, timeoutMs);
				UnexpectedEvent syncFinished = notExpectEvent(MeshEvent.INDEX_SYNC_FINISHED, timeoutMs)) {
			vertx().eventBus().publish(MeshEvent.INDEX_CHECK_REQUEST.address, null);
		}
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
}
