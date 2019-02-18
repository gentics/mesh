package com.gentics.mesh.core.schema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_DELETED;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.COMPLETED;
import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ClientHelper.validateDeletion;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.awaitConcurrentRequests;
import static com.gentics.mesh.test.context.MeshTestHelper.validateCreation;
import static com.gentics.mesh.test.util.MeshAssert.assertElement;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
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
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.definition.BasicRestTestcases;
import com.syncleus.ferma.tx.Tx;

import io.reactivex.Observable;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class SchemaEndpointTest extends AbstractMeshTest implements BasicRestTestcases {

	@After
	public void waitForJobs() {
		tx(() -> group().addRole(roles().get("admin")));
		triggerAndWaitForAllJobs(COMPLETED);
	}

	@Test
	@Override
	public void testCreate() throws GenericRestException, Exception {
		SchemaCreateRequest createRequest = FieldUtil.createMinimalValidSchemaCreateRequest();

		assertThat(trackingSearchProvider()).hasEvents(0, 0, 0, 0);

		expectEvents(SCHEMA_CREATED, 1, MeshElementEventModelImpl.class, event -> {
			assertThat(event).hasName(createRequest.getName()).uuidNotNull();
			return true;
		});

		SchemaResponse restSchema = call(() -> client().createSchema(createRequest));

		waitForEvents();

		assertThat(trackingSearchProvider()).hasEvents(1, 0, 0, 0);
		assertThat(trackingSearchProvider()).hasStore(SchemaContainer.composeIndexName(), SchemaContainer.composeDocumentId(restSchema.getUuid()));
		try (Tx tx = tx()) {
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
		try (Tx tx = tx()) {
			role().revokePermissions(meshRoot().getSchemaContainerRoot(), CREATE_PERM);
			tx.success();
		}
		call(() -> client().createSchema(schema), FORBIDDEN, "error_missing_perm", schemaRootUuid, CREATE_PERM.getRestPerm().getName());
	}

	@Test
	@Override
	@Ignore("Not yet implemented")
	public void testCreateWithUuid() throws Exception {
	}

	@Test
	@Override
	@Ignore("Not yet implemented")
	public void testCreateWithDuplicateUuid() throws Exception {
	}

	@Test
	@Override
	public void testCreateReadDelete() throws GenericRestException, Exception {

		try (Tx tx = tx()) {
			assertThat(trackingSearchProvider()).hasEvents(0, 0, 0, 0);
			SchemaCreateRequest schema = FieldUtil.createMinimalValidSchemaCreateRequest();

			SchemaResponse restSchema = call(() -> client().createSchema(schema));
			assertThat(trackingSearchProvider()).hasEvents(1, 0, 0, 0);
			assertThat(schema).matches(restSchema);
			assertElement(boot().meshRoot().getSchemaContainerRoot(), restSchema.getUuid(), true);
			call(() -> client().findSchemaByUuid(restSchema.getUuid()));

			trackingSearchProvider().clear().blockingAwait();
			call(() -> client().deleteSchema(restSchema.getUuid()));
			// Only schemas which are not in use can be delete and also removed from the index
			assertThat(trackingSearchProvider()).hasEvents(0, 1, 0, 0);
		}

	}

	@Test
	@Override
	public void testReadMultiple() throws Exception {
		int totalSchemas;
		final int nSchemas = 22;

		try (Tx tx = tx()) {
			SchemaContainerRoot schemaRoot = meshRoot().getSchemaContainerRoot();

			// Create schema with no read permission
			SchemaModel schema = FieldUtil.createMinimalValidSchema();
			schema.setName("No_Perm_Schema");
			SchemaContainer noPermSchema = schemaRoot.create(schema, user());
			SchemaModel dummySchema = new SchemaModelImpl();
			dummySchema.setName("dummy");
			dummySchema.setVersion("1.0");
			noPermSchema.getLatestVersion().setSchema(dummySchema);

			// Create multiple schemas
			for (int i = 0; i < nSchemas; i++) {
				schema = FieldUtil.createMinimalValidSchema();
				schema.setName("extra_schema_" + i);
				SchemaContainer extraSchema = schemaRoot.create(schema, user());
				extraSchema.getLatestVersion().setSchema(dummySchema);
				role().grantPermissions(extraSchema, READ_PERM);
			}
			totalSchemas = nSchemas + data().getSchemaContainers().size();
			tx.success();
		}

		try (Tx tx = tx()) {
			// Test default paging parameters
			SchemaListResponse restResponse = call(() -> client().findSchemas());
			assertNull(restResponse.getMetainfo().getPerPage());
			assertEquals(1, restResponse.getMetainfo().getCurrentPage());
			assertEquals(25, restResponse.getData().size());

			long perPage = 11;
			restResponse = call(() -> client().findSchemas(new PagingParametersImpl(2, perPage)));
			assertEquals(perPage, restResponse.getData().size());

			// Extra schemas + default schema
			int totalPages = (int) Math.ceil(totalSchemas / (double) perPage);
			assertEquals("The response did not contain the correct amount of items", 11, restResponse.getData().size());
			assertEquals(2, restResponse.getMetainfo().getCurrentPage());
			assertEquals(totalPages, restResponse.getMetainfo().getPageCount());
			assertEquals(perPage, restResponse.getMetainfo().getPerPage().longValue());
			assertEquals(totalSchemas, restResponse.getMetainfo().getTotalCount());

			List<Schema> allSchemas = new ArrayList<>();
			for (int page = 1; page <= totalPages; page++) {
				restResponse = client().findSchemas(new PagingParametersImpl(page, perPage)).blockingGet();
				allSchemas.addAll(restResponse.getData());
			}
			assertEquals("Somehow not all schemas were loaded when loading all pages.", totalSchemas, allSchemas.size());

			// Verify that the no perm schema is not part of the response
			// final String noPermSchemaName = noPermSchema.getName();
			// List<Schema> filteredSchemaList = allSchemas.parallelStream().filter(restSchema -> restSchema.getName().equals(noPermSchemaName))
			// .collect(Collectors.toList());
			// assertTrue("The no perm schema should not be part of the list since no permissions were added.", filteredSchemaList.size() == 0);

			call(() -> client().findSchemas(new PagingParametersImpl(-1, perPage)), BAD_REQUEST, "error_page_parameter_must_be_positive", "-1");

			call(() -> client().findSchemas(new PagingParametersImpl(1, -1L)), BAD_REQUEST, "error_pagesize_parameter", "-1");

			SchemaListResponse list = call(() -> client().findSchemas(new PagingParametersImpl(4242, 25L)));
			assertEquals(4242, list.getMetainfo().getCurrentPage());
			assertEquals(0, list.getData().size());
		}
	}

	@Test
	public void testReadMetaCountOnly() {
		SchemaListResponse list = call(() -> client().findSchemas(new PagingParametersImpl(1, 0L)));
		assertEquals(0, list.getData().size());
	}

	@Test
	@Override
	public void testReadByUUID() throws Exception {
		try (Tx tx = tx()) {
			SchemaContainer container = schemaContainer("content");
			SchemaContainerVersion schemaContainerVersion = container.getLatestVersion();
			SchemaResponse restSchema = call(() -> client().findSchemaByUuid(container.getUuid()));
			assertThat(restSchema).matches(schemaContainerVersion).isValid();
		}
	}

	@Test
	public void testReadVersion() {
		String uuid = tx(() -> schemaContainer("content").getUuid());
		String latestVersion = tx(() -> schemaContainer("content").getLatestVersion().getVersion());
		String json = tx(() -> schemaContainer("content").getLatestVersion().getJson());

		// Load the latest version
		SchemaResponse restSchema = call(() -> client().findSchemaByUuid(uuid, new VersioningParametersImpl().setVersion(latestVersion)));
		assertEquals("The loaded version did not match up with the requested version.", latestVersion, restSchema.getVersion());

		// Now update the schema
		SchemaUpdateRequest request = JsonUtil.readValue(json, SchemaUpdateRequest.class);
		request.setDescription("New description");
		request.addField(FieldUtil.createHtmlFieldSchema("someHtml"));

		tx(() -> group().addRole(roles().get("admin")));
		waitForJobs(() -> {
			call(() -> client().updateSchema(uuid, request));
		}, MigrationStatus.COMPLETED, 1);
		tx(() -> group().removeRole(roles().get("admin")));

		// Load the previous version
		restSchema = call(() -> client().findSchemaByUuid(uuid, new VersioningParametersImpl().setVersion(latestVersion)));
		assertEquals("The loaded version did not match up with the requested version.", latestVersion, restSchema.getVersion());

		// Load the latest version (2.0)
		restSchema = call(() -> client().findSchemaByUuid(uuid));
		assertEquals("The loaded version did not match up with the requested version.", "2.0", restSchema.getVersion());

		// Load the expected 2.0 version
		restSchema = call(() -> client().findSchemaByUuid(uuid, new VersioningParametersImpl().setVersion("2.0")));
		assertEquals("The loaded version did not match up with the requested version.", "2.0", restSchema.getVersion());
	}

	@Test
	public void testReadBogusVersion() {
		String uuid = tx(() -> schemaContainer("content").getUuid());

		call(() -> client().findSchemaByUuid(uuid, new VersioningParametersImpl().setVersion("5.0")), NOT_FOUND, "object_not_found_for_uuid_version",
			uuid, "5.0");

		call(() -> client().findSchemaByUuid(uuid, new VersioningParametersImpl().setVersion("sadgsdgasgd")), BAD_REQUEST, "error_illegal_version",
			"sadgsdgasgd");
	}

	@Test
	@Override
	public void testReadByUuidWithRolePerms() {
		String uuid = db().tx(() -> schemaContainer("content").getUuid());

		SchemaResponse schema = call(() -> client().findSchemaByUuid(uuid, new RolePermissionParametersImpl().setRoleUuid(db().tx(() -> role()
			.getUuid()))));
		assertNotNull(schema.getRolePerms());
		assertThat(schema.getRolePerms()).hasPerm(Permission.values());
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		SchemaContainer schema = schemaContainer("content");
		try (Tx tx = tx()) {
			role().grantPermissions(schema, DELETE_PERM);
			role().grantPermissions(schema, UPDATE_PERM);
			role().grantPermissions(schema, CREATE_PERM);
			role().revokePermissions(schema, READ_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			call(() -> client().findSchemaByUuid(schema.getUuid()), FORBIDDEN, "error_missing_perm", schema.getUuid(),
				READ_PERM.getRestPerm().getName());
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

	@Test
	public void testUpdateWithUrlFields() {
		String uuid = tx(() -> schemaContainer("folder").getUuid());
		String json = tx(() -> schemaContainer("folder").getLatestVersion().getJson());
		SchemaUpdateRequest request = JsonUtil.readValue(json, SchemaUpdateRequest.class);
		request.setUrlFields("slug");

		tx(() -> group().addRole(roles().get("admin")));
		waitForJobs(() -> {
			call(() -> client().updateSchema(uuid, request));
		}, COMPLETED, 1);
	}

	/**
	 * Test automagically assignment of referenced microschemas to projects.
	 */
	@Test
	public void testUpdateWithReferencedMicroschema() {

		SchemaUpdateRequest schemaUpdate = db().tx(() -> JsonUtil.readValue(schemaContainer("content").getLatestVersion().getJson(),
			SchemaUpdateRequest.class));
		String schemaUuid = db().tx(() -> schemaContainer("content").getUuid());

		// 1. Create the microschema
		MicroschemaCreateRequest microschemaRequest = new MicroschemaCreateRequest();
		microschemaRequest.setName("TestMicroschema");
		microschemaRequest.addField(FieldUtil.createStringFieldSchema("text"));
		microschemaRequest.addField(FieldUtil.createNodeFieldSchema("nodeRef").setAllowedSchemas("content"));
		MicroschemaResponse microschemaResponse = call(() -> client().createMicroschema(microschemaRequest));
		String microschemaUuid = microschemaResponse.getUuid();

		List<MicroschemaResponse> filteredList = call(() -> client().findMicroschemas(PROJECT_NAME)).getData().stream().filter(
			microschema -> microschema.getUuid().equals(microschemaUuid)).collect(Collectors.toList());

		assertThat(filteredList).isEmpty();

		// 2. Add micronode field to content schema
		schemaUpdate.addField(FieldUtil.createMicronodeFieldSchema("micro").setAllowedMicroSchemas("TestMicroschema"));

		tx(() -> group().addRole(roles().get("admin")));
		waitForJobs(() -> {
			call(() -> client().updateSchema(schemaUuid, schemaUpdate));
		}, MigrationStatus.COMPLETED, 1);
		tx(() -> group().removeRole(roles().get("admin")));

		filteredList = call(() -> client().findMicroschemas(PROJECT_NAME)).getData().stream().filter(microschema -> microschema.getUuid().equals(
			microschemaUuid)).collect(Collectors.toList());
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
		try (Tx tx = tx()) {
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
		try (Tx tx = tx()) {
			SchemaContainer schema = schemaContainer("content");
			assertThat(schema.getNodes()).isNotEmpty();
		}

		String uuid = db().tx(() -> schemaContainer("content").getUuid());

		call(() -> client().deleteSchema(uuid), BAD_REQUEST, "schema_delete_still_in_use", uuid);

		tx(() -> group().addRole(roles().get("admin")));
		waitForJobs(() -> {
			SchemaResponse schemaResponse = call(() -> client().findSchemaByUuid(uuid));
			SchemaUpdateRequest request = JsonUtil.readValue(schemaResponse.toJson(), SchemaUpdateRequest.class);
			request.setDescription("SomeOtherDescription");
			call(() -> client().updateSchema(uuid, request));
		}, COMPLETED, 1);

		String jobUuid = tx(() -> schemaContainer("content").getLatestVersion().referencedJobsViaTo().iterator().next().getUuid());

		try (Tx tx = tx()) {
			SchemaContainer reloaded = boot().schemaContainerRoot().findByUuid(uuid);

			assertNotNull("The schema should not have been deleted.", reloaded);
			// Validate and delete all remaining nodes that use the schema
			assertThat(reloaded.getNodes()).isNotEmpty();
			BulkActionContext context = createBulkContext();
			for (Node node : reloaded.getNodes()) {
				node.delete(context);
			}
			assertThat(reloaded.getNodes()).isEmpty();
			tx.success();
		}

		String versionUuid = tx(() -> schemaContainer("content").getLatestVersion().getUuid());
		assertTrue("The version should exist.", tx(tx -> {
			return tx.getGraph().getVertices("uuid", versionUuid).iterator().hasNext();
		}));

		expectEvents(SCHEMA_DELETED, 1, MeshElementEventModelImpl.class, event -> {
			assertThat(event).hasName("content").hasUuid(uuid);
			return true;
		});

		// We should be able to execute the deletion now that all nodes are gone.
		call(() -> client().deleteSchema(uuid));

		waitForEvents();

		try (Tx tx = tx()) {
			assertFalse("The referenced job should have been deleted", tx.getGraph().getVertices("uuid", jobUuid).iterator().hasNext());
			SchemaContainer reloaded = boot().schemaContainerRoot().findByUuid(uuid);
			assertFalse("The version of the schema container should have been deleted as well.", tx.getGraph().getVertices("uuid", versionUuid)
				.iterator().hasNext());
			assertNull("The schema should have been deleted.", reloaded);
		}
	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		SchemaContainer schema = schemaContainer("content");
		try (Tx tx = tx()) {
			role().revokePermissions(schema, DELETE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			call(() -> client().deleteSchema(schema.getUuid()), FORBIDDEN, "error_missing_perm", schema.getUuid(),
				DELETE_PERM.getRestPerm().getName());
			assertElement(boot().schemaContainerRoot(), schema.getUuid(), true);
		}
	}

	@Test
	@Override
	@Ignore("Fails on CI pipeline. See https://github.com/gentics/mesh/issues/608")
	public void testUpdateMultithreaded() throws Exception {
		String uuid = tx(() -> schemaContainer("content").getUuid());
		String json = tx(() -> schemaContainer("content").getLatestVersion().getJson());

		int nJobs = 20;
		Observable.range(0, nJobs)
			.flatMapCompletable(i -> {
				SchemaUpdateRequest request = JsonUtil.readValue(json, SchemaUpdateRequest.class);
				request.setName("newname" + i);
				return client().updateSchema(uuid, request).toCompletable();
			}).blockingAwait();
	}

	@Test
	@Override
	@Ignore("not yet supported")
	public void testReadByUuidMultithreaded() throws Exception {
		int nJobs = 10;
		try (Tx tx = tx()) {
			SchemaContainer schema = schemaContainer("content");
			String uuid = schema.getUuid();
			Observable.range(0, nJobs)
				.flatMapCompletable(i -> client().findSchemaByUuid(uuid).toCompletable())
				.blockingAwait();
		}
	}

	@Test
	@Override
	@Ignore("not yet supported")
	public void testDeleteByUUIDMultithreaded() throws Exception {
		int nJobs = 3;
		try (Tx tx = tx()) {
			SchemaContainer schema = schemaContainer("content");
			validateDeletion(i -> client().deleteSchema(schema.getUuid()), nJobs);
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

		validateCreation(nJobs, i -> client().createSchema(request));
	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws Exception {
		int nJobs = 200;
		try (Tx tx = tx()) {
			SchemaContainer schema = schemaContainer("content");
			awaitConcurrentRequests(nJobs, i -> client().findSchemaByUuid(schema.getUuid()));
		}
	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() throws Exception {
		String schemaUuid;
		try (Tx tx = tx()) {
			SchemaContainer schema = schemaContainer("content");
			role().revokePermissions(schema, UPDATE_PERM);
			schemaUuid = schema.getUuid();
			tx.success();
		}

		SchemaUpdateRequest request = new SchemaUpdateRequest();
		request.setName("new-name");
		call(() -> client().updateSchema(schemaUuid, request), FORBIDDEN, "error_missing_perm", schemaUuid, UPDATE_PERM.getRestPerm().getName());
	}

}