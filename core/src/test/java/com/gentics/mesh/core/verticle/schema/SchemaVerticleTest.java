package com.gentics.mesh.core.verticle.schema;

import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import io.vertx.core.Future;

import java.util.ArrayList;
import java.util.List;

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
import com.gentics.mesh.core.verticle.SchemaVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class SchemaVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private SchemaVerticle schemaVerticle;

	@Override
	public AbstractWebVerticle getVerticle() {
		return schemaVerticle;
	}

	// Create Tests

	@Test
	public void testCreateSimpleSchema() throws HttpStatusCodeErrorException, Exception {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("new schema name");

		Future<SchemaResponse> future = getClient().createSchema(request);
		latchFor(future);
		assertSuccess(future);
		SchemaResponse restSchemaResponse = future.result();
		test.assertSchema(request, restSchemaResponse);
		boot.schemaContainerRoot().findByUuid(restSchemaResponse.getUuid(), rh -> {
			SchemaContainer schemaContainer = rh.result();
			assertNotNull(schemaContainer);
			assertEquals("Name does not match with the requested name", request.getName(), schemaContainer.getName());
			// assertEquals("Description does not match with the requested description", request.getDescription(), schema.getDescription());
			// assertEquals("There should be exactly one property schema.", 1, schema.getPropertyTypes().size());
			});

	}

	@Test
	public void testCreateDeleteSimpleSchema() throws HttpStatusCodeErrorException, Exception {

		SchemaCreateRequest request = new SchemaCreateRequest();
		// request.setDescription("new description");
		request.setName("new schema name");
		// request.setProjectUuid(data().getProject().getUuid());
		// PropertyTypeSchemaResponse propertySchema = new PropertyTypeSchemaResponse();
		// propertySchema.setKey("extra-content");
		// propertySchema.setType("html");
		// propertySchema.setDescription("Some extra content");
		// request.getPropertyTypeSchemas().add(propertySchema);
		//
		Future<SchemaResponse> createFuture = getClient().createSchema(request);
		latchFor(createFuture);
		assertSuccess(createFuture);
		SchemaResponse restSchema = createFuture.result();
		test.assertSchema(request, restSchema);

		// Verify that the object was created
		data().getMeshRoot().getSchemaContainerRoot().findByUuid(restSchema.getUuid(), rh -> {
			SchemaContainer schemaContainer = rh.result();
			assertNotNull(schemaContainer);
		});
		// test.assertSchema(schema, restSchema);
		// assertEquals("There should be exactly one property schema.", 1, schema.getPropertyTypes().size());

		Future<GenericMessageResponse> deleteFuture = getClient().deleteSchema(restSchema.getUuid());
		latchFor(deleteFuture);
		assertSuccess(deleteFuture);
		expectMessageResponse("schema_deleted", deleteFuture, restSchema.getUuid() + "/" + restSchema.getName());

	}

	// Read Tests

	@Test
	public void testReadAllSchemaList() throws Exception {

		SchemaContainerRoot schemaRoot = data().getMeshRoot().getSchemaContainerRoot();
		final int nSchemas = 22;
		Schema schema = new SchemaImpl();
		schema.setName("No Perm Schema");
		SchemaContainer noPermSchema = schemaRoot.create(schema);
		Schema dummySchema = new SchemaImpl();
		dummySchema.setName("dummy");
		noPermSchema.setSchema(dummySchema);
		for (int i = 0; i < nSchemas; i++) {
			schema = new SchemaImpl();
			schema.setName("extra_schema_" + i);
			SchemaContainer extraSchema = schemaRoot.create(schema);
			extraSchema.setSchema(dummySchema);
			role().addPermissions(extraSchema, READ_PERM);
		}
		// Don't grant permissions to no perm schema

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
		int totalSchemas = nSchemas + data().getSchemaContainers().size();
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
	public void testReadSchemaByUUID() throws Exception {

		SchemaContainer schemaContainer = schemaContainer("content");
		Future<SchemaResponse> future = getClient().findSchemaByUuid(schemaContainer.getUuid());
		latchFor(future);
		assertSuccess(future);
		SchemaResponse restSchema = future.result();
		test.assertSchema(schemaContainer, restSchema);
	}

	@Test
	public void testReadSchemaByUUIDWithNoPerm() throws Exception {
		SchemaContainer schema = schemaContainer("content");

		role().addPermissions(schema, DELETE_PERM);
		role().addPermissions(schema, UPDATE_PERM);
		role().addPermissions(schema, CREATE_PERM);
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
	public void testUpdateSchemaByUUID() throws HttpStatusCodeErrorException, Exception {
		SchemaContainer schema = schemaContainer("content");
		SchemaUpdateRequest request = new SchemaUpdateRequest();
		request.setName("new-name");

		Future<SchemaResponse> future = getClient().updateSchema(schema.getUuid(), request);
		latchFor(future);
		assertSuccess(future);
		SchemaResponse restSchema = future.result();
		// assertEquals(request.getName(), restSchema.getName());

		boot.schemaContainerRoot().findByUuid(schema.getUuid(), rh -> {
			SchemaContainer reloaded = rh.result();
			// assertEquals("The name should have been updated", "new-name", reloaded.getName());
			});

	}

	@Test
	public void testUpdateSchemaByBogusUUID() throws HttpStatusCodeErrorException, Exception {
		SchemaContainer schema = schemaContainer("content");

		String oldName = schema.getName();
		SchemaUpdateRequest request = new SchemaUpdateRequest();
		request.setName("new-name");

		Future<SchemaResponse> future = getClient().updateSchema("bogus", request);
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");

		boot.schemaContainerRoot().findByUuid(schema.getUuid(), rh -> {
			SchemaContainer reloaded = rh.result();
			assertEquals("The name should not have been changed.", oldName, reloaded.getName());
		});

	}

	// Delete Tests

	@Test
	public void testDeleteSchemaByUUID() throws Exception {
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

	public void testDeleteSchemaWithMissingPermission() throws Exception {
		SchemaContainer schema = schemaContainer("content");
		Future<GenericMessageResponse> future = getClient().deleteSchema(schema.getUuid());
		latchFor(future);
		assertSuccess(future);

		fail("unspecified test");
		String json = "error";
		// assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		boot.schemaContainerRoot().findByUuid(schema.getUuid(), rh -> {
			SchemaContainer reloaded = rh.result();
			assertNotNull("The schema should not have been deleted.", reloaded);
		});

	}
}
