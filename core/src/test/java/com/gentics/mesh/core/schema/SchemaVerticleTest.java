package com.gentics.mesh.core.schema;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.util.MeshAssert.assertElement;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaResponse;
import com.gentics.mesh.core.rest.schema.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.core.verticle.schema.SchemaVerticle;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.test.AbstractBasicCrudVerticleTest;

import io.vertx.core.Future;

public class SchemaVerticleTest extends AbstractBasicCrudVerticleTest {

	@Autowired
	private SchemaVerticle verticle;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	// Create Tests

	@Test
	@Override
	public void testCreate() throws HttpStatusCodeErrorException, Exception {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("new schema name");
		request.setDisplayField("name");
		Future<SchemaResponse> future = getClient().createSchema(request);
		latchFor(future);
		assertSuccess(future);
		SchemaResponse restSchemaResponse = future.result();
		test.assertSchema(request, restSchemaResponse);

		CountDownLatch latch = new CountDownLatch(1);
		boot.schemaContainerRoot().findByUuid(restSchemaResponse.getUuid(), rh -> {
			SchemaContainer schemaContainer = rh.result();
			assertNotNull(schemaContainer);
			assertEquals("Name does not match with the requested name", request.getName(), schemaContainer.getName());
			// assertEquals("Description does not match with the requested description", request.getDescription(), schema.getDescription());
			// assertEquals("There should be exactly one property schema.", 1, schema.getPropertyTypes().size());
			latch.countDown();
		});
		failingLatch(latch);

	}

	@Test
	@Override
	public void testCreateReadDelete() throws HttpStatusCodeErrorException, Exception {

		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("new schema name");
		request.setDisplayField("name");
		Future<SchemaResponse> createFuture = getClient().createSchema(request);
		latchFor(createFuture);
		assertSuccess(createFuture);
		SchemaResponse restSchema = createFuture.result();
		test.assertSchema(request, restSchema);

		assertElement(boot.meshRoot().getSchemaContainerRoot(), restSchema.getUuid(), true);
		// test.assertSchema(schema, restSchema);
		// assertEquals("There should be exactly one property schema.", 1, schema.getPropertyTypes().size());

		Future<SchemaResponse> readFuture = getClient().findSchemaByUuid(restSchema.getUuid());
		latchFor(readFuture);
		assertSuccess(readFuture);

		Future<GenericMessageResponse> deleteFuture = getClient().deleteSchema(restSchema.getUuid());
		latchFor(deleteFuture);
		assertSuccess(deleteFuture);
		expectMessageResponse("schema_deleted", deleteFuture, restSchema.getUuid() + "/" + restSchema.getName());

	}

	// Read Tests

