package com.gentics.mesh.generator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

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
import com.gentics.mesh.core.rest.common.RestModel;
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
	Resource apiResource = new Resource();

	public static void main(String[] args) throws Exception {
		new RAMLGenerator().generator();
	}

	public void generator() throws Exception {

		raml.setTitle("Gentics Mesh REST API");
		raml.setVersion("1");
		raml.setBaseUri("http://localhost:8080");
		raml.getProtocols().add(Protocol.HTTP);
		raml.getProtocols().add(Protocol.HTTPS);
		raml.setMediaType("application/json");

		raml.getResources().put("/api/v1", apiResource);

		addCoreVerticles(raml);
		addProjectVerticles(raml);

		RamlEmitter emitter = new RamlEmitter();
		String dumpFromRaml = emitter.dump(raml);
		System.out.println(dumpFromRaml);
	}

	private void addEndpoints(Resource baseResource, AbstractWebVerticle vericle) {

		Resource verticleResource = new Resource();
		for (Endpoint endpoint : vericle.getEndpoints()) {

			Action action = new Action();
			action.setIs(Arrays.asList(endpoint.getTraits()));
			action.setDisplayName(endpoint.getDisplayName());
			action.setDescription(endpoint.getDescription());

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

			String path = endpoint.getPath();
			if (path == null) {
				path = endpoint.getPathRegex();
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
		baseResource.getResources().put("/" + vericle.getBasePath(), verticleResource);

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

	private void addProjectVerticles(Raml raml) throws Exception {
		NodeVerticle nodeVerticle = Mockito.spy(new NodeVerticle());
		initVerticle(nodeVerticle);
		addEndpoints(apiResource, nodeVerticle);

		TagFamilyVerticle tagFamilyVerticle = Mockito.spy(new TagFamilyVerticle());
		initVerticle(tagFamilyVerticle);
		addEndpoints(apiResource, tagFamilyVerticle);

		NavRootVerticle navVerticle = Mockito.spy(new NavRootVerticle());
		initVerticle(navVerticle);
		addEndpoints(apiResource, navVerticle);

		// TagCloudVerticle tagCloudVerticle = Mockito.spy(new TagCloudVerticle());
		// initVerticle(tagCloudVerticle);
		// addEndpoints(apiResource, tagCloudVerticle);

		WebRootVerticle webVerticle = Mockito.spy(new WebRootVerticle());
		initVerticle(webVerticle);
		addEndpoints(apiResource, webVerticle);

		ReleaseVerticle releaseVerticle = Mockito.spy(new ReleaseVerticle());
		initVerticle(releaseVerticle);
		addEndpoints(apiResource, releaseVerticle);

		ProjectSearchVerticle projectSearchVerticle = Mockito.spy(new ProjectSearchVerticle());
		initVerticle(projectSearchVerticle);
		addEndpoints(apiResource, projectSearchVerticle);

		ProjectMicroschemaVerticle projectMicroschemaVerticle = Mockito.spy(new ProjectMicroschemaVerticle());
		initVerticle(projectMicroschemaVerticle);
		addEndpoints(apiResource, projectMicroschemaVerticle);

	}

	private void addCoreVerticles(Raml raml) throws Exception {
		UserVerticle userVerticle = Mockito.spy(new UserVerticle());
		initVerticle(userVerticle);
		addEndpoints(apiResource, userVerticle);

		RoleVerticle roleVerticle = Mockito.spy(new RoleVerticle());
		initVerticle(roleVerticle);
		addEndpoints(apiResource, roleVerticle);

		GroupVerticle groupVerticle = Mockito.spy(new GroupVerticle());
		initVerticle(groupVerticle);
		addEndpoints(apiResource, groupVerticle);

		ProjectVerticle projectVerticle = Mockito.spy(new ProjectVerticle());
		initVerticle(projectVerticle);
		addEndpoints(apiResource, projectVerticle);

		SchemaVerticle schemaVerticle = Mockito.spy(new SchemaVerticle());
		initVerticle(schemaVerticle);
		addEndpoints(apiResource, schemaVerticle);

		MicroschemaVerticle microschemaVerticle = Mockito.spy(new MicroschemaVerticle());
		initVerticle(microschemaVerticle);
		addEndpoints(apiResource, microschemaVerticle);

		AdminVerticle adminVerticle = Mockito.spy(new AdminVerticle());
		initVerticle(adminVerticle);
		addEndpoints(apiResource, adminVerticle);

		SearchVerticle searchVerticle = Mockito.spy(new SearchVerticle());
		initVerticle(searchVerticle);
		addEndpoints(apiResource, searchVerticle);

		UtilityVerticle utilityVerticle = Mockito.spy(new UtilityVerticle());
		initVerticle(utilityVerticle);
		addEndpoints(apiResource, utilityVerticle);

		AuthenticationVerticle authVerticle = Mockito.spy(new AuthenticationVerticle());
		initVerticle(authVerticle);
		addEndpoints(apiResource, authVerticle);

	}
}
