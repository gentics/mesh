package com.gentics.mesh.core.verticle.schema;
import static com.gentics.mesh.util.TinkerpopUtils.count;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import io.vertx.core.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.core.data.model.auth.PermissionType;
import com.gentics.mesh.core.data.model.tinkerpop.ObjectSchema;
import com.gentics.mesh.core.data.model.tinkerpop.Project;
import com.gentics.mesh.core.data.service.ObjectSchemaService;
import com.gentics.mesh.core.data.service.ProjectService;
import com.gentics.mesh.core.rest.schema.request.ObjectSchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.request.ObjectSchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.response.ObjectSchemaListResponse;
import com.gentics.mesh.core.rest.schema.response.ObjectSchemaResponse;
import com.gentics.mesh.core.rest.schema.response.PropertyTypeSchemaResponse;
import com.gentics.mesh.core.verticle.ObjectSchemaVerticle;
import com.gentics.mesh.error.HttpStatusCodeErrorException;
import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.util.JsonUtils;


public class ObjectSchemaVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private ObjectSchemaVerticle objectSchemaVerticle;

	@Autowired
	private ObjectSchemaService objectSchemaService;

	@Autowired
	private ProjectService projectService;

	@Autowired
	private GraphDatabaseService databaseService;

	@Override
	public AbstractRestVerticle getVerticle() {
		return objectSchemaVerticle;
	}

	// Create Tests

	@Test
	public void testCreateSimpleSchema() throws HttpStatusCodeErrorException, Exception {

		ObjectSchemaCreateRequest request = new ObjectSchemaCreateRequest();
		request.setDescription("new description");
		request.setName("new schema name");
		request.setDisplayName("Some display name");
		request.setProjectUuid(data().getProject().getUuid());
		PropertyTypeSchemaResponse propertySchema = new PropertyTypeSchemaResponse();
		propertySchema.setKey("extra-content");
		propertySchema.setType("html");
		propertySchema.setDescription("Some extra content");
		request.getPropertyTypeSchemas().add(propertySchema);

		String response = request(info, HttpMethod.POST, "/api/v1/schemas/", 200, "OK", JsonUtils.toJson(request));
		ObjectSchemaResponse restSchema = JsonUtils.readValue(response, ObjectSchemaResponse.class);
		test.assertSchema(request, restSchema);

		ObjectSchemaResponse responseObject = JsonUtils.readValue(response, ObjectSchemaResponse.class);
		ObjectSchema schema = objectSchemaService.findByUUID(responseObject.getUuid());
		assertEquals("Name does not match with the requested name", request.getName(), schema.getName());
		assertEquals("Description does not match with the requested description", request.getDescription(), schema.getDescription());
		assertEquals("There should be exactly one property schema.", 1, count(schema.getPropertyTypeSchemas()));

	}

	@Test
	public void testCreateDeleteSimpleSchema() throws HttpStatusCodeErrorException, Exception {

		ObjectSchemaCreateRequest request = new ObjectSchemaCreateRequest();
		request.setDescription("new description");
		request.setName("new schema name");
		request.setProjectUuid(data().getProject().getUuid());
		PropertyTypeSchemaResponse propertySchema = new PropertyTypeSchemaResponse();
		propertySchema.setKey("extra-content");
		propertySchema.setType("html");
		propertySchema.setDescription("Some extra content");
		request.getPropertyTypeSchemas().add(propertySchema);

		String response = request(info, HttpMethod.POST, "/api/v1/schemas/", 200, "OK", JsonUtils.toJson(request));
		ObjectSchemaResponse restSchema = JsonUtils.readValue(response, ObjectSchemaResponse.class);
		test.assertSchema(request, restSchema);

		// Verify that the object was created
		ObjectSchema schema = objectSchemaService.findByUUID(restSchema.getUuid());
		test.assertSchema(schema, restSchema);
		assertEquals("There should be exactly one property schema.", 1, count(schema.getPropertyTypeSchemas()));

		response = request(info, HttpMethod.DELETE, "/api/v1/schemas/" + restSchema.getUuid(), 200, "OK");
		expectMessageResponse("schema_deleted", response, restSchema.getName());

	}

	// Read Tests

	@Test
	public void testReadAllSchemaList() throws Exception {
		final int nSchemas = 22;
		ObjectSchema noPermSchema = objectSchemaService.create("no_perm_schema");
//		try (Transaction tx = graphDb.beginTx()) {
			for (int i = 0; i < nSchemas; i++) {
				ObjectSchema extraSchema = objectSchemaService.create("extra_schema_" + i);
				extraSchema = objectSchemaService.save(extraSchema);
				roleService.addPermission(info.getRole(), extraSchema, PermissionType.READ);
			}
			// Don't grant permissions to no perm schema
			noPermSchema = objectSchemaService.save(noPermSchema);
//			tx.success();
//		}

		// Test default paging parameters
		String response = request(info, HttpMethod.GET, "/api/v1/schemas/", 200, "OK");
		ObjectSchemaListResponse restResponse = JsonUtils.readValue(response, ObjectSchemaListResponse.class);
		assertEquals(25, restResponse.getMetainfo().getPerPage());
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		assertEquals(25, restResponse.getData().size());

		int perPage = 11;
		response = request(info, HttpMethod.GET, "/api/v1/schemas/?per_page=" + perPage + "&page=" + 2, 200, "OK");
		restResponse = JsonUtils.readValue(response, ObjectSchemaListResponse.class);
		assertEquals(perPage, restResponse.getData().size());

		// Extra schemas + default schema
		int totalSchemas = nSchemas + data().getSchemas().size();
		int totalPages = (int) Math.ceil(totalSchemas / (double) perPage);
		assertEquals("The response did not contain the correct amount of items", 11, restResponse.getData().size());
		assertEquals(2, restResponse.getMetainfo().getCurrentPage());
		assertEquals(totalPages, restResponse.getMetainfo().getPageCount());
		assertEquals(perPage, restResponse.getMetainfo().getPerPage());
		assertEquals(totalSchemas, restResponse.getMetainfo().getTotalCount());

		List<ObjectSchemaResponse> allSchemas = new ArrayList<>();
		for (int page = 1; page <= totalPages; page++) {
			response = request(info, HttpMethod.GET, "/api/v1/schemas/?per_page=" + perPage + "&page=" + page, 200, "OK");
			restResponse = JsonUtils.readValue(response, ObjectSchemaListResponse.class);
			allSchemas.addAll(restResponse.getData());
		}
		assertEquals("Somehow not all schemas were loaded when loading all pages.", totalSchemas, allSchemas.size());

		// Verify that the no perm schema is not part of the response
		final String noPermSchemaName = noPermSchema.getName();
		List<ObjectSchemaResponse> filteredSchemaList = allSchemas.parallelStream()
				.filter(restSchema -> restSchema.getName().equals(noPermSchemaName)).collect(Collectors.toList());
		assertTrue("The no perm schema should not be part of the list since no permissions were added.", filteredSchemaList.size() == 0);

		response = request(info, HttpMethod.GET, "/api/v1/schemas/?per_page=" + perPage + "&page=" + -1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, HttpMethod.GET, "/api/v1/schemas/?per_page=" + 0 + "&page=" + 1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, HttpMethod.GET, "/api/v1/schemas/?per_page=" + -1 + "&page=" + 1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);

		response = request(info, HttpMethod.GET, "/api/v1/schemas/?per_page=" + 25 + "&page=" + 4242, 200, "OK");
		ObjectSchemaListResponse list = JsonUtils.readValue(response, ObjectSchemaListResponse.class);
		assertEquals(4242, list.getMetainfo().getCurrentPage());
		assertEquals(0, list.getData().size());
	}

	@Test
	public void testReadSchemaByUUID() throws Exception {
		ObjectSchema schema = data().getSchema("content");
		String response = request(info, HttpMethod.GET, "/api/v1/schemas/" + schema.getUuid(), 200, "OK");
		ObjectSchemaResponse restSchema = JsonUtils.readValue(response, ObjectSchemaResponse.class);
		test.assertSchema(schema, restSchema);
	}

	@Test
	public void testReadSchemaByUUIDWithNoPerm() throws Exception {
		ObjectSchema schema = data().getSchema("content");

//		try (Transaction tx = graphDb.beginTx()) {
			roleService.addPermission(info.getRole(), schema, PermissionType.DELETE);
			roleService.addPermission(info.getRole(), schema, PermissionType.UPDATE);
			roleService.addPermission(info.getRole(), schema, PermissionType.CREATE);
			roleService.revokePermission(info.getRole(), schema, PermissionType.READ);
//			tx.success();
//		}

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
		ObjectSchema schema = data().getSchema("content");
		ObjectSchemaUpdateRequest request = new ObjectSchemaUpdateRequest();
		request.setUuid(schema.getUuid());
		request.setName("new-name");

		String response = request(info, HttpMethod.PUT, "/api/v1/schemas/" + schema.getUuid(), 200, "OK", JsonUtils.toJson(request));
		ObjectSchemaResponse restSchema = JsonUtils.readValue(response, ObjectSchemaResponse.class);
		assertEquals(request.getName(), restSchema.getName());

		ObjectSchema reloaded = objectSchemaService.findByUUID(schema.getUuid());
		assertEquals("The name should have been updated", "new-name", reloaded.getName());

	}

	@Test
	public void testUpdateSchemaByBogusUUID() throws HttpStatusCodeErrorException, Exception {
		ObjectSchema schema = data().getSchema("content");

		ObjectSchemaUpdateRequest request = new ObjectSchemaUpdateRequest();
		request.setUuid("bogus");
		request.setName("new-name");

		String response = request(info, HttpMethod.PUT, "/api/v1/schemas/" + "bogus", 404, "Not Found", JsonUtils.toJson(request));
		expectMessageResponse("object_not_found_for_uuid", response, "bogus");

		ObjectSchema reloaded = objectSchemaService.findByUUID(schema.getUuid());
		assertEquals("The name should not have been changed.", schema.getName(), reloaded.getName());

	}

	// Delete Tests

	@Test
	public void testDeleteSchemaByUUID() throws Exception {
		ObjectSchema schema = data().getSchema("content");
		String response = request(info, HttpMethod.DELETE, "/api/v1/schemas/" + schema.getUuid(), 200, "OK");
		System.out.println(response);
		expectMessageResponse("schema_deleted", response, schema.getName());

		ObjectSchema reloaded = objectSchemaService.findByUUID(schema.getUuid());
		assertNull("The schema should have been deleted.", reloaded);
	}

	public void testDeleteSchemaWithMissingPermission() throws Exception {
		ObjectSchema schema = data().getSchema("content");
		String response = request(info, HttpMethod.DELETE, "/api/v1/schemas/" + schema.getUuid(), 200, "OK");
		String json = "error";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		ObjectSchema reloaded = objectSchemaService.findByUUID(schema.getUuid());
		assertNotNull("The schema should not have been deleted.", reloaded);

	}

	// Schema Project Testcases - PUT / Add

	@Test
	public void testAddSchemaToProjectWithPerm() throws Exception {
		ObjectSchema schema = data().getSchema("content");

		Project extraProject = projectService.create("extraProject");
//		try (Transaction tx = graphDb.beginTx()) {
			extraProject = projectService.save(extraProject);
//			tx.success();
//		}

		// Add only read perms
//		try (Transaction tx = graphDb.beginTx()) {
			roleService.addPermission(info.getRole(), schema, PermissionType.READ);
			roleService.addPermission(info.getRole(), extraProject, PermissionType.UPDATE);
//			tx.success();
//		}

		String response = request(info, HttpMethod.POST, "/api/v1/schemas/" + schema.getUuid() + "/projects/" + extraProject.getUuid(), 200, "OK");
		ObjectSchemaResponse restSchema = JsonUtils.readValue(response, ObjectSchemaResponse.class);
		test.assertSchema(schema, restSchema);

		// Reload the schema and check for expected changes
		schema = objectSchemaService.reload(schema);
		assertTrue("The schema should be added to the extra project", schema.getProjects().contains(extraProject));

	}

	@Test
	public void testAddSchemaToProjectWithoutPerm() throws Exception {
		ObjectSchema schema = data().getSchema("content");
		Project project = data().getProject();

		Project extraProject = projectService.create("extraProject");
//		try (Transaction tx = graphDb.beginTx()) {
			extraProject = projectService.save(extraProject);
			// Add only read perms
			roleService.addPermission(info.getRole(), schema, PermissionType.READ);
			roleService.addPermission(info.getRole(), project, PermissionType.READ);
//			tx.success();
//		}

		String response = request(info, HttpMethod.POST, "/api/v1/schemas/" + schema.getUuid() + "/projects/" + extraProject.getUuid(), 403,
				"Forbidden");
		expectMessageResponse("error_missing_perm", response, extraProject.getUuid());

		// Reload the schema and check for expected changes
		schema = objectSchemaService.reload(schema);
		assertFalse("The schema should not have been added to the extra project", schema.getProjects().contains(extraProject));

	}

	// Schema Project Testcases - DELETE / Remove
	@Test
	public void testRemoveSchemaFromProjectWithPerm() throws Exception {
		ObjectSchema schema = data().getSchema("content");
		Project project = data().getProject();
//		try (Transaction tx = graphDb.beginTx()) {
			//project = neo4jTemplate.fetch(project);
			assertTrue("The schema should be assigned to the project.", schema.getProjects().contains(project));
//			tx.success();
//		}

		String response = request(info, HttpMethod.DELETE, "/api/v1/schemas/" + schema.getUuid() + "/projects/" + project.getUuid(), 200, "OK");
		ObjectSchemaResponse restSchema = JsonUtils.readValue(response, ObjectSchemaResponse.class);
		test.assertSchema(schema, restSchema);

		final String removedProjectName = project.getName();
		assertFalse(restSchema.getProjects().stream().filter(p -> p.getName() == removedProjectName).findFirst().isPresent());

		// Reload the schema and check for expected changes
		schema = objectSchemaService.reload(schema);
		assertFalse("The schema should have been removed from the extra project", schema.getProjects().contains(project));
	}

	@Test
	public void testRemoveSchemaFromProjectWithoutPerm() throws Exception {
		ObjectSchema schema = data().getSchema("content");
		Project project = data().getProject();

		assertTrue("The schema should be assigned to the project.", schema.getProjects().contains(project));

		// Revoke update perms on the project
//		try (Transaction tx = graphDb.beginTx()) {
			roleService.revokePermission(info.getRole(), project, PermissionType.UPDATE);
//			tx.success();
//		}

		String response = request(info, HttpMethod.DELETE, "/api/v1/schemas/" + schema.getUuid() + "/projects/" + project.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, project.getUuid());

		// Reload the schema and check for expected changes
		schema = objectSchemaService.reload(schema);
		assertTrue("The schema should still be listed for the project.", schema.getProjects().contains(project));
	}
}