	@Test
	@Override
	public void testReadMultiple() throws Exception {
		int totalSchemas;
		SchemaContainerRoot schemaRoot = meshRoot().getSchemaContainerRoot();
		final int nSchemas = 22;
		Schema schema = new SchemaImpl();
		schema.setName("No Perm Schema");
		schema.setDisplayField("name");
		SchemaContainer noPermSchema = schemaRoot.create(schema, user());
		Schema dummySchema = new SchemaImpl();
		dummySchema.setName("dummy");
		noPermSchema.setSchema(dummySchema);
		for (int i = 0; i < nSchemas; i++) {
			schema = new SchemaImpl();
			schema.setName("extra_schema_" + i);
			schema.setDisplayField("name");
			SchemaContainer extraSchema = schemaRoot.create(schema, user());
			extraSchema.setSchema(dummySchema);
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
		future = getClient().findSchemas(new PagingInfo(2, perPage));
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

		List<SchemaResponse> allSchemas = new ArrayList<>();
		for (int page = 1; page <= totalPages; page++) {
			Future<SchemaListResponse> pageFuture = getClient().findSchemas(new PagingInfo(page, perPage));
			latchFor(pageFuture);
			assertSuccess(pageFuture);

			restResponse = pageFuture.result();
			allSchemas.addAll(restResponse.getData());
		}
		assertEquals("Somehow not all schemas were loaded when loading all pages.", totalSchemas, allSchemas.size());

		// Verify that the no perm schema is not part of the response
		// final String noPermSchemaName = noPermSchema.getName();
		// List<SchemaResponse> filteredSchemaList = allSchemas.parallelStream().filter(restSchema -> restSchema.getName().equals(noPermSchemaName))
		// .collect(Collectors.toList());
		// assertTrue("The no perm schema should not be part of the list since no permissions were added.", filteredSchemaList.size() == 0);

		future = getClient().findSchemas(new PagingInfo(-1, perPage));
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_invalid_paging_parameters");

		future = getClient().findSchemas(new PagingInfo(1, 0));
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_invalid_paging_parameters");

		future = getClient().findSchemas(new PagingInfo(1, -1));
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_invalid_paging_parameters");

		future = getClient().findSchemas(new PagingInfo(4242, 25));
		latchFor(future);
		assertSuccess(future);

		SchemaListResponse list = future.result();
		assertEquals(4242, list.getMetainfo().getCurrentPage());
		assertEquals(0, list.getData().size());
	}

	@Test
	@Override
	public void testReadByUUID() throws Exception {
		SchemaContainer schemaContainer = schemaContainer("content");
		Future<SchemaResponse> future = getClient().findSchemaByUuid(schemaContainer.getUuid());
		latchFor(future);
		assertSuccess(future);
		SchemaResponse restSchema = future.result();
		test.assertSchema(schemaContainer, restSchema);
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		SchemaContainer schema;
		schema = schemaContainer("content");

		role().grantPermissions(schema, DELETE_PERM);
		role().grantPermissions(schema, UPDATE_PERM);
		role().grantPermissions(schema, CREATE_PERM);
		role().revokePermissions(schema, READ_PERM);
		Future<SchemaResponse> future = getClient().findSchemaByUuid(schema.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", schema.getUuid());
	}

	@Test
	public void testReadSchemaByInvalidUUID() throws Exception {
		Future<SchemaResponse> future = getClient().findSchemaByUuid("bogus");
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	// Update Tests

	@Test
	@Override
	public void testUpdate() throws HttpStatusCodeErrorException, Exception {
		String name = "new-name";
		SchemaContainer schema = schemaContainer("content");
		SchemaUpdateRequest request = new SchemaUpdateRequest();
		request.setName(name);

		Future<SchemaResponse> future = getClient().updateSchema(schema.getUuid(), request);
		latchFor(future);
		assertSuccess(future);
		SchemaResponse restSchema = future.result();
		assertEquals(request.getName(), restSchema.getName());
		schema.reload();
		assertEquals("The name of the schema was not updated", name, schema.getName());
		CountDownLatch latch = new CountDownLatch(1);
		boot.schemaContainerRoot().findByUuid(schema.getUuid(), rh -> {
			SchemaContainer reloaded = rh.result();
			assertEquals("The name should have been updated", name, reloaded.getName());
			latch.countDown();
		});
		failingLatch(latch);

	}

	@Test
	public void testUpdateWithConflictingName() {
		String name = "folder";
		String originalSchemaName = "content";
		SchemaContainer schema = schemaContainer(originalSchemaName);
		SchemaUpdateRequest request = new SchemaUpdateRequest();
		request.setName(name);

		Future<SchemaResponse> future = getClient().updateSchema(schema.getUuid(), request);
		latchFor(future);
		expectException(future, BAD_REQUEST, "schema_conflicting_name", name);
		schema.reload();
		assertEquals("The name of the schema was updated", originalSchemaName, schema.getName());

	}

	@Test
	public void testUpdateWithBogusUuid() throws HttpStatusCodeErrorException, Exception {
		SchemaContainer schema = schemaContainer("content");
		String oldName = schema.getName();
		SchemaUpdateRequest request = new SchemaUpdateRequest();
		request.setName("new-name");

		Future<SchemaResponse> future = getClient().updateSchema("bogus", request);
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");

		CountDownLatch latch = new CountDownLatch(1);
		boot.schemaContainerRoot().findByUuid(schema.getUuid(), rh -> {
			SchemaContainer reloaded = rh.result();
			assertEquals("The name should not have been changed.", oldName, reloaded.getName());
			latch.countDown();
		});
		failingLatch(latch);
	}

	// Delete Tests

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {
		SchemaContainer schema = schemaContainer("content");

		Future<GenericMessageResponse> future = getClient().deleteSchema(schema.getUuid());
		latchFor(future);
		assertSuccess(future);
		expectMessageResponse("schema_deleted", future, schema.getUuid() + "/" + schema.getName());

		boot.schemaContainerRoot().findByUuid(schema.getUuid(), rh -> {
			SchemaContainer reloaded = rh.result();
			assertNull("The schema should have been deleted.", reloaded);
		});
	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		SchemaContainer schema;
		schema = schemaContainer("content");
		role().revokePermissions(schema, DELETE_PERM);

		Future<GenericMessageResponse> future = getClient().deleteSchema(schema.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", schema.getUuid());

		assertElement(boot.schemaContainerRoot(), schema.getUuid(), true);

	}

	@Test
	@Override
	@Ignore("not yet supported")
	public void testUpdateMultithreaded() throws Exception {
		SchemaContainer schema = schemaContainer("content");
		SchemaUpdateRequest request = new SchemaUpdateRequest();
		request.setName("new-name");

		int nJobs = 5;
		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<Future<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().updateSchema(schema.getUuid(), request));
		}
		validateSet(set, barrier);
	}

	@Test
	@Override
	@Ignore("not yet supported")
	public void testReadByUuidMultithreaded() throws Exception {
		int nJobs = 10;
		SchemaContainer schema = schemaContainer("content");
		String uuid = schema.getUuid();
		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<Future<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().findSchemaByUuid(uuid));
		}
		validateSet(set, barrier);
	}

	@Test
	@Override
	@Ignore("not yet supported")
	public void testDeleteByUUIDMultithreaded() throws Exception {
		int nJobs = 3;
		SchemaContainer schema = schemaContainer("content");
		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<Future<GenericMessageResponse>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().deleteSchema(schema.getUuid()));
		}
		validateDeletion(set, barrier);
	}

	@Test
	@Override
	@Ignore("not yet supported")
	public void testCreateMultithreaded() throws Exception {
		int nJobs = 5;
		SchemaCreateRequest request = new SchemaCreateRequest();
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
		SchemaContainer schema = schemaContainer("content");
		Set<Future<SchemaResponse>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().findSchemaByUuid(schema.getUuid()));
		}
		for (Future<SchemaResponse> future : set) {
			latchFor(future);
			assertSuccess(future);
		}
	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() throws Exception {
		SchemaContainer schema = schemaContainer("content");
		role().revokePermissions(schema, UPDATE_PERM);

		SchemaUpdateRequest request = new SchemaUpdateRequest();
		request.setName("new-name");

		Future<SchemaResponse> future = getClient().updateSchema(schema.getUuid(), request);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", schema.getUuid());

	}

}