package com.gentics.mesh.generator;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.mockito.Mockito;
import org.raml.emitter.RamlEmitter;
import org.raml.model.Action;
import org.raml.model.ActionType;
import org.raml.model.MimeType;
import org.raml.model.Protocol;
import org.raml.model.Raml;
import org.raml.model.Resource;
import org.raml.model.Response;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.verticle.admin.AdminVerticle;
import com.gentics.mesh.core.verticle.auth.AuthenticationVerticle;
import com.gentics.mesh.core.verticle.group.GroupVerticle;
import com.gentics.mesh.core.verticle.microschema.MicroschemaVerticle;
import com.gentics.mesh.core.verticle.microschema.ProjectMicroschemaVerticle;
import com.gentics.mesh.core.verticle.navroot.NavRootVerticle;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.core.verticle.project.ProjectVerticle;
import com.gentics.mesh.core.verticle.release.ReleaseVerticle;
import com.gentics.mesh.core.verticle.role.RoleVerticle;
import com.gentics.mesh.core.verticle.schema.SchemaVerticle;
import com.gentics.mesh.core.verticle.tagfamily.TagFamilyVerticle;
import com.gentics.mesh.core.verticle.user.UserVerticle;
import com.gentics.mesh.core.verticle.utility.UtilityVerticle;
import com.gentics.mesh.core.verticle.webroot.WebRootVerticle;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.Endpoint;
import com.gentics.mesh.search.ProjectSearchVerticle;
import com.gentics.mesh.search.SearchVerticle;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;

public class RAMLGenerator {

	Raml raml = new Raml();

	public static void main(String[] args) throws Exception {
		new RAMLGenerator().generator();
	}

	public void generator() throws Exception {

		raml.setTitle("Gentics Mesh REST API");
		raml.setVersion("1");
		raml.setBaseUri("http://localhost:8080/api/v1");
		raml.getProtocols().add(Protocol.HTTP);
		raml.getProtocols().add(Protocol.HTTPS);
		raml.setMediaType("application/json");

		addCoreVerticles(raml.getResources());
		addProjectVerticles(raml.getResources());

		RamlEmitter emitter = new RamlEmitter();
		String dumpFromRaml = emitter.dump(raml);
		FileUtils.writeStringToFile(new File("api.raml"), dumpFromRaml);
		System.out.println(dumpFromRaml);
	}

	private void addEndpoints(String basePath, Map<String, Resource> resources, AbstractWebVerticle vericle) {

		Resource verticleResource = new Resource();
		for (Endpoint endpoint : vericle.getEndpoints()) {

			Action action = new Action();
			action.setIs(Arrays.asList(endpoint.getTraits()));
			action.setDisplayName(endpoint.getDisplayName());
			action.setDescription(endpoint.getDescription());

			action.setQueryParameters(endpoint.getQueryParameters());

			// Add response examples
			for (Entry<Integer, Object> entry : endpoint.getExampleResponses().entrySet()) {
				Response response = new Response();
				HashMap<String, MimeType> map = new HashMap<>();
				response.setBody(map);

				MimeType mimeType = new MimeType();
				mimeType.setExample(JsonUtil.toJson(entry.getValue()));
				map.put("application/json", mimeType);
				action.getResponses().put(String.valueOf(entry.getKey()), response);
			}

			// Add request example
			if (endpoint.getExampleRequest() != null) {
				HashMap<String, MimeType> bodyMap = new HashMap<>();
				MimeType mimeType = new MimeType();
				mimeType.setExample(JsonUtil.toJson(endpoint.getExampleRequest()));
				bodyMap.put("application/json", mimeType);
				action.setBody(bodyMap);
			}

			String path = endpoint.getRamlPath();
			if (path == null) {
				throw new RuntimeException("Could not determine path for endpoint of verticle " + vericle.getClass() + " " + endpoint.getPathRegex());
			}
			Resource pathResource = verticleResource.getResources().get(path);
			if (pathResource == null) {
				pathResource = new Resource();
			}
			if (endpoint.getMethod() == null) {
				continue;
			}
			pathResource.getActions().put(getActionType(endpoint.getMethod()), action);

			verticleResource.getResources().put(path, pathResource);

		}
		//verticleResource.setDisplayName("D:" + System.currentTimeMillis());
		resources.put(basePath + "/" + vericle.getBasePath(), verticleResource);

	}

	/**
	 * Convert the http method to a RAML action type.
	 * 
	 * @param method
	 * @return
	 */
	private ActionType getActionType(HttpMethod method) {
		return ActionType.valueOf(method.name());
	}

