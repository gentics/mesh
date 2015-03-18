package com.gentics.cailun.core.verticle;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import io.vertx.core.http.HttpMethod;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.service.ProjectService;
import com.gentics.cailun.core.rest.project.request.ProjectCreateRequest;
import com.gentics.cailun.core.rest.project.request.ProjectUpdateRequest;
import com.gentics.cailun.test.AbstractRestVerticleTest;

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
		String requestJson = new ObjectMapper().writeValueAsString(request);

		String response = request(info, HttpMethod.POST, "/api/v1/projects/", 200, "OK", requestJson);
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"test12345\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		assertNotNull(projectService.findByName(name));

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
		request.setName("New Name");

		String response = request(info, HttpMethod.PUT, "/api/v1/projects/" + project.getUuid(), 200, "OK",
				new ObjectMapper().writeValueAsString(request));
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

		String response = request(info, HttpMethod.PUT, "/api/v1/projects/" + project.getUuid(), 403, "Forbidden",
				new ObjectMapper().writeValueAsString(request));
		String json = "{\"message\":\"Missing permission on object {" + project.getUuid() + "}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		Project reloadedProject = projectService.findByUUID(project.getUuid());

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

	// @Test
	// public void testDeleteProjectByName() throws Exception {
	// Project project = data().getProject();
	// assertNotNull(project.getUuid());
	//
	// roleService.addPermission(info.getRole(), project, PermissionType.DELETE);
	//
	// String response = request(info, HttpMethod.DELETE, "/api/v1/projects/" + project.getName(), 200, "OK");
	// String json = "{\"message\":\"Deleted project {dummy}\"}";
	// assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	// assertNull("The project should have been deleted", projectService.findByUUID(project.getUuid()));
	// }
	//
	// @Test
	// public void testDeleteProjectByNameWithNoPermission() throws Exception {
	// Project project = data().getProject();
	// assertNotNull(project.getUuid());
	//
	// String response = request(info, HttpMethod.DELETE, "/api/v1/projects/" + project.getName(), 403, "Forbidden");
	// String json = "{\"message\":\"Missing permission on object {" + project.getUuid() + "}\"}";
	// assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	// assertNotNull("The project should not have been deleted", projectService.findByUUID(project.getUuid()));
	// }

}
