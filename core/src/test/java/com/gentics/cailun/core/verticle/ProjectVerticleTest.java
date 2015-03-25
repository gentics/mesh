package com.gentics.cailun.core.verticle;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import io.vertx.core.http.HttpMethod;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.CaiLunRoot;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.service.ProjectService;
import com.gentics.cailun.core.rest.project.request.ProjectCreateRequest;
import com.gentics.cailun.core.rest.project.request.ProjectUpdateRequest;
import com.gentics.cailun.core.rest.project.response.ProjectResponse;
import com.gentics.cailun.test.AbstractRestVerticleTest;
import com.gentics.cailun.util.JsonUtils;

public class ProjectVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private ProjectVerticle projectVerticle;

	@Autowired
	private ProjectService projectService;

	@Override
	public AbstractRestVerticle getVerticle() {
		return projectVerticle;
	}

	// Create Tests

	@Test
	public void testCreateProject() throws Exception {

		final String name = "test12345";
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(name);

		roleService.addPermission(info.getRole(), data().getCaiLunRoot(), PermissionType.CREATE);
		String requestJson = JsonUtils.toJson(request);

		String response = request(info, HttpMethod.POST, "/api/v1/projects/", 200, "OK", requestJson);
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"test12345\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		assertNotNull("The project should have been created.", projectService.findByName(name));

	}

	@Test
	public void testCreateDeleteProject() throws Exception {

		final String name = "test12345";
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(name);

		roleService.addPermission(info.getRole(), data().getCaiLunRoot(), PermissionType.CREATE);
		String requestJson = JsonUtils.toJson(request);

		String response = request(info, HttpMethod.POST, "/api/v1/projects/", 200, "OK", requestJson);
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"test12345\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		assertNotNull("The project should have been created.", projectService.findByName(name));

		ProjectResponse restProject = JsonUtils.readValue(response, ProjectResponse.class);
		response = request(info, HttpMethod.DELETE, "/api/v1/projects/" + restProject.getUUID(), 200, "OK");
		json = "{\"message\":\"Deleted project {test12345}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

	}

	// Read Tests

	@Test
	public void testReadProjectList() throws Exception {

		Project extraProject = new Project("test1234");
		extraProject = projectService.save(extraProject);
		Project extraProject2 = new Project("test1234-2-must-not-be-in-json");
		extraProject2 = projectService.save(extraProject2);

		Project project = data().getProject();
		assertNotNull("The UUID of the project must not be null.", project.getUuid());

		roleService.addPermission(info.getRole(), project, PermissionType.READ);
		roleService.addPermission(info.getRole(), extraProject, PermissionType.READ);
		// No read permission on last project

		String response = request(info, HttpMethod.GET, "/api/v1/projects/", 200, "OK");
		String json = "{\"projects\":[{\"uuid\":\"uuid-value\",\"name\":\"dummy\"},{\"uuid\":\"uuid-value\",\"name\":\"test1234\"}],\"_metainfo\":{\"page\":0,\"per_page\":0,\"page_count\":0,\"total_count\":0,\"links\":{}}}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testReadProjectByUUID() throws Exception {
		Project project = data().getProject();
		assertNotNull("The UUID of the project must not be null.", project.getUuid());

		roleService.addPermission(info.getRole(), project, PermissionType.READ);

		String response = request(info, HttpMethod.GET, "/api/v1/projects/" + project.getUuid(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"dummy\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testReadProjectByUUIDWithNoPerm() throws Exception {
		Project project = data().getProject();
		assertNotNull("The UUID of the project must not be null.", project.getUuid());

		String response = request(info, HttpMethod.GET, "/api/v1/projects/" + project.getUuid(), 403, "Forbidden");
		String json = "{\"message\":\"Missing permission on object {" + project.getUuid() + "}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	// Update Tests

	@Test
	public void testUpdateProject() throws JsonGenerationException, JsonMappingException, IOException, Exception {
		Project project = data().getProject();

		roleService.addPermission(info.getRole(), project, PermissionType.UPDATE);

		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setUuid(project.getUuid());
		request.setName("New Name");

		String response = request(info, HttpMethod.PUT, "/api/v1/projects/" + project.getUuid(), 200, "OK", JsonUtils.toJson(request));
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"New Name\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		Project reloadedProject = projectService.findByUUID(project.getUuid());

		Assert.assertEquals("New Name", reloadedProject.getName());
	}

	@Test
	public void testUpdateProjectWithNoPerm() throws JsonProcessingException, Exception {
		Project project = data().getProject();

		roleService.addPermission(info.getRole(), project, PermissionType.READ);

		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setName("New Name");

		String response = request(info, HttpMethod.PUT, "/api/v1/projects/" + project.getUuid(), 403, "Forbidden", JsonUtils.toJson(request));
		String json = "{\"message\":\"Missing permission on object {" + project.getUuid() + "}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		Project reloadedProject = projectService.reload(project);

		Assert.assertEquals("The name should not have been changed", project.getName(), reloadedProject.getName());
	}

	// Delete Tests

	@Test
	public void testDeleteProjectByUUID() throws Exception {
		Project project = data().getProject();
		assertNotNull(project.getUuid());

		roleService.addPermission(info.getRole(), project, PermissionType.DELETE);

		String response = request(info, HttpMethod.DELETE, "/api/v1/projects/" + project.getUuid(), 200, "OK");
		String json = "{\"message\":\"Deleted project {dummy}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		assertNull("The project should have been deleted", projectService.findByUUID(project.getUuid()));
	}

	@Test
	public void testDeleteProjectByUUIDWithNoPermission() throws Exception {
		Project project = data().getProject();

		String response = request(info, HttpMethod.DELETE, "/api/v1/projects/" + project.getUuid(), 403, "Forbidden");
		String json = "{\"message\":\"Missing permission on object {" + project.getUuid() + "}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		assertNotNull("The project should not have been deleted", projectService.findByUUID(project.getUuid()));
	}

}
