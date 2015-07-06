package com.gentics.mesh.core.verticle.schema;

import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import io.vertx.core.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.rest.schema.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaResponse;
import com.gentics.mesh.core.rest.schema.SchemaUpdateRequest;
import com.gentics.mesh.core.verticle.SchemaVerticle;
import com.gentics.mesh.error.HttpStatusCodeErrorException;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class SchemaVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private SchemaVerticle schemaVerticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return schemaVerticle;
	}

	// Create Tests

	@Test
	public void testCreateSimpleSchema() throws HttpStatusCodeErrorException, Exception {
		SchemaCreateRequest request = new SchemaCreateRequest();

		request.setName("new schema name");

		//		request.getPropertyTypeSchemas().add(propertySchema);

		String response = request(info, HttpMethod.POST, "/api/v1/schemas/", 200, "OK", JsonUtil.toJson(request));
		SchemaResponse restSchemaResponse = JsonUtil.readValue(response, SchemaResponse.class);
		test.assertSchema(request, restSchemaResponse);

		SchemaResponse responseObject = JsonUtil.readValue(response, SchemaResponse.class);
		SchemaContainer schema = boot.schemaContainerRoot().findByUUID(responseObject.getUuid());
		//		assertEquals("Name does not match with the requested name", request.getName(), schema.getName());
		//		assertEquals("Description does not match with the requested description", request.getDescription(), schema.getDescription());
		//		assertEquals("There should be exactly one property schema.", 1, schema.getPropertyTypes().size());

	}

	@Test
	public void testCreateDeleteSimpleSchema() throws HttpStatusCodeErrorException, Exception {

		//		SchemaCreateRequest request = new SchemaCreateRequest();
		//		request.setDescription("new description");
		//		request.setName("new schema name");
		//		request.setProjectUuid(data().getProject().getUuid());
		//		PropertyTypeSchemaResponse propertySchema = new PropertyTypeSchemaResponse();
		//		propertySchema.setKey("extra-content");
		//		propertySchema.setType("html");
		//		propertySchema.setDescription("Some extra content");
		//		request.getPropertyTypeSchemas().add(propertySchema);
		//
		//		String response = request(info, HttpMethod.POST, "/api/v1/schemas/", 200, "OK", JsonUtils.toJson(request));
		//		SchemaResponse restSchema = JsonUtils.readValue(response, SchemaResponse.class);
		//		test.assertSchema(request, restSchema);
		//
		//		// Verify that the object was created
		//		Schema schema = schemaRoot.findByUUID(restSchema.getUuid());
		//		test.assertSchema(schema, restSchema);
		//		assertEquals("There should be exactly one property schema.", 1, schema.getPropertyTypes().size());
		//
		//		response = request(info, HttpMethod.DELETE, "/api/v1/schemas/" + restSchema.getUuid(), 200, "OK");
		//		expectMessageResponse("schema_deleted", response, restSchema.getName());

	}

	// Read Tests

	@Test
	public void testReadAllSchemaList() throws Exception {

		SchemaContainerRoot schemaRoot = data().getMeshRoot().getSchemaContainerRoot();
		final int nSchemas = 22;
		SchemaContainer noPermSchema = schemaRoot.create("no_perm_schema");
		for (int i = 0; i < nSchemas; i++) {
			SchemaContainer extraSchema = schemaRoot.create("extra_schema_" + i);
			info.getRole().addPermissions(extraSchema, READ_PERM);
		}
		// Don't grant permissions to no perm schema

		// Test default paging parameters
		String response = request(info, HttpMethod.GET, "/api/v1/schemas/", 200, "OK");
		SchemaListResponse restResponse = JsonUtil.readValue(response, SchemaListResponse.class);
		assertEquals(25, restResponse.getMetainfo().getPerPage());
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		assertEquals(25, restResponse.getData().size());

		int perPage = 11;
		response = request(info, HttpMethod.GET, "/api/v1/schemas/?per_page=" + perPage + "&page=" + 2, 200, "OK");
		restResponse = JsonUtil.readValue(response, SchemaListResponse.class);
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
			response = request(info, HttpMethod.GET, "/api/v1/schemas/?per_page=" + perPage + "&page=" + page, 200, "OK");
			restResponse = JsonUtil.readValue(response, SchemaListResponse.class);
			allSchemas.addAll(restResponse.getData());
		}
		assertEquals("Somehow not all schemas were loaded when loading all pages.", totalSchemas, allSchemas.size());

		// Verify that the no perm schema is not part of the response
		//		final String noPermSchemaName = noPermSchema.getName();
		//		List<SchemaResponse> filteredSchemaList = allSchemas.parallelStream().filter(restSchema -> restSchema.getName().equals(noPermSchemaName))
		//				.collect(Collectors.toList());
		//		assertTrue("The no perm schema should not be part of the list since no permissions were added.", filteredSchemaList.size() == 0);

		response = request(info, HttpMethod.GET, "/api/v1/schemas/?per_page=" + perPage + "&page=" + -1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, HttpMethod.GET, "/api/v1/schemas/?per_page=" + 0 + "&page=" + 1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, HttpMethod.GET, "/api/v1/schemas/?per_page=" + -1 + "&page=" + 1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);

		response = request(info, HttpMethod.GET, "/api/v1/schemas/?per_page=" + 25 + "&page=" + 4242, 200, "OK");
		SchemaListResponse list = JsonUtil.readValue(response, SchemaListResponse.class);
		assertEquals(4242, list.getMetainfo().getCurrentPage());
		assertEquals(0, list.getData().size());
	}

	@Test
	public void testReadSchemaByUUID() throws Exception {

		SchemaContainer schema = data().getSchemaContainer("content");
		String response = request(info, HttpMethod.GET, "/api/v1/schemas/" + schema.getUuid(), 200, "OK");
		SchemaResponse restSchema = JsonUtil.readValue(response, SchemaResponse.class);
		test.assertSchema(schema, restSchema);
	}

	@Test
	public void testReadSchemaByUUIDWithNoPerm() throws Exception {
		SchemaContainer schema = data().getSchemaContainer("content");

		info.getRole().addPermissions(schema, DELETE_PERM);
		info.getRole().addPermissions(schema, UPDATE_PERM);
		info.getRole().addPermissions(schema, CREATE_PERM);
		info.getRole().revokePermissions(schema, READ_PERM);

		String response = request(info, HttpMethod.GET, "/api/v1/schemas/" + schema.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, schema.getUuid());
	}

	@Test
	public void testReadSchemaByInvalidUUID() throws Exception {
		String response = request(info, HttpMethod.GET, "/api/v1/schemas/bogus", 404, "Not Found");
		expectMessageResponse("object_not_found_for_uuid", response, "bogus");
	}

	// Update Tests

	@Test
	public void testUpdateSchemaByUUID() throws HttpStatusCodeErrorException, Exception {
		SchemaContainer schema = data().getSchemaContainer("content");
		SchemaUpdateRequest request = new SchemaUpdateRequest();
		request.setUuid(schema.getUuid());
		request.setName("new-name");

		String response = request(info, HttpMethod.PUT, "/api/v1/schemas/" + schema.getUuid(), 200, "OK", JsonUtil.toJson(request));
		SchemaResponse restSchema = JsonUtil.readValue(response, SchemaResponse.class);
		//		assertEquals(request.getName(), restSchema.getName());

		SchemaContainer reloaded = boot.schemaContainerRoot().findByUUID(schema.getUuid());
		//		assertEquals("The name should have been updated", "new-name", reloaded.getName());

	}

	@Test
	public void testUpdateSchemaByBogusUUID() throws HttpStatusCodeErrorException, Exception {
		SchemaContainer schema = data().getSchemaContainer("content");

		SchemaUpdateRequest request = new SchemaUpdateRequest();
		request.setUuid("bogus");
		request.setName("new-name");

		String response = request(info, HttpMethod.PUT, "/api/v1/schemas/" + "bogus", 404, "Not Found", JsonUtil.toJson(request));
		expectMessageResponse("object_not_found_for_uuid", response, "bogus");

		SchemaContainer reloaded = boot.schemaContainerRoot().findByUUID(schema.getUuid());
		//		assertEquals("The name should not have been changed.", schema.getName(), reloaded.getName());

	}

	// Delete Tests

	@Test
	public void testDeleteSchemaByUUID() throws Exception {
		SchemaContainer schema = data().getSchemaContainer("content");
		String response = request(info, HttpMethod.DELETE, "/api/v1/schemas/" + schema.getUuid(), 200, "OK");
		System.out.println(response);
		//		expectMessageResponse("schema_deleted", response, schema.getName());

		SchemaContainer reloaded = boot.schemaContainerRoot().findByUUID(schema.getUuid());
		assertNull("The schema should have been deleted.", reloaded);
	}

	public void testDeleteSchemaWithMissingPermission() throws Exception {
		SchemaContainer schema = data().getSchemaContainer("content");
		String response = request(info, HttpMethod.DELETE, "/api/v1/schemas/" + schema.getUuid(), 200, "OK");
		String json = "error";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		SchemaContainer reloaded = boot.schemaContainerRoot().findByUUID(schema.getUuid());
		assertNotNull("The schema should not have been deleted.", reloaded);

	}

	// Schema Project Testcases - PUT / Add

	@Test
	public void testAddSchemaToProjectWithPerm() throws Exception {
		SchemaContainer schema = data().getSchemaContainer("content");
		ProjectRoot projectRoot = data().getMeshRoot().getProjectRoot();
		Project extraProject = projectRoot.create("extraProject");

		// Add only read perms
		info.getRole().addPermissions(schema, READ_PERM);
		info.getRole().addPermissions(extraProject, UPDATE_PERM);

		String response = request(info, HttpMethod.POST, "/api/v1/schemas/" + schema.getUuid() + "/projects/" + extraProject.getUuid(), 200, "OK");
		SchemaResponse restSchema = JsonUtil.readValue(response, SchemaResponse.class);
		test.assertSchema(schema, restSchema);

		// Reload the schema and check for expected changes
		assertTrue("The schema should be added to the extra project", schema.getProjects().contains(extraProject));

	}

	@Test
	public void testAddSchemaToProjectWithoutPerm() throws Exception {
		SchemaContainer schema = data().getSchemaContainer("content");
		Project project = data().getProject();
		ProjectRoot projectRoot = data().getMeshRoot().getProjectRoot();
		Project extraProject = projectRoot.create("extraProject");
		// Add only read perms
		info.getRole().addPermissions(schema, READ_PERM);
		info.getRole().addPermissions(project, READ_PERM);

		String response = request(info, HttpMethod.POST, "/api/v1/schemas/" + schema.getUuid() + "/projects/" + extraProject.getUuid(), 403,
				"Forbidden");
		expectMessageResponse("error_missing_perm", response, extraProject.getUuid());

		// Reload the schema and check for expected changes
		assertFalse("The schema should not have been added to the extra project", schema.getProjects().contains(extraProject));

	}

	// Schema Project Testcases - DELETE / Remove
	@Test
	public void testRemoveSchemaFromProjectWithPerm() throws Exception {
		SchemaContainer schema = data().getSchemaContainer("content");
		Project project = data().getProject();
		assertTrue("The schema should be assigned to the project.", schema.getProjects().contains(project));

		String response = request(info, HttpMethod.DELETE, "/api/v1/schemas/" + schema.getUuid() + "/projects/" + project.getUuid(), 200, "OK");
		SchemaResponse restSchema = JsonUtil.readValue(response, SchemaResponse.class);
		test.assertSchema(schema, restSchema);

		final String removedProjectName = project.getName();
		assertFalse(restSchema.getProjects().stream().filter(p -> p.getName() == removedProjectName).findFirst().isPresent());

		// Reload the schema and check for expected changes
		assertFalse("The schema should have been removed from the extra project", schema.getProjects().contains(project));
	}

	@Test
	public void testRemoveSchemaFromProjectWithoutPerm() throws Exception {
		SchemaContainer schema = data().getSchemaContainer("content");
		Project project = data().getProject();

		assertTrue("The schema should be assigned to the project.", schema.getProjects().contains(project));

		// Revoke update perms on the project
		info.getRole().revokePermissions(project, UPDATE_PERM);

		String response = request(info, HttpMethod.DELETE, "/api/v1/schemas/" + schema.getUuid() + "/projects/" + project.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, project.getUuid());

		// Reload the schema and check for expected changes
		assertTrue("The schema should still be listed for the project.", schema.getProjects().contains(project));
	}
}
