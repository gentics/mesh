package com.gentics.mesh.core.schema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.call;
import static com.gentics.mesh.test.context.MeshTestHelper.expectException;
import static com.gentics.mesh.test.context.MeshTestHelper.prepareBarrier;
import static com.gentics.mesh.test.context.MeshTestHelper.validateCreation;
import static com.gentics.mesh.test.context.MeshTestHelper.validateDeletion;
import static com.gentics.mesh.test.context.MeshTestHelper.validateSet;
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
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.ferma.Tx;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.RolePermissionParametersImpl;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.definition.BasicRestTestcases;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class SchemaEndpointTest extends AbstractMeshTest implements BasicRestTestcases {

	@Test
	@Override
	public void testCreate() throws GenericRestException, Exception {
		SchemaCreateRequest createRequest = FieldUtil.createMinimalValidSchemaCreateRequest();

		assertThat(dummySearchProvider()).hasEvents(0, 0, 0, 0);
		SchemaResponse restSchema = call(() -> client().createSchema(createRequest));
		assertThat(dummySearchProvider()).hasEvents(1, 0, 0, 0);
		assertThat(dummySearchProvider()).hasStore(SchemaContainer.composeIndexName(), SchemaContainer.composeIndexType(),
				SchemaContainer.composeDocumentId(restSchema.getUuid()));
		try (Tx tx = db().tx()) {
			assertThat(createRequest).matches(restSchema);
			assertThat(restSchema.getPermissions()).hasPerm(CREATE, READ, UPDATE, DELETE);

			SchemaContainer schemaContainer = boot().schemaContainerRoot().findByUuid(restSchema.getUuid());
			assertNotNull(schemaContainer);
			assertEquals("Name does not match with the requested name", createRequest.getName(), schemaContainer.getName());
			// assertEquals("Description does not match with the requested description", request.getDescription(), schema.getDescription());
			// assertEquals("There should be exactly one property schema.", 1, schema.getPropertyTypes().size());
		}
	}

	@Test
	@Override
	public void testCreateWithNoPerm() throws Exception {
		SchemaCreateRequest schema = FieldUtil.createMinimalValidSchemaCreateRequest();
		String schemaRootUuid = db().tx(() -> meshRoot().getSchemaContainerRoot().getUuid());
		try (Tx tx = db().tx()) {
			role().revokePermissions(meshRoot().getSchemaContainerRoot(), CREATE_PERM);
		}
		call(() -> client().createSchema(schema), FORBIDDEN, "error_missing_perm", schemaRootUuid);
	}

	@Test
	@Override
	public void testCreateReadDelete() throws GenericRestException, Exception {

		try (Tx tx = db().tx()) {
			assertThat(dummySearchProvider()).hasEvents(0, 0, 0, 0);
			SchemaCreateRequest schema = FieldUtil.createMinimalValidSchemaCreateRequest();

			SchemaResponse restSchema = call(() -> client().createSchema(schema));
			assertThat(dummySearchProvider()).hasEvents(1, 0, 0, 0);
			assertThat(schema).matches(restSchema);
			assertElement(boot().meshRoot().getSchemaContainerRoot(), restSchema.getUuid(), true);
			call(() -> client().findSchemaByUuid(restSchema.getUuid()));

			dummySearchProvider().clear();
			call(() -> client().deleteSchema(restSchema.getUuid()));
			// Only schemas which are not in use can be delete and also removed from the index
			assertThat(dummySearchProvider()).hasEvents(0, 1, 0, 0);
		}

	}

	@Test
	@Override
	public void testReadMultiple() throws Exception {
		try (Tx tx = db().tx()) {
			int totalSchemas;
			SchemaContainerRoot schemaRoot = meshRoot().getSchemaContainerRoot();
			final int nSchemas = 22;
			SchemaModel schema = FieldUtil.createMinimalValidSchema();
			schema.setName("No_Perm_Schema");
			SchemaContainer noPermSchema = schemaRoot.create(schema, user());
			SchemaModel dummySchema = new SchemaModelImpl();
			dummySchema.setName("dummy");
			noPermSchema.getLatestVersion().setSchema(dummySchema);
			for (int i = 0; i < nSchemas; i++) {
				schema = FieldUtil.createMinimalValidSchema();
				schema.setName("extra_schema_" + i);
				SchemaContainer extraSchema = schemaRoot.create(schema, user());
				extraSchema.getLatestVersion().setSchema(dummySchema);
				role().grantPermissions(extraSchema, READ_PERM);
			}
			// Don't grant permissions to no perm schema
			totalSchemas = nSchemas + data().getSchemaContainers().size();
			// Test default paging parameters
			MeshResponse<SchemaListResponse> future = client().findSchemas().invoke();
			latchFor(future);
			assertSuccess(future);
			SchemaListResponse restResponse = future.result();
			assertEquals(25, restResponse.getMetainfo().getPerPage());
			assertEquals(1, restResponse.getMetainfo().getCurrentPage());
			assertEquals(25, restResponse.getData().size());

			int perPage = 11;
			future = client().findSchemas(new PagingParametersImpl(2, perPage)).invoke();
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
				MeshResponse<SchemaListResponse> pageFuture = client().findSchemas(new PagingParametersImpl(page, perPage)).invoke();
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

			future = client().findSchemas(new PagingParametersImpl(-1, perPage)).invoke();
			latchFor(future);
			expectException(future, BAD_REQUEST, "error_page_parameter_must_be_positive", "-1");

			future = client().findSchemas(new PagingParametersImpl(1, -1)).invoke();
			latchFor(future);
			expectException(future, BAD_REQUEST, "error_pagesize_parameter", "-1");

			future = client().findSchemas(new PagingParametersImpl(4242, 25)).invoke();
			latchFor(future);
			assertSuccess(future);

			SchemaListResponse list = future.result();
			assertEquals(4242, list.getMetainfo().getCurrentPage());
			assertEquals(0, list.getData().size());
		}
	}

	@Test
	public void testReadMetaCountOnly() {
		MeshResponse<SchemaListResponse> future = client().findSchemas(new PagingParametersImpl(1, 0)).invoke();
		latchFor(future);
		assertSuccess(future);
		assertEquals(0, future.result().getData().size());
	}

	@Test
	@Override
	public void testReadByUUID() throws Exception {
		try (Tx tx = db().tx()) {
			SchemaContainer container = schemaContainer("content");
			SchemaContainerVersion schemaContainerVersion = container.getLatestVersion();
			SchemaResponse restSchema = call(() -> client().findSchemaByUuid(container.getUuid()));
			assertThat(restSchema).matches(schemaContainerVersion).isValid();
		}
	}

	@Test
	@Override
	public void testReadByUuidWithRolePerms() {
		String uuid = db().tx(() -> schemaContainer("content").getUuid());

		SchemaResponse schema = call(
				() -> client().findSchemaByUuid(uuid, new RolePermissionParametersImpl().setRoleUuid(db().tx(() -> role().getUuid()))));
		assertNotNull(schema.getRolePerms());
		assertThat(schema.getRolePerms()).hasPerm(Permission.values());
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		SchemaContainer schema;
		try (Tx tx = db().tx()) {
			schema = schemaContainer("content");

			role().grantPermissions(schema, DELETE_PERM);
			role().grantPermissions(schema, UPDATE_PERM);
			role().grantPermissions(schema, CREATE_PERM);
			role().revokePermissions(schema, READ_PERM);
		}

		try (Tx tx = db().tx()) {
			call(() -> client().findSchemaByUuid(schema.getUuid()), FORBIDDEN, "error_missing_perm", schema.getUuid());
		}
	}

	@Test
	public void testReadSchemaByInvalidUUID() throws Exception {
		call(() -> client().findSchemaByUuid("bogus"), NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	@Override
	@Ignore("Update tests are covered by dedicated test class")
	public void testUpdate() throws GenericRestException, Exception {

	}

	@Test
	public void testCreateWithConflictingName() {
		String name = "folder";
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setSegmentField("name");
		request.getFields().add(FieldUtil.createStringFieldSchema("name").setRequired(true));
		request.setDisplayField("name");
		request.setName(name);

		call(() -> client().createSchema(request), CONFLICT, "schema_conflicting_name", name);
	}

	/**
	 * Test automagically assignment of referenced microschemas to projects.
	 */
	@Test
	public void testUpdateWithReferencedMicroschema() {

		SchemaUpdateRequest schemaUpdate = db()
				.tx(() -> JsonUtil.readValue(schemaContainer("content").getLatestVersion().getJson(), SchemaUpdateRequest.class));
		String schemaUuid = db().tx(() -> schemaContainer("content").getUuid());

		// 1. Create the microschema
		MicroschemaCreateRequest microschemaRequest = new MicroschemaCreateRequest();
		microschemaRequest.setName("TestMicroschema");
		microschemaRequest.addField(FieldUtil.createStringFieldSchema("text"));
		microschemaRequest.addField(FieldUtil.createNodeFieldSchema("nodeRef").setAllowedSchemas("content"));
		MicroschemaResponse microschemaResponse = call(() -> client().createMicroschema(microschemaRequest));
		String microschemaUuid = microschemaResponse.getUuid();

		List<MicroschemaResponse> filteredList = call(() -> client().findMicroschemas(PROJECT_NAME)).getData().stream()
				.filter(microschema -> microschema.getUuid().equals(microschemaUuid)).collect(Collectors.toList());

		assertThat(filteredList).isEmpty();

		// 2. Add micronode field to content schema
		schemaUpdate.addField(FieldUtil.createMicronodeFieldSchema("micro").setAllowedMicroSchemas("TestMicroschema"));
		call(() -> client().updateSchema(schemaUuid, schemaUpdate));

		filteredList = call(() -> client().findMicroschemas(PROJECT_NAME)).getData().stream()
				.filter(microschema -> microschema.getUuid().equals(microschemaUuid)).collect(Collectors.toList());
		assertThat(filteredList).hasSize(1);

	}

	@Test
	public void testUpdateWithConflictingName() {
		String name = "newschema";
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setSegmentField("name");
		request.getFields().add(FieldUtil.createStringFieldSchema("name").setRequired(true));
		request.setDisplayField("name");
		request.setName(name);

		SchemaResponse response = call(() -> client().createSchema(request));

		SchemaUpdateRequest updateRequest = new SchemaUpdateRequest();
		updateRequest.setSegmentField("name");
		updateRequest.getFields().add(FieldUtil.createStringFieldSchema("name").setRequired(true));
		updateRequest.setDisplayField("name");
		updateRequest.setName("folder");

		call(() -> client().updateSchema(response.getUuid(), updateRequest), CONFLICT, "schema_conflicting_name", "folder");
	}

	@Test
	public void testUpdateWithBogusUuid() throws GenericRestException, Exception {
		try (Tx tx = db().tx()) {
			SchemaContainer schema = schemaContainer("content");
			String oldName = schema.getName();
			SchemaUpdateRequest request = new SchemaUpdateRequest();
			request.setName("new-name");

			call(() -> client().updateSchema("bogus", request), NOT_FOUND, "object_not_found_for_uuid", "bogus");

			SchemaContainer reloaded = boot().schemaContainerRoot().findByUuid(schema.getUuid());
			assertEquals("The name should not have been changed.", oldName, reloaded.getName());
		}
	}

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {
		try (Tx tx = db().tx()) {
			SchemaContainer schema = schemaContainer("content");
			assertThat(schema.getNodes()).isNotEmpty();
		}

		String uuid = db().tx(() -> schemaContainer("content").getUuid());
		call(() -> client().deleteSchema(uuid), BAD_REQUEST, "schema_delete_still_in_use", uuid);

		try (Tx tx = db().tx()) {
			SchemaContainer reloaded = boot().schemaContainerRoot().findByUuid(uuid);
			assertNotNull("The schema should not have been deleted.", reloaded);
			// Validate and delete all remaining nodes that use the schema
			assertThat(reloaded.getNodes()).isNotEmpty();
			SearchQueueBatch batch = createBatch();
			for (Node node : reloaded.getNodes()) {
				node.delete(batch);
			}
			assertThat(reloaded.getNodes()).isEmpty();
			tx.success();
		}

		call(() -> client().deleteSchema(uuid));

		try (Tx tx = db().tx()) {
			boot().schemaContainerRoot().reload();
			SchemaContainer reloaded = boot().schemaContainerRoot().findByUuid(uuid);
			assertNull("The schema should have been deleted.", reloaded);
		}
	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		try (Tx tx = db().tx()) {
			SchemaContainer schema = schemaContainer("content");
			role().revokePermissions(schema, DELETE_PERM);
			call(() -> client().deleteSchema(schema.getUuid()), FORBIDDEN, "error_missing_perm", schema.getUuid());
			assertElement(boot().schemaContainerRoot(), schema.getUuid(), true);
		}
	}

	@Test
	@Override
	@Ignore("not yet supported")
	public void testUpdateMultithreaded() throws Exception {
		try (Tx tx = db().tx()) {
			SchemaContainer schema = schemaContainer("content");
			SchemaUpdateRequest request = new SchemaUpdateRequest();
			request.setName("new-name");

			int nJobs = 5;
			CyclicBarrier barrier = prepareBarrier(nJobs);
			Set<MeshResponse<?>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(client().updateSchema(schema.getUuid(), request).invoke());
			}
			validateSet(set, barrier);
		}
	}

	@Test
	@Override
	@Ignore("not yet supported")
	public void testReadByUuidMultithreaded() throws Exception {
		int nJobs = 10;
		try (Tx tx = db().tx()) {
			SchemaContainer schema = schemaContainer("content");
			String uuid = schema.getUuid();
			CyclicBarrier barrier = prepareBarrier(nJobs);
			Set<MeshResponse<?>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(client().findSchemaByUuid(uuid).invoke());
			}
			validateSet(set, barrier);
		}
	}

	@Test
	@Override
	@Ignore("not yet supported")
	public void testDeleteByUUIDMultithreaded() throws Exception {
		int nJobs = 3;
		try (Tx tx = db().tx()) {
			SchemaContainer schema = schemaContainer("content");
			CyclicBarrier barrier = prepareBarrier(nJobs);
			Set<MeshResponse<Void>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(client().deleteSchema(schema.getUuid()).invoke());
			}
			validateDeletion(set, barrier);
		}
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
		Set<MeshResponse<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(client().createSchema(request).invoke());
		}
		validateCreation(set, barrier);
	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws Exception {
		int nJobs = 200;
		try (Tx tx = db().tx()) {
			SchemaContainer schema = schemaContainer("content");
			Set<MeshResponse<SchemaResponse>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(client().findSchemaByUuid(schema.getUuid()).invoke());
			}
			for (MeshResponse<SchemaResponse> future : set) {
				latchFor(future);
				assertSuccess(future);
			}
		}
	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() throws Exception {
		String schemaUuid;
		try (Tx tx = db().tx()) {
			SchemaContainer schema = schemaContainer("content");
			role().revokePermissions(schema, UPDATE_PERM);
			schemaUuid = schema.getUuid();
		}

		try (Tx tx = db().tx()) {
			SchemaUpdateRequest request = new SchemaUpdateRequest();
			request.setName("new-name");
			call(() -> client().updateSchema(schemaUuid, request), FORBIDDEN, "error_missing_perm", schemaUuid);
		}

	}

}