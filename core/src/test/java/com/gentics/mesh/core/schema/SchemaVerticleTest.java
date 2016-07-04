package com.gentics.mesh.core.schema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.util.MeshAssert.assertElement;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.core.verticle.schema.SchemaVerticle;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.parameter.impl.RolePermissionParameters;
import com.gentics.mesh.test.AbstractBasicIsolatedCrudVerticleTest;
import com.gentics.mesh.util.FieldUtil;

import io.vertx.core.Future;

public class SchemaVerticleTest extends AbstractBasicIsolatedCrudVerticleTest {

	@Autowired
	private SchemaVerticle schemaVerticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(schemaVerticle);
		return list;
	}

	// Create Tests

	@Test
	@Override
	public void testCreate() throws GenericRestException, Exception {
		try (NoTrx noTx = db.noTrx()) {
			Schema schema = FieldUtil.createMinimalValidSchema();

			assertThat(searchProvider).recordedStoreEvents(0);
			Future<Schema> future = getClient().createSchema(schema);
			latchFor(future);
			assertSuccess(future);
			assertThat(searchProvider).recordedStoreEvents(1);
			Schema restSchema = future.result();
			assertThat(schema).matches(restSchema);
			assertThat(restSchema.getPermissions()).isNotEmpty().contains("create", "read", "update", "delete");

			SchemaContainer schemaContainer = boot.schemaContainerRoot().findByUuid(restSchema.getUuid()).toBlocking().first();
			assertNotNull(schemaContainer);
			assertEquals("Name does not match with the requested name", schema.getName(), schemaContainer.getName());
			// assertEquals("Description does not match with the requested description", request.getDescription(), schema.getDescription());
			// assertEquals("There should be exactly one property schema.", 1, schema.getPropertyTypes().size());
		}
	}

	@Test
	@Override
	public void testCreateReadDelete() throws GenericRestException, Exception {

		try (NoTrx noTx = db.noTrx()) {
			assertThat(searchProvider).recordedStoreEvents(0);
			Schema schema = FieldUtil.createMinimalValidSchema();

			Future<Schema> createFuture = getClient().createSchema(schema);
			latchFor(createFuture);
			assertSuccess(createFuture);
			assertThat(searchProvider).recordedStoreEvents(1);
			Schema restSchema = createFuture.result();
			assertThat(schema).matches(restSchema);
			assertElement(boot.meshRoot().getSchemaContainerRoot(), restSchema.getUuid(), true);
			// assertEquals("There should be exactly one property schema.", 1, schema.getPropertyTypes().size());

			Future<Schema> readFuture = getClient().findSchemaByUuid(restSchema.getUuid());
			latchFor(readFuture);
			assertSuccess(readFuture);

			Future<GenericMessageResponse> deleteFuture = getClient().deleteSchema(restSchema.getUuid());
			latchFor(deleteFuture);
			assertSuccess(deleteFuture);
			expectResponseMessage(deleteFuture, "schema_deleted", restSchema.getUuid() + "/" + restSchema.getName());
			// TODO actually also the used nodes should have been deleted
			assertThat(searchProvider).recordedDeleteEvents(1);
			assertThat(searchProvider).recordedStoreEvents(1);
		}

	}

	// Read Tests

	@Test
	@Override
	public void testReadMultiple() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			int totalSchemas;
			SchemaContainerRoot schemaRoot = meshRoot().getSchemaContainerRoot();
			final int nSchemas = 22;
			Schema schema = new SchemaModel();
			schema.setName("No Perm Schema");
			schema.setDisplayField("name");
			SchemaContainer noPermSchema = schemaRoot.create(schema, user());
			Schema dummySchema = new SchemaModel();
			dummySchema.setName("dummy");
			noPermSchema.getLatestVersion().setSchema(dummySchema);
			for (int i = 0; i < nSchemas; i++) {
				schema = new SchemaModel();
				schema.setName("extra_schema_" + i);
				schema.setDisplayField("name");
				SchemaContainer extraSchema = schemaRoot.create(schema, user());
				extraSchema.getLatestVersion().setSchema(dummySchema);
				role().grantPermissions(extraSchema, READ_PERM);
			}
			// Don't grant permissions to no perm schema
			totalSchemas = nSchemas + schemaContainers().size();
			// Test default paging parameters
			Future<SchemaListResponse> future = getClient().findSchemas();
			latchFor(future);
			assertSuccess(future);
			SchemaListResponse restResponse = future.result();
			assertEquals(25, restResponse.getMetainfo().getPerPage());
			assertEquals(1, restResponse.getMetainfo().getCurrentPage());
			assertEquals(25, restResponse.getData().size());

			int perPage = 11;
			future = getClient().findSchemas(new PagingParameters(2, perPage));
			latchFor(future);
			assertSuccess(future);
			restResponse = future.result();
			assertEquals(perPage, restResponse.getData().size());

			// Extra schemas + default schema
			int totalPages = (int) Math.ceil(totalSchemas / (double) perPage);
			assertEquals("The response did not contain the correct amount of items", 11, restResponse.getData().size());
			assertEquals(2, restResponse.getMetainfo().getCurrentPage());
			assertEquals(totalPages, restResponse.getMetainfo().getPageCount());
			assertEquals(perPage, restResponse.getMetainfo().getPerPage());
			assertEquals(totalSchemas, restResponse.getMetainfo().getTotalCount());

			List<Schema> allSchemas = new ArrayList<>();
			for (int page = 1; page <= totalPages; page++) {
				Future<SchemaListResponse> pageFuture = getClient().findSchemas(new PagingParameters(page, perPage));
				latchFor(pageFuture);
				assertSuccess(pageFuture);

				restResponse = pageFuture.result();
				allSchemas.addAll(restResponse.getData());
			}
			assertEquals("Somehow not all schemas were loaded when loading all pages.", totalSchemas, allSchemas.size());

			// Verify that the no perm schema is not part of the response
			// final String noPermSchemaName = noPermSchema.getName();
			// List<Schema> filteredSchemaList = allSchemas.parallelStream().filter(restSchema -> restSchema.getName().equals(noPermSchemaName))
			// .collect(Collectors.toList());
			// assertTrue("The no perm schema should not be part of the list since no permissions were added.", filteredSchemaList.size() == 0);

			future = getClient().findSchemas(new PagingParameters(-1, perPage));
			latchFor(future);
			expectException(future, BAD_REQUEST, "error_page_parameter_must_be_positive", "-1");

			future = getClient().findSchemas(new PagingParameters(1, -1));
			latchFor(future);
			expectException(future, BAD_REQUEST, "error_pagesize_parameter", "-1");

			future = getClient().findSchemas(new PagingParameters(4242, 25));
			latchFor(future);
			assertSuccess(future);

			SchemaListResponse list = future.result();
			assertEquals(4242, list.getMetainfo().getCurrentPage());
			assertEquals(0, list.getData().size());
		}
	}

	@Test
	public void testReadMetaCountOnly() {
		Future<SchemaListResponse> future = getClient().findSchemas(new PagingParameters(1, 0));
		latchFor(future);
		assertSuccess(future);
		assertEquals(0, future.result().getData().size());
	}

	@Test
	@Override
	public void testReadByUUID() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			SchemaContainer schemaContainer = schemaContainer("content");
			Future<Schema> future = getClient().findSchemaByUuid(schemaContainer.getUuid());
			latchFor(future);
			assertSuccess(future);
			Schema restSchema = future.result();
			assertThat(restSchema).matches(schemaContainer);
		}
	}

	@Test
	@Override
	public void testReadByUuidWithRolePerms() {
		String uuid = db.noTrx(() -> schemaContainer("content").getUuid());

		Future<Schema> future = getClient().findSchemaByUuid(uuid, new RolePermissionParameters().setRoleUuid(db.noTrx(() -> role().getUuid())));
		latchFor(future);
		assertSuccess(future);
		assertNotNull(future.result().getRolePerms());
		assertEquals(6, future.result().getRolePerms().length);
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		SchemaContainer schema;
		try (NoTrx noTx = db.noTrx()) {
			schema = schemaContainer("content");

			role().grantPermissions(schema, DELETE_PERM);
			role().grantPermissions(schema, UPDATE_PERM);
			role().grantPermissions(schema, CREATE_PERM);
			role().revokePermissions(schema, READ_PERM);
		}

		try (NoTrx noTx = db.noTrx()) {
			Future<Schema> future = getClient().findSchemaByUuid(schema.getUuid());
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", schema.getUuid());
		}
	}

	@Test
	public void testReadSchemaByInvalidUUID() throws Exception {
		Future<Schema> future = getClient().findSchemaByUuid("bogus");
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	// Update Tests

	@Test
	@Override
	@Ignore("Update tests are covered by dedicated test class")
	public void testUpdate() throws GenericRestException, Exception {

	}

	@Test
	public void testCreateWithConflictingName() {
		String name = "folder";
		Schema request = new SchemaModel();
		request.setSegmentField("name");
		request.getFields().add(FieldUtil.createStringFieldSchema("name").setRequired(true));
		request.setDisplayField("name");
		request.setName(name);

		Future<Schema> future = getClient().createSchema(request);
		latchFor(future);
		expectException(future, CONFLICT, "schema_conflicting_name", name);
	}

	@Test
	public void testUpdateWithBogusUuid() throws GenericRestException, Exception {
		try (NoTrx noTx = db.noTrx()) {
			SchemaContainer schema = schemaContainer("content");
			String oldName = schema.getName();
			Schema request = new SchemaModel();
			request.setName("new-name");

			Future<GenericMessageResponse> future = getClient().updateSchema("bogus", request);
			latchFor(future);
			expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");

			SchemaContainer reloaded = boot.schemaContainerRoot().findByUuid(schema.getUuid()).toBlocking().single();
			assertEquals("The name should not have been changed.", oldName, reloaded.getName());
		}
	}

	// Delete Tests

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			SchemaContainer schema = schemaContainer("content");
			assertThat(schema.getNodes()).isNotEmpty();

			String name = schema.getUuid() + "/" + schema.getName();
			String uuid = schema.getUuid();
			Future<GenericMessageResponse> future = getClient().deleteSchema(schema.getUuid());
			latchFor(future);

			expectException(future, BAD_REQUEST, "schema_delete_still_in_use", uuid);

			SchemaContainer reloaded = boot.schemaContainerRoot().findByUuid(uuid).toBlocking().single();
			assertNotNull("The schema should not have been deleted.", reloaded);

			// Validate and delete all remaining nodes that use the schema
			assertThat(reloaded.getNodes()).isNotEmpty();
			SearchQueueBatch batch = createBatch();
			for (Node node : reloaded.getNodes()) {
				node.delete(batch);
			}

			future = getClient().deleteSchema(schema.getUuid());
			latchFor(future);
			assertSuccess(future);
			expectResponseMessage(future, "schema_deleted", name);

			boot.schemaContainerRoot().reload();
			reloaded = boot.schemaContainerRoot().findByUuid(uuid).toBlocking().single();
			assertNull("The schema should have been deleted.", reloaded);
		}
	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			SchemaContainer schema = schemaContainer("content");
			role().revokePermissions(schema, DELETE_PERM);

			Future<GenericMessageResponse> future = getClient().deleteSchema(schema.getUuid());
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", schema.getUuid());

			assertElement(boot.schemaContainerRoot(), schema.getUuid(), true);
		}
	}

	@Test
	@Override
	@Ignore("not yet supported")
	public void testUpdateMultithreaded() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			SchemaContainer schema = schemaContainer("content");
			Schema request = new SchemaModel();
			request.setName("new-name");

			int nJobs = 5;
			CyclicBarrier barrier = prepareBarrier(nJobs);
			Set<Future<?>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(getClient().updateSchema(schema.getUuid(), request));
			}
			validateSet(set, barrier);
		}
	}

	@Test
	@Override
	@Ignore("not yet supported")
	public void testReadByUuidMultithreaded() throws Exception {
		int nJobs = 10;
		try (NoTrx noTx = db.noTrx()) {
			SchemaContainer schema = schemaContainer("content");
			String uuid = schema.getUuid();
			CyclicBarrier barrier = prepareBarrier(nJobs);
			Set<Future<?>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(getClient().findSchemaByUuid(uuid));
			}
			validateSet(set, barrier);
		}
	}

	@Test
	@Override
	@Ignore("not yet supported")
	public void testDeleteByUUIDMultithreaded() throws Exception {
		int nJobs = 3;
		try (NoTrx noTx = db.noTrx()) {
			SchemaContainer schema = schemaContainer("content");
			CyclicBarrier barrier = prepareBarrier(nJobs);
			Set<Future<GenericMessageResponse>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(getClient().deleteSchema(schema.getUuid()));
			}
			validateDeletion(set, barrier);
		}
	}

	@Test
	@Override
	@Ignore("not yet supported")
	public void testCreateMultithreaded() throws Exception {
		int nJobs = 5;
		Schema request = new SchemaModel();
		request.setName("new schema name");
		request.setDisplayField("name");

		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<Future<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().createSchema(request));
		}
		validateCreation(set, barrier);
	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws Exception {
		int nJobs = 200;
		try (NoTrx noTx = db.noTrx()) {
			SchemaContainer schema = schemaContainer("content");
			Set<Future<Schema>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(getClient().findSchemaByUuid(schema.getUuid()));
			}
			for (Future<Schema> future : set) {
				latchFor(future);
				assertSuccess(future);
			}
		}
	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() throws Exception {
		String schemaUuid;
		try (NoTrx noTx = db.noTrx()) {
			SchemaContainer schema = schemaContainer("content");
			role().revokePermissions(schema, UPDATE_PERM);
			schemaUuid = schema.getUuid();
		}

		try (NoTrx noTx = db.noTrx()) {
			Schema request = new SchemaModel();
			request.setName("new-name");

			Future<GenericMessageResponse> future = getClient().updateSchema(schemaUuid, request);
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", schemaUuid);
		}

	}

}