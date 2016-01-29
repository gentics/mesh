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
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaResponse;
import com.gentics.mesh.core.rest.schema.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.core.verticle.schema.SchemaVerticle;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.query.impl.RolePermissionParameter;
import com.gentics.mesh.test.AbstractBasicCrudVerticleTest;
import com.gentics.mesh.util.FieldUtil;

import io.vertx.core.Future;

public class SchemaVerticleTest extends AbstractBasicCrudVerticleTest {

	@Autowired
	private SchemaVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
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
		request.setSegmentField("name");

		assertThat(searchProvider).recordedStoreEvents(0);
		Future<SchemaResponse> future = getClient().createSchema(request);
		latchFor(future);
		assertSuccess(future);
		assertThat(searchProvider).recordedStoreEvents(1);
		SchemaResponse restSchemaResponse = future.result();
		test.assertSchema(request, restSchemaResponse);

		SchemaContainer schemaContainer = boot.schemaContainerRoot().findByUuid(restSchemaResponse.getUuid()).toBlocking().first();
		assertNotNull(schemaContainer);
		assertEquals("Name does not match with the requested name", request.getName(), schemaContainer.getName());
		// assertEquals("Description does not match with the requested description", request.getDescription(), schema.getDescription());
		// assertEquals("There should be exactly one property schema.", 1, schema.getPropertyTypes().size());

	}

	@Test
	@Override
	public void testCreateReadDelete() throws HttpStatusCodeErrorException, Exception {

		assertThat(searchProvider).recordedStoreEvents(0);
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("new schema name");
		request.setDisplayField("name");
		request.setSegmentField("name");
		Future<SchemaResponse> createFuture = getClient().createSchema(request);
		latchFor(createFuture);
		assertSuccess(createFuture);
		assertThat(searchProvider).recordedStoreEvents(1);
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
		// TODO actually also the used nodes should have been deleted
		assertThat(searchProvider).recordedDeleteEvents(1);
		assertThat(searchProvider).recordedStoreEvents(1);

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
		future = getClient().findSchemas(new PagingParameter(2, perPage));
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
			Future<SchemaListResponse> pageFuture = getClient().findSchemas(new PagingParameter(page, perPage));
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

		future = getClient().findSchemas(new PagingParameter(-1, perPage));
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_page_parameter_must_be_positive", "-1");

		future = getClient().findSchemas(new PagingParameter(1, -1));
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_pagesize_parameter", "-1");

		future = getClient().findSchemas(new PagingParameter(4242, 25));
		latchFor(future);
		assertSuccess(future);

		SchemaListResponse list = future.result();
		assertEquals(4242, list.getMetainfo().getCurrentPage());
		assertEquals(0, list.getData().size());
	}

	@Test
	public void testReadMetaCountOnly() {
		Future<SchemaListResponse> future = getClient().findSchemas(new PagingParameter(1, 0));
		latchFor(future);
		assertSuccess(future);
		assertEquals(0, future.result().getData().size());
	}

	@Test
	@Override
	public void testReadByUUID() throws Exception {
		SchemaContainer schemaContainer = schemaContainer("content");
		Future<SchemaResponse> future = getClient().findSchemaByUuid(schemaContainer.getUuid());
		latchFor(future);
		assertSuccess(future);
		SchemaResponse restSchema = future.result();
		assertThat(restSchema).matches(schemaContainer);
	}

	@Test
	@Override
	public void testReadByUuidWithRolePerms() {
		SchemaContainer schemaContainer = schemaContainer("content");
		String uuid = schemaContainer.getUuid();

		Future<SchemaResponse> future = getClient().findSchemaByUuid(uuid, new RolePermissionParameter().setRoleUuid(role().getUuid()));
		latchFor(future);
		assertSuccess(future);
		assertNotNull(future.result().getRolePerms());
		assertEquals(4, future.result().getRolePerms().length);
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
		SchemaContainer reloaded = boot.schemaContainerRoot().findByUuid(schema.getUuid()).toBlocking().first();
		assertEquals("The name should have been updated", name, reloaded.getName());

	}

	@Test
	public void testCreateWithConflictingName() {
		String name = "folder";
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setSegmentField("name");
		request.getFields().add(FieldUtil.createStringFieldSchema("name").setRequired(true));
		request.setDisplayField("name");
		request.setName(name);

		Future<SchemaResponse> future = getClient().createSchema(request);
		latchFor(future);
		expectException(future, CONFLICT, "schema_conflicting_name", name);
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
		expectException(future, CONFLICT, "schema_conflicting_name", name);
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

		SchemaContainer reloaded = boot.schemaContainerRoot().findByUuid(schema.getUuid()).toBlocking().single();
		assertEquals("The name should not have been changed.", oldName, reloaded.getName());
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

		SchemaContainer reloaded = boot.schemaContainerRoot().findByUuid(schema.getUuid()).toBlocking().single();
		assertNull("The schema should have been deleted.", reloaded);
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