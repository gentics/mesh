package com.gentics.mesh.generator;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mockito.Mockito.mock;

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
import com.gentics.mesh.core.verticle.admin.RestInfoEndpoint;
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
import com.gentics.mesh.etc.RouterStorage;
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

	private File outputFolder;

	public RAMLGenerator(File outputFolder) {
		this.outputFolder = outputFolder;
	}

	/**
	 * Run the RAML generation.
	 * 
	 * @param writeFiles
	 * @throws Exception
	 * @return Generated RAML
	 */
	public String generate() {
		log.info("Starting RAML generation...");
		raml.setTitle("Gentics Mesh REST API");
		raml.setVersion("0.7");
		raml.setBaseUri("http://localhost:8080/api/v1");
		raml.getProtocols().add(Protocol.HTTP);
		raml.getProtocols().add(Protocol.HTTPS);
		raml.setMediaType("application/json");

		try {
			addCoreEndpoints(raml.getResources());
			addProjectEndpoints(raml.getResources());
		} catch (IOException e) {
			throw new RuntimeException("Could not add all verticles to raml generator", e);
		}

		RamlEmitter emitter = new RamlEmitter();
		String dumpFromRaml = emitter.dump(raml);
		log.info("RAML generation completed.");
		return dumpFromRaml;
	}

	/**
	 * Add the endpoinnts for the given verticle to the RAML data structure.
	 * 
	 * @param basePath
	 * @param resources
	 * @param verticle
	 *            Endpoint which provides endpoints
	 * @throws IOException
	 */
	private void addEndpoints(String basePath, Map<String, Resource> resources, AbstractEndpoint verticle) throws IOException {

		String ramlPath = basePath + "/" + verticle.getBasePath();
		// Check whether the resource was already added. Maybe we just need to extend it
		Resource verticleResource = resources.get(ramlPath);
		if (verticleResource == null) {
			verticleResource = new Resource();
			verticleResource.setDisplayName(basePath + "/" + verticle.getBasePath());
			verticleResource.setDescription(verticle.getDescription());
			resources.put(ramlPath, verticleResource);
		}
		for (Endpoint endpoint : verticle.getEndpoints().stream().sorted().collect(Collectors.toList())) {

			String fullPath = "api/v1" + basePath + "/" + verticle.getBasePath() + endpoint.getRamlPath();
			if (isEmpty(verticle.getBasePath())) {
				fullPath = "api/v1" + basePath + endpoint.getRamlPath();
			}
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

	}

	/**
	 * Save the string content to the given file in the output folder.
	 * 
	 * @param filename
	 * @param content
	 * @throws IOException
	 */
	public void writeFile(String filename, String content) throws IOException {
		if (outputFolder != null) {
			File outputFile = new File(outputFolder, filename);
			FileUtils.writeStringToFile(outputFile, content);
			log.info("File saved to {" + outputFile.getPath() + "}");
		}
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

	private void initEndpoint(AbstractEndpoint endpoint) {
		Vertx vertx = mock(Vertx.class);
		Mockito.when(endpoint.getRouter()).thenReturn(Router.router(vertx));
		endpoint.registerEndPoints();
	}

	/**
	 * Add all project verticles to the list resources.
	 * 
	 * @param resources
	 * @throws IOException
	 * @throws Exception
	 */
	private void addProjectEndpoints(Map<String, Resource> resources) throws IOException {
		NodeEndpoint nodeEndpoint = Mockito.spy(new NodeEndpoint());
		initEndpoint(nodeEndpoint);
		String projectBasePath = "/{project}";
		addEndpoints(projectBasePath, resources, nodeEndpoint);

		TagFamilyEndpoint tagFamilyEndpoint = Mockito.spy(new TagFamilyEndpoint());
		initEndpoint(tagFamilyEndpoint);
		addEndpoints(projectBasePath, resources, tagFamilyEndpoint);

		NavRootEndpoint navEndpoint = Mockito.spy(new NavRootEndpoint());
		initEndpoint(navEndpoint);
		addEndpoints(projectBasePath, resources, navEndpoint);

		WebRootEndpoint webEndpoint = Mockito.spy(new WebRootEndpoint());
		initEndpoint(webEndpoint);
		addEndpoints(projectBasePath, resources, webEndpoint);

		ReleaseEndpoint releaseEndpoint = Mockito.spy(new ReleaseEndpoint());
		initEndpoint(releaseEndpoint);
		addEndpoints(projectBasePath, resources, releaseEndpoint);

		ProjectSearchEndpoint projectSearchEndpoint = Mockito.spy(new ProjectSearchEndpoint());
		initEndpoint(projectSearchEndpoint);
		addEndpoints(projectBasePath, resources, projectSearchEndpoint);

		ProjectSchemaEndpoint projectSchemaEndpoint = Mockito.spy(new ProjectSchemaEndpoint());
		initEndpoint(projectSchemaEndpoint);
		addEndpoints(projectBasePath, resources, projectSchemaEndpoint);

		ProjectMicroschemaEndpoint projectMicroschemaEndpoint = Mockito.spy(new ProjectMicroschemaEndpoint());
		initEndpoint(projectMicroschemaEndpoint);
		addEndpoints(projectBasePath, resources, projectMicroschemaEndpoint);

	}

	/**
	 * Add all core verticles to the map of RAML resources.
	 * 
	 * @param resources
	 * @throws IOException
	 * @throws Exception
	 */
	private void addCoreEndpoints(Map<String, Resource> resources) throws IOException {
		String coreBasePath = "";
		UserEndpoint userEndpoint = Mockito.spy(new UserEndpoint());
		initEndpoint(userEndpoint);
		addEndpoints(coreBasePath, resources, userEndpoint);

		RoleEndpoint roleEndpoint = Mockito.spy(new RoleEndpoint());
		initEndpoint(roleEndpoint);
		addEndpoints(coreBasePath, resources, roleEndpoint);

		GroupEndpoint groupEndpoint = Mockito.spy(new GroupEndpoint());
		initEndpoint(groupEndpoint);
		addEndpoints(coreBasePath, resources, groupEndpoint);

		ProjectEndpoint projectEndpoint = Mockito.spy(new ProjectEndpoint());
		initEndpoint(projectEndpoint);
		addEndpoints(coreBasePath, resources, projectEndpoint);

		SchemaEndpoint schemaEndpoint = Mockito.spy(new SchemaEndpoint());
		initEndpoint(schemaEndpoint);
		addEndpoints(coreBasePath, resources, schemaEndpoint);

		MicroschemaEndpoint microschemaEndpoint = Mockito.spy(new MicroschemaEndpoint());
		initEndpoint(microschemaEndpoint);
		addEndpoints(coreBasePath, resources, microschemaEndpoint);

		AdminEndpoint adminEndpoint = Mockito.spy(new AdminEndpoint());
		initEndpoint(adminEndpoint);
		addEndpoints(coreBasePath, resources, adminEndpoint);

		SearchEndpoint searchEndpoint = Mockito.spy(new SearchEndpoint());
		initEndpoint(searchEndpoint);
		addEndpoints(coreBasePath, resources, searchEndpoint);

		UtilityEndpoint utilityEndpoint = Mockito.spy(new UtilityEndpoint());
		initEndpoint(utilityEndpoint);
		addEndpoints(coreBasePath, resources, utilityEndpoint);

		AuthenticationEndpoint authEndpoint = Mockito.spy(new AuthenticationEndpoint());
		initEndpoint(authEndpoint);
		addEndpoints(coreBasePath, resources, authEndpoint);

		EventbusEndpoint eventbusEndpoint = Mockito.spy(new EventbusEndpoint());
		initEndpoint(eventbusEndpoint);
		addEndpoints(coreBasePath, resources, eventbusEndpoint);

		RouterStorage storage = Mockito.mock(RouterStorage.class);
		RestInfoEndpoint infoEndpoint = Mockito.spy(new RestInfoEndpoint("", storage));
		initEndpoint(infoEndpoint);
		addEndpoints(coreBasePath, resources, infoEndpoint);

		ProjectInfoEndpoint projectInfoEndpoint = Mockito.spy(new ProjectInfoEndpoint());
		initEndpoint(projectInfoEndpoint);
		addEndpoints(coreBasePath, resources, projectInfoEndpoint);

	}
}
