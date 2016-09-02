package com.gentics.mesh.generator;

import java.io.File;
import java.io.IOException;
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
import com.gentics.mesh.core.verticle.eventbus.EventbusVerticle;
import com.gentics.mesh.core.verticle.group.GroupVerticle;
import com.gentics.mesh.core.verticle.microschema.MicroschemaVerticle;
import com.gentics.mesh.core.verticle.microschema.ProjectMicroschemaVerticle;
import com.gentics.mesh.core.verticle.navroot.NavRootVerticle;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.core.verticle.project.ProjectVerticle;
import com.gentics.mesh.core.verticle.release.ReleaseVerticle;
import com.gentics.mesh.core.verticle.role.RoleVerticle;
import com.gentics.mesh.core.verticle.schema.ProjectSchemaVerticle;
import com.gentics.mesh.core.verticle.schema.SchemaVerticle;
import com.gentics.mesh.core.verticle.tagfamily.TagFamilyVerticle;
import com.gentics.mesh.core.verticle.user.UserVerticle;
import com.gentics.mesh.core.verticle.utility.UtilityVerticle;
import com.gentics.mesh.core.verticle.webroot.WebRootVerticle;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.Endpoint;
import com.gentics.mesh.search.ProjectSearchVerticle;
import com.gentics.mesh.search.SearchVerticle;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;

/**
 * Generator for RAML documentation.
 */
public class RAMLGenerator {

	private static final Logger log = LoggerFactory.getLogger(RAMLGenerator.class);

	private Raml raml = new Raml();

	private static File outputFolder = new File("target", "api");

	public static void main(String[] args) throws Exception {
		if (outputFolder.exists()) {
			FileUtils.deleteDirectory(outputFolder);
		}
		new RAMLGenerator().run();
	}

	/**
	 * Run the RAML generation.
	 * 
	 * @throws Exception
	 */
	public void run() throws Exception {
		log.info("Starting RAML generation...");
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
		writeFile("api.raml", dumpFromRaml);
		log.info("RAML generation completed.");
	}

	/**
	 * Add the endpoinnts for the given verticle to the RAML data structure.
	 * 
	 * @param basePath
	 * @param resources
	 * @param verticle
	 *            Verticle which provides endpoints
	 * @throws IOException
	 */
	private void addEndpoints(String basePath, Map<String, Resource> resources, AbstractWebVerticle verticle) throws IOException {

		Resource verticleResource = new Resource();
		for (Endpoint endpoint : verticle.getEndpoints()) {

			String fullPath = "api/v1" + basePath + "/" + verticle.getBasePath() + endpoint.getRamlPath();
			Action action = new Action();
			action.setIs(Arrays.asList(endpoint.getTraits()));
			action.setDisplayName(endpoint.getDisplayName());
			action.setDescription(endpoint.getDescription());
			action.setQueryParameters(endpoint.getQueryParameters());

			// Add response examples
			for (Entry<Integer, Response> entry : endpoint.getExampleResponses().entrySet()) {
				String key = String.valueOf(entry.getKey());
				Response response = entry.getValue();
				//write example response to dedicated file
				String filename = "response/" + fullPath + "/" + key + "/example.json";
				if (response.getBody() != null && response.getBody().get("application/json") != null) {
					String json = response.getBody().get("application/json").getExample();
					writeFile(filename, json);
				}
				action.getResponses().put(key, response);
			}

			// Add request example
			if (endpoint.getExampleRequest() != null) {
				HashMap<String, MimeType> bodyMap = new HashMap<>();
				MimeType mimeType = new MimeType();
				String json = JsonUtil.toJson(endpoint.getExampleRequest());
				mimeType.setExample(json);
				bodyMap.put("application/json", mimeType);
				action.setBody(bodyMap);

				//write example request to dedicated file
				String filename = "request/" + fullPath + "/" + endpoint.getExampleRequest().getClass().getSimpleName() + ".json";
				writeFile(filename, json);
			}

			String path = endpoint.getRamlPath();
			if (path == null) {
				throw new RuntimeException(
						"Could not determine path for endpoint of verticle " + verticle.getClass() + " " + endpoint.getPathRegex());
			}
			Resource pathResource = verticleResource.getResources().get(path);
			if (pathResource == null) {
				pathResource = new Resource();
			}
			if (endpoint.getMethod() == null) {
				continue;
			}
			pathResource.getActions().put(getActionType(endpoint.getMethod()), action);
			pathResource.setUriParameters(endpoint.getUriParameters());
			verticleResource.getResources().put(path, pathResource);

		}
		verticleResource.setDisplayName(basePath + "/" + verticle.getBasePath());
		verticleResource.setDescription(verticle.getDescription());
		//action.setBaseUriParameters(endpoint.getUriParameters());
		resources.put(basePath + "/" + verticle.getBasePath(), verticleResource);

	}

	/**
	 * Save the string content to the given file in the output folder.
	 * 
	 * @param filename
	 * @param content
	 * @throws IOException
	 */
	private void writeFile(String filename, String content) throws IOException {
		File outputFile = new File(outputFolder, filename);
		FileUtils.writeStringToFile(outputFile, content);
		log.info("File saved to {" + outputFile.getPath() + "}");
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
		verticle.registerEndPoints();
	}

	/**
	 * Add all project verticles to the list resources.
	 * 
	 * @param resources
	 * @throws Exception
	 */
	private void addProjectVerticles(Map<String, Resource> resources) throws Exception {
		NodeVerticle nodeVerticle = Mockito.spy(new NodeVerticle());
		initVerticle(nodeVerticle);
		String projectBasePath = "/{project}";
		addEndpoints(projectBasePath, resources, nodeVerticle);

		TagFamilyVerticle tagFamilyVerticle = Mockito.spy(new TagFamilyVerticle());
		initVerticle(tagFamilyVerticle);
		addEndpoints(projectBasePath, resources, tagFamilyVerticle);

		NavRootVerticle navVerticle = Mockito.spy(new NavRootVerticle());
		initVerticle(navVerticle);
		addEndpoints(projectBasePath, resources, navVerticle);

		WebRootVerticle webVerticle = Mockito.spy(new WebRootVerticle());
		initVerticle(webVerticle);
		addEndpoints(projectBasePath, resources, webVerticle);

		ReleaseVerticle releaseVerticle = Mockito.spy(new ReleaseVerticle());
		initVerticle(releaseVerticle);
		addEndpoints(projectBasePath, resources, releaseVerticle);

		ProjectSearchVerticle projectSearchVerticle = Mockito.spy(new ProjectSearchVerticle());
		initVerticle(projectSearchVerticle);
		addEndpoints(projectBasePath, resources, projectSearchVerticle);

		ProjectSchemaVerticle projectSchemaVerticle = Mockito.spy(new ProjectSchemaVerticle());
		initVerticle(projectSchemaVerticle);
		addEndpoints(projectBasePath, resources, projectSchemaVerticle);

		ProjectMicroschemaVerticle projectMicroschemaVerticle = Mockito.spy(new ProjectMicroschemaVerticle());
		initVerticle(projectMicroschemaVerticle);
		addEndpoints(projectBasePath, resources, projectMicroschemaVerticle);

	}

	/**
	 * Add all core verticles to the map of RAML resources.
	 * 
	 * @param resources
	 * @throws Exception
	 */
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

		EventbusVerticle eventbusVerticle = Mockito.spy(new EventbusVerticle());
		initVerticle(eventbusVerticle);
		addEndpoints(coreBasePath, resources, eventbusVerticle);

	}
}