	private void initVerticle(AbstractWebVerticle verticle) throws Exception {
		Mockito.when(verticle.getRouter()).thenReturn(Router.router(Vertx.vertx()));
		MeshSpringConfiguration mockConfig = Mockito.mock(MeshSpringConfiguration.class);
		Mockito.when(verticle.getSpringConfiguration()).thenReturn(mockConfig);
		// Router mockRouter = Mockito.mock(Router.class);
		// Mockito.when(verticle.getRouter()).thenReturn(mockRouter);
		// NodeCrudHandler mockHandler = Mockito.mock(NodeCrudHandler.class);
		// Mockito.when(verticle.getCrudHandler()).thenReturn(mockHandler);

		verticle.registerEndPoints();
	}

	private void addProjectVerticles(Map<String, Resource> resources) throws Exception {
		NodeVerticle nodeVerticle = Mockito.spy(new NodeVerticle());
		initVerticle(nodeVerticle);
		String projectBasePath = "/:projectName";
		addEndpoints(projectBasePath, resources, nodeVerticle);

		TagFamilyVerticle tagFamilyVerticle = Mockito.spy(new TagFamilyVerticle());
		initVerticle(tagFamilyVerticle);
		addEndpoints(projectBasePath, resources, tagFamilyVerticle);

		NavRootVerticle navVerticle = Mockito.spy(new NavRootVerticle());
		initVerticle(navVerticle);
		addEndpoints(projectBasePath, resources, navVerticle);

		// TagCloudVerticle tagCloudVerticle = Mockito.spy(new TagCloudVerticle());
		// initVerticle(tagCloudVerticle);
		// addEndpoints(apiResource, tagCloudVerticle);

		WebRootVerticle webVerticle = Mockito.spy(new WebRootVerticle());
		initVerticle(webVerticle);
		addEndpoints(projectBasePath, resources, webVerticle);

		ReleaseVerticle releaseVerticle = Mockito.spy(new ReleaseVerticle());
		initVerticle(releaseVerticle);
		addEndpoints(projectBasePath, resources, releaseVerticle);

		ProjectSearchVerticle projectSearchVerticle = Mockito.spy(new ProjectSearchVerticle());
		initVerticle(projectSearchVerticle);
		addEndpoints(projectBasePath, resources, projectSearchVerticle);

		ProjectMicroschemaVerticle projectMicroschemaVerticle = Mockito.spy(new ProjectMicroschemaVerticle());
		initVerticle(projectMicroschemaVerticle);
		addEndpoints(projectBasePath, resources, projectMicroschemaVerticle);

	}

	private void addCoreVerticles(Map<String, Resource> resources) throws Exception {
		String coreBasePath = "";
		UserVerticle userVerticle = Mockito.spy(new UserVerticle());
		initVerticle(userVerticle);
		addEndpoints(coreBasePath, resources, userVerticle);

		RoleVerticle roleVerticle = Mockito.spy(new RoleVerticle());
		initVerticle(roleVerticle);
		addEndpoints(coreBasePath, resources, roleVerticle);

		GroupVerticle groupVerticle = Mockito.spy(new GroupVerticle());
		initVerticle(groupVerticle);
		addEndpoints(coreBasePath, resources, groupVerticle);

		ProjectVerticle projectVerticle = Mockito.spy(new ProjectVerticle());
		initVerticle(projectVerticle);
		addEndpoints(coreBasePath, resources, projectVerticle);

		SchemaVerticle schemaVerticle = Mockito.spy(new SchemaVerticle());
		initVerticle(schemaVerticle);
		addEndpoints(coreBasePath, resources, schemaVerticle);

		MicroschemaVerticle microschemaVerticle = Mockito.spy(new MicroschemaVerticle());
		initVerticle(microschemaVerticle);
		addEndpoints(coreBasePath, resources, microschemaVerticle);

		AdminVerticle adminVerticle = Mockito.spy(new AdminVerticle());
		initVerticle(adminVerticle);
		addEndpoints(coreBasePath, resources, adminVerticle);

		SearchVerticle searchVerticle = Mockito.spy(new SearchVerticle());
		initVerticle(searchVerticle);
		addEndpoints(coreBasePath, resources, searchVerticle);

		UtilityVerticle utilityVerticle = Mockito.spy(new UtilityVerticle());
		initVerticle(utilityVerticle);
		addEndpoints(coreBasePath, resources, utilityVerticle);

		AuthenticationVerticle authVerticle = Mockito.spy(new AuthenticationVerticle());
		initVerticle(authVerticle);
		addEndpoints(coreBasePath, resources, authVerticle);

	}
}
