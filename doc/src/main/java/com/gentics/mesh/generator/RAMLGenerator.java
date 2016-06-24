package com.gentics.mesh.generator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import org.mockito.Mockito;
import org.raml.emitter.RamlEmitter;
import org.raml.model.Action;
import org.raml.model.ActionType;
import org.raml.model.MimeType;
import org.raml.model.Raml;
import org.raml.model.Resource;
import org.raml.model.Response;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.core.verticle.user.UserVerticle;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.Endpoint;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

public class RAMLGenerator {

	Raml raml = new Raml();
	Resource apiResource = new Resource();

	public static void main(String[] args) throws Exception {
		new RAMLGenerator().generator();
	}

	public void generator() throws Exception {

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
			for (Entry<Integer, RestModel> entry : endpoint.getExampleResponses().entrySet()) {
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

			Resource pathResource = new Resource();

			pathResource.getActions().put(ActionType.GET, action);
			String path = endpoint.getPath();
			if (path == null) {
				path = endpoint.getPathRegex();
			}
			verticleResource.getResources().put(path, pathResource);

		}
		baseResource.getResources().put("/" + vericle.getBasePath(), verticleResource);

	}

	private void initVerticle(AbstractWebVerticle verticle) throws Exception {
		Mockito.when(verticle.getRouter()).thenReturn(Router.router(Vertx.vertx()));
		MeshSpringConfiguration mockConfig = Mockito.mock(MeshSpringConfiguration.class);
		Mockito.when(verticle.getSpringConfiguration()).thenReturn(mockConfig);

		// NodeCrudHandler mockHandler = Mockito.mock(NodeCrudHandler.class);
		// Mockito.when(verticle.getCrudHandler()).thenReturn(mockHandler);

		verticle.registerEndPoints();
	}

	private void addProjectVerticles(Raml raml) throws Exception {
		NodeVerticle verticle = Mockito.spy(new NodeVerticle());
		initVerticle(verticle);

		// raml.getResources()
		addEndpoints(apiResource, verticle);
	}

	private void addCoreVerticles(Raml raml) throws Exception {
		UserVerticle userVerticle = Mockito.spy(new UserVerticle());
		initVerticle(userVerticle);
		addEndpoints(apiResource, userVerticle);
	}
}
