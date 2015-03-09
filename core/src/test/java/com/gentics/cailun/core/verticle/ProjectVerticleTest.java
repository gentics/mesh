package com.gentics.cailun.core.verticle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import io.vertx.core.http.HttpMethod;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.service.ProjectService;
import com.gentics.cailun.core.rest.project.request.ProjectCreateRequest;
import com.gentics.cailun.core.rest.project.response.ProjectResponse;
import com.gentics.cailun.test.AbstractRestVerticleTest;
import com.gentics.cailun.test.TestUtil;

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
		String json = "{\"id\":54,\"uuid\":\"uuid-value\",\"project\":null,\"creator\":null,\"locked\":false,\"name\":\"test12345\",\"rootTag\":null,\"new\":false}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		assertNotNull(projectService.findByName(name));

	}

	// Read Tests

	@Test
	public void testReadProjectByUUID() throws Exception {
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"dummy\"}";
		Project project = data().getProject();
		assertNotNull("The UUID of the project must not be null.", project.getUuid());

		String response = request(info, HttpMethod.GET, "/api/v1/projects/" + project.getUuid(), 200, "OK");
		TestUtil.assertEqualsSanitizedJson(json, response, ProjectResponse.class);
	}

	@Test
	public void testReadProjectByName() throws Exception {
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"dummy\"}";
		Project project = data().getProject();
		assertNotNull("The name of the project must not be null.", project.getName());

		String response = request(info, HttpMethod.GET, "/api/v1/projects/" + project.getName(), 200, "OK");
		TestUtil.assertEqualsSanitizedJson(json, response, ProjectResponse.class);
	}

	@Test
	public void testReadProjectInvalidName() throws Exception {
		String json = "{\"message\":\"Project not found {bogusName}\"}";
		Project project = data().getProject();
		assertNotNull("The UUID of the project must not be null.", project.getUuid());

		String response = request(info, HttpMethod.GET, "/api/v1/projects/" + "bogusName", 404, "Not Found");
		assertEquals(json, response);
	}

	// Update Tests
	@Test
	public void testUpdateProject() {
		fail("Not yet implemented");
	}

	// Delete Tests

	@Test
	public void testDeleteProjectByUUID() {
		fail("Not yet implemented");
	}

	@Test
	public void testDeleteProjectByName() {
		fail("Not yet implemented");
	}

}
