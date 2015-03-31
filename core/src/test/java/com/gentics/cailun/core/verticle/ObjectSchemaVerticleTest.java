package com.gentics.cailun.core.verticle;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import io.vertx.core.http.HttpMethod;

import org.junit.Assert;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.service.ObjectSchemaService;
import com.gentics.cailun.core.data.service.ProjectService;
import com.gentics.cailun.core.rest.schema.request.ObjectSchemaCreateRequest;
import com.gentics.cailun.core.rest.schema.request.ObjectSchemaUpdateRequest;
import com.gentics.cailun.core.rest.schema.response.ObjectSchemaResponse;
import com.gentics.cailun.core.rest.schema.response.PropertyTypeSchemaResponse;
import com.gentics.cailun.error.HttpStatusCodeErrorException;
import com.gentics.cailun.test.AbstractRestVerticleTest;
import com.gentics.cailun.util.JsonUtils;

public class ObjectSchemaVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private ObjectSchemaVerticle objectSchemaVerticle;

	@Autowired
	private ObjectSchemaService objectSchemaService;

	@Autowired
	private ProjectService projectService;

	@Autowired
	private Neo4jTemplate neo4jTemplate;

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
		request.setProjectUuid(data().getProject().getUuid());
		PropertyTypeSchemaResponse propertySchema = new PropertyTypeSchemaResponse();
		propertySchema.setKey("extra-content");
		propertySchema.setType("html");
		propertySchema.setDescription("Some extra content");
		request.getPropertyTypeSchemas().add(propertySchema);

		roleService.addPermission(info.getRole(), data().getProject(), PermissionType.CREATE);

		String response = request(info, HttpMethod.POST, "/api/v1/schemas/", 200, "OK", JsonUtils.toJson(request));
		String json = "{\"uuid\":\"uuid-value\",\"type\":\"object\",\"description\":\"new description\",\"projects\":[{\"uuid\":\"uuid-value\",\"name\":\"dummy\"}],\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"title\":\"new schema name\",\"properties\":[{\"uuid\":\"uuid-value\",\"type\":\"html\",\"key\":\"extra-content\",\"desciption\":\"Some extra content\",\"order\":0}]}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		ObjectSchemaResponse responseObject = JsonUtils.readValue(response, ObjectSchemaResponse.class);
		ObjectSchema schema = objectSchemaService.findByUUID(responseObject.getUUID());
		Assert.assertEquals("Name does not match with the requested name", request.getName(), schema.getName());
		Assert.assertEquals("Description does not match with the requested description", request.getDescription(), schema.getDescription());
		Assert.assertEquals("There should be exactly one property schema.", 1, schema.getPropertyTypeSchemas().size());

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

		roleService.addPermission(info.getRole(), data().getProject(), PermissionType.CREATE);

		String response = request(info, HttpMethod.POST, "/api/v1/schemas/", 200, "OK", JsonUtils.toJson(request));
		String json = "{\"uuid\":\"uuid-value\",\"type\":\"object\",\"description\":\"new description\",\"projects\":[{\"uuid\":\"uuid-value\",\"name\":\"dummy\"}],\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"title\":\"new schema name\",\"properties\":[{\"uuid\":\"uuid-value\",\"type\":\"html\",\"key\":\"extra-content\",\"desciption\":\"Some extra content\",\"order\":0}]}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		ObjectSchemaResponse responseObject = JsonUtils.readValue(response, ObjectSchemaResponse.class);
		ObjectSchema schema = objectSchemaService.findByUUID(responseObject.getUUID());
		Assert.assertEquals("Name does not match with the requested name", request.getName(), schema.getName());
		Assert.assertEquals("Description does not match with the requested description", request.getDescription(), schema.getDescription());
		Assert.assertEquals("There should be exactly one property schema.", 1, schema.getPropertyTypeSchemas().size());

		ObjectSchemaResponse restSchema = JsonUtils.readValue(response, ObjectSchemaResponse.class);
		response = request(info, HttpMethod.DELETE, "/api/v1/schemas/" + restSchema.getUUID(), 200, "OK");
		expectMessageResponse("schema_deleted", response, restSchema.getUUID());

	}

	// Read Tests

	@Test
	public void testReadAllSchemasForProject() throws Exception {
		String response = request(info, HttpMethod.GET, "/api/v1/schemas/", 200, "OK");
		String json = "{\"custom-content\":{\"uuid\":\"uuid-value\",\"type\":\"object\",\"description\":\"Custom schema for contents\",\"projects\":[{\"uuid\":\"uuid-value\",\"name\":\"dummy\"}],\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"title\":\"custom-content\",\"properties\":[{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"content\",\"order\":0},{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"filename\",\"order\":0},{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"name\",\"order\":0},{\"uuid\":\"uuid-value\",\"type\":\"string\",\"key\":\"secret\",\"order\":0}]},\"content\":{\"uuid\":\"uuid-value\",\"type\":\"object\",\"description\":\"Default schema for contents\",\"projects\":[{\"uuid\":\"uuid-value\",\"name\":\"dummy\"}],\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"title\":\"content\",\"properties\":[{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"content\",\"order\":0},{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"filename\",\"order\":0},{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"name\",\"order\":0}]}}";
		assertEqualsSanitizedJson("The response json did not match the expected one.", json, response);
	}

	@Test
	public void testReadSchemaByUUID() throws Exception {
		ObjectSchema schema = data().getContentSchema();

		roleService.addPermission(info.getRole(), schema, PermissionType.READ);

		String response = request(info, HttpMethod.GET, "/api/v1/schemas/" + schema.getUuid(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"type\":\"object\",\"description\":\"Default schema for contents\",\"projects\":[{\"uuid\":\"uuid-value\",\"name\":\"dummy\"}],\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"title\":\"content\",\"properties\":[{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"content\",\"order\":0},{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"filename\",\"order\":0},{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"name\",\"order\":0}]}";
		assertEqualsSanitizedJson("The response json did not match the expected one.", json, response);
	}

	@Test
	public void testReadSchemaByUUIDWithNoPerm() throws Exception {
		ObjectSchema schema = data().getContentSchema();

		roleService.addPermission(info.getRole(), schema, PermissionType.DELETE);
		roleService.addPermission(info.getRole(), schema, PermissionType.UPDATE);
		roleService.addPermission(info.getRole(), schema, PermissionType.CREATE);

		String response = request(info, HttpMethod.GET, "/api/v1/schemas/" + schema.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, schema.getUuid());
	}

	@Test
	public void testReadSchemaByInvalidUUID() throws Exception {

		String response = request(info, HttpMethod.GET, "/api/v1/schemas/bogus", 404, "Not Found");
		String json = "{\"message\":\"Object with uuid \\\"bogus\\\" could not be found.\"}";
		assertEqualsSanitizedJson("The response json did not match the expected one.", json, response);
	}

	// Update Tests

	@Test
	public void testUpdateSchemaByUUID() throws HttpStatusCodeErrorException, Exception {
		ObjectSchema schema = data().getContentSchema();
		roleService.addPermission(info.getRole(), schema, PermissionType.UPDATE);

		ObjectSchemaUpdateRequest request = new ObjectSchemaUpdateRequest();
		request.setUuid(schema.getUuid());
		request.setName("new-name");

		String response = request(info, HttpMethod.PUT, "/api/v1/schemas/" + schema.getUuid(), 200, "OK", JsonUtils.toJson(request));
		String json = "{\"uuid\":\"uuid-value\",\"type\":\"object\",\"projects\":[{\"uuid\":\"uuid-value\",\"name\":\"dummy\"}],\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"title\":\"new-name\",\"properties\":[{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"content\",\"order\":0},{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"filename\",\"order\":0},{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"name\",\"order\":0}]}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		ObjectSchema reloaded = objectSchemaService.findByUUID(schema.getUuid());
		Assert.assertEquals("The name should have been updated", "new-name", reloaded.getName());

	}

	@Test
	public void testUpdateSchemaByBogusUUID() throws HttpStatusCodeErrorException, Exception {
		ObjectSchema schema = data().getContentSchema();

		ObjectSchemaUpdateRequest request = new ObjectSchemaUpdateRequest();
		request.setUuid("bogus");
		request.setName("new-name");

		String response = request(info, HttpMethod.PUT, "/api/v1/schemas/" + "bogus", 404, "Not Found", JsonUtils.toJson(request));
		String json = "{\"message\":\"Object with uuid \\\"bogus\\\" could not be found.\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		ObjectSchema reloaded = objectSchemaService.findByUUID(schema.getUuid());
		Assert.assertEquals("The name should not have been changed.", schema.getName(), reloaded.getName());

	}

	// Delete Tests

	@Test
	public void testDeleteSchemaByUUID() throws Exception {
		ObjectSchema schema = data().getContentSchema();

		roleService.addPermission(info.getRole(), schema, PermissionType.DELETE);

		String response = request(info, HttpMethod.DELETE, "/api/v1/schemas/" + schema.getUuid(), 200, "OK");
		String json = "{\"message\":\"Schema with uuid \\\"" + schema.getUuid() + "\\\" was deleted.\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		ObjectSchema reloaded = objectSchemaService.findByUUID(schema.getUuid());
		assertNull("The schema should have been deleted.", reloaded);
	}

	public void testDeleteSchemaWithMissingPermission() throws Exception {
		ObjectSchema schema = data().getContentSchema();

		String response = request(info, HttpMethod.DELETE, "/api/v1/schemas/" + schema.getUuid(), 200, "OK");
		String json = "error";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		ObjectSchema reloaded = objectSchemaService.findByUUID(schema.getUuid());
		assertNotNull("The schema should not have been deleted.", reloaded);

	}

	// Schema Project Testcases - PUT / Add

	@Test
	public void testAddSchemaToProjectWithPerm() throws Exception {
		ObjectSchema schema = data().getContentSchema();

		Project extraProject = new Project("extraProject");
		extraProject = projectService.save(extraProject);
		extraProject = projectService.reload(extraProject);

		// Add only read perms
		roleService.addPermission(info.getRole(), schema, PermissionType.READ);
		roleService.addPermission(info.getRole(), extraProject, PermissionType.UPDATE);

		String response = request(info, HttpMethod.POST, "/api/v1/schemas/" + schema.getUuid() + "/projects/" + extraProject.getUuid(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"type\":\"object\",\"description\":\"Default schema for contents\",\"projects\":[{\"uuid\":\"uuid-value\",\"name\":\"dummy\"},{\"uuid\":\"uuid-value\",\"name\":\"extraProject\"}],\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"title\":\"content\",\"properties\":[{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"content\",\"order\":0},{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"filename\",\"order\":0},{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"name\",\"order\":0}]}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		// Reload the schema and check for expected changes
		schema = objectSchemaService.reload(schema);
		assertTrue("The schema should be added to the extra project", schema.getProjects().contains(extraProject));

	}

	@Test
	public void testAddSchemaToProjectWithoutPerm() throws Exception {
		ObjectSchema schema = data().getContentSchema();
		Project project = data().getProject();

		Project extraProject = new Project("extraProject");
		extraProject = projectService.save(extraProject);
		extraProject = projectService.reload(extraProject);

		// Add only read perms
		roleService.addPermission(info.getRole(), schema, PermissionType.READ);
		roleService.addPermission(info.getRole(), project, PermissionType.READ);

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
		ObjectSchema schema = data().getContentSchema();
		Project project = data().getProject();

		assertTrue("The schema should be assigned to the project.", schema.getProjects().contains(project));

		// Add only read perms
		roleService.addPermission(info.getRole(), schema, PermissionType.READ);
		roleService.addPermission(info.getRole(), project, PermissionType.UPDATE);

		String response = request(info, HttpMethod.DELETE, "/api/v1/schemas/" + schema.getUuid() + "/projects/" + project.getUuid(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"type\":\"object\",\"description\":\"Default schema for contents\",\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"title\":\"content\",\"properties\":[{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"content\",\"order\":0},{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"filename\",\"order\":0},{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"name\",\"order\":0}]}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		// Reload the schema and check for expected changes
		schema = objectSchemaService.reload(schema);
		assertFalse("The schema should have been removed from the extra project", schema.getProjects().contains(project));
	}

	@Test
	public void testRemoveSchemaFromProjectWithoutPerm() throws Exception {
		ObjectSchema schema = data().getContentSchema();
		Project project = data().getProject();

		assertTrue("The schema should be assigned to the project.", schema.getProjects().contains(project));

		// Add only read perms
		roleService.addPermission(info.getRole(), schema, PermissionType.READ);
		roleService.addPermission(info.getRole(), project, PermissionType.READ);

		String response = request(info, HttpMethod.DELETE, "/api/v1/schemas/" + schema.getUuid() + "/projects/" + project.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, project.getUuid());
		
		// Reload the schema and check for expected changes
		schema = objectSchemaService.reload(schema);
		assertTrue("The schema should still be listed for the project.", schema.getProjects().contains(project));
	}
}
