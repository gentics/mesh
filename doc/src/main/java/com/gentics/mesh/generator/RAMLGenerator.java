package com.gentics.mesh.generator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.mockito.Mockito;
import org.raml.emitter.RamlEmitter;
import org.raml.model.Action;
import org.raml.model.ActionType;
import org.raml.model.Protocol;
import org.raml.model.Raml;
import org.raml.model.Resource;
import org.raml.model.Response;

import com.gentics.mesh.core.AbstractEndpoint;
import com.gentics.mesh.core.verticle.admin.AdminEndpoint;
import com.gentics.mesh.core.verticle.auth.AuthenticationEndpoint;
import com.gentics.mesh.core.verticle.eventbus.EventbusEndpoint;
import com.gentics.mesh.core.verticle.group.GroupEndpoint;
import com.gentics.mesh.core.verticle.microschema.MicroschemaEndpoint;
import com.gentics.mesh.core.verticle.microschema.ProjectMicroschemaEndpoint;
import com.gentics.mesh.core.verticle.navroot.NavRootEndpoint;
import com.gentics.mesh.core.verticle.node.NodeEndpoint;
import com.gentics.mesh.core.verticle.project.ProjectEndpoint;
import com.gentics.mesh.core.verticle.project.ProjectInfoEndpoint;
import com.gentics.mesh.core.verticle.release.ReleaseEndpoint;
import com.gentics.mesh.core.verticle.role.RoleEndpoint;
import com.gentics.mesh.core.verticle.schema.ProjectSchemaEndpoint;
import com.gentics.mesh.core.verticle.schema.SchemaEndpoint;
import com.gentics.mesh.core.verticle.tagfamily.TagFamilyEndpoint;
import com.gentics.mesh.core.verticle.user.UserEndpoint;
import com.gentics.mesh.core.verticle.utility.UtilityEndpoint;
import com.gentics.mesh.core.verticle.webroot.WebRootEndpoint;
import com.gentics.mesh.rest.Endpoint;
import com.gentics.mesh.search.ProjectSearchEndpoint;
import com.gentics.mesh.search.SearchEndpoint;

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
		raml.setVersion("0.7");
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
	private void addEndpoints(String basePath, Map<String, Resource> resources, AbstractEndpoint verticle) throws IOException {

		Resource verticleResource = new Resource();
		for (Endpoint endpoint : verticle.getEndpoints().stream().sorted().collect(Collectors.toList())) {

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
			if (endpoint.getExampleRequestMap() != null) {
				action.setBody(endpoint.getExampleRequestMap());

				for (String mimeType : endpoint.getExampleRequestMap().keySet()) {
					String body = endpoint.getExampleRequestMap().get(mimeType).getExample();
					if (mimeType.equalsIgnoreCase("application/json")) {
						//write example request to dedicated file
						String filename = "request/" + fullPath + "/request-body.json";
						writeFile(filename, body);
					} else if (mimeType.equalsIgnoreCase("text/plain")) {
						//write example request to dedicated file
						String filename = "request/" + fullPath + "/request-body.txt";
						writeFile(filename, body);
					}

				}

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

	private void initVerticle(AbstractEndpoint verticle) throws Exception {
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
		NodeEndpoint nodeVerticle = Mockito.spy(new NodeEndpoint());
		initVerticle(nodeVerticle);
		String projectBasePath = "/{project}";
		addEndpoints(projectBasePath, resources, nodeVerticle);

		TagFamilyEndpoint tagFamilyVerticle = Mockito.spy(new TagFamilyEndpoint());
		initVerticle(tagFamilyVerticle);
		addEndpoints(projectBasePath, resources, tagFamilyVerticle);

		NavRootEndpoint navVerticle = Mockito.spy(new NavRootEndpoint());
		initVerticle(navVerticle);
		addEndpoints(projectBasePath, resources, navVerticle);

		WebRootEndpoint webVerticle = Mockito.spy(new WebRootEndpoint());
		initVerticle(webVerticle);
		addEndpoints(projectBasePath, resources, webVerticle);

		ReleaseEndpoint releaseVerticle = Mockito.spy(new ReleaseEndpoint());
		initVerticle(releaseVerticle);
		addEndpoints(projectBasePath, resources, releaseVerticle);

		ProjectSearchEndpoint projectSearchVerticle = Mockito.spy(new ProjectSearchEndpoint());
		initVerticle(projectSearchVerticle);
		addEndpoints(projectBasePath, resources, projectSearchVerticle);

		ProjectSchemaEndpoint projectSchemaVerticle = Mockito.spy(new ProjectSchemaEndpoint());
		initVerticle(projectSchemaVerticle);
		addEndpoints(projectBasePath, resources, projectSchemaVerticle);

		ProjectMicroschemaEndpoint projectMicroschemaVerticle = Mockito.spy(new ProjectMicroschemaEndpoint());
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
		UserEndpoint userVerticle = Mockito.spy(new UserEndpoint());
		initVerticle(userVerticle);
		addEndpoints(coreBasePath, resources, userVerticle);

		ProjectInfoEndpoint projectInfoVerticle = Mockito.spy(new ProjectInfoEndpoint());
		initVerticle(projectInfoVerticle);
		addEndpoints(coreBasePath, resources, projectInfoVerticle);

		RoleEndpoint roleVerticle = Mockito.spy(new RoleEndpoint());
		initVerticle(roleVerticle);
		addEndpoints(coreBasePath, resources, roleVerticle);

		GroupEndpoint groupVerticle = Mockito.spy(new GroupEndpoint());
		initVerticle(groupVerticle);
		addEndpoints(coreBasePath, resources, groupVerticle);

		ProjectEndpoint projectVerticle = Mockito.spy(new ProjectEndpoint());
		initVerticle(projectVerticle);
		addEndpoints(coreBasePath, resources, projectVerticle);

		SchemaEndpoint schemaVerticle = Mockito.spy(new SchemaEndpoint());
		initVerticle(schemaVerticle);
		addEndpoints(coreBasePath, resources, schemaVerticle);

		MicroschemaEndpoint microschemaVerticle = Mockito.spy(new MicroschemaEndpoint());
		initVerticle(microschemaVerticle);
		addEndpoints(coreBasePath, resources, microschemaVerticle);

		AdminEndpoint adminVerticle = Mockito.spy(new AdminEndpoint());
		initVerticle(adminVerticle);
		addEndpoints(coreBasePath, resources, adminVerticle);

		SearchEndpoint searchVerticle = Mockito.spy(new SearchEndpoint());
		initVerticle(searchVerticle);
		addEndpoints(coreBasePath, resources, searchVerticle);

		UtilityEndpoint utilityVerticle = Mockito.spy(new UtilityEndpoint());
		initVerticle(utilityVerticle);
		addEndpoints(coreBasePath, resources, utilityVerticle);

		AuthenticationEndpoint authVerticle = Mockito.spy(new AuthenticationEndpoint());
		initVerticle(authVerticle);
		addEndpoints(coreBasePath, resources, authVerticle);

		EventbusEndpoint eventbusVerticle = Mockito.spy(new EventbusEndpoint());
		initVerticle(eventbusVerticle);
		addEndpoints(coreBasePath, resources, eventbusVerticle);

	}
}
