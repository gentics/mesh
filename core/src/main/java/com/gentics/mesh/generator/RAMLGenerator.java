package com.gentics.mesh.generator;

import static com.gentics.mesh.MeshVersion.CURRENT_API_VERSION;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

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

import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.core.endpoint.admin.AdminEndpoint;
import com.gentics.mesh.core.endpoint.admin.HealthEndpoint;
import com.gentics.mesh.core.endpoint.admin.RestInfoEndpoint;
import com.gentics.mesh.core.endpoint.auth.AuthenticationEndpoint;
import com.gentics.mesh.core.endpoint.branch.BranchEndpoint;
import com.gentics.mesh.core.endpoint.eventbus.EventbusEndpoint;
import com.gentics.mesh.core.endpoint.group.GroupEndpoint;
import com.gentics.mesh.core.endpoint.microschema.MicroschemaEndpoint;
import com.gentics.mesh.core.endpoint.microschema.ProjectMicroschemaEndpoint;
import com.gentics.mesh.core.endpoint.navroot.NavRootEndpoint;
import com.gentics.mesh.core.endpoint.node.NodeEndpoint;
import com.gentics.mesh.core.endpoint.project.ProjectEndpoint;
import com.gentics.mesh.core.endpoint.project.ProjectInfoEndpoint;
import com.gentics.mesh.core.endpoint.role.RoleEndpoint;
import com.gentics.mesh.core.endpoint.schema.ProjectSchemaEndpoint;
import com.gentics.mesh.core.endpoint.schema.SchemaEndpoint;
import com.gentics.mesh.core.endpoint.tagfamily.TagFamilyEndpoint;
import com.gentics.mesh.core.endpoint.user.UserEndpoint;
import com.gentics.mesh.core.endpoint.utility.UtilityEndpoint;
import com.gentics.mesh.core.endpoint.webroot.WebRootEndpoint;
import com.gentics.mesh.graphql.GraphQLEndpoint;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.APIRouterImpl;
import com.gentics.mesh.router.RootRouterImpl;
import com.gentics.mesh.router.RouterStorageImpl;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;
import com.gentics.mesh.search.ProjectRawSearchEndpointImpl;
import com.gentics.mesh.search.ProjectSearchEndpointImpl;
import com.gentics.mesh.search.RawSearchEndpointImpl;
import com.gentics.mesh.search.SearchEndpointImpl;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;

/**
 * Generator for RAML documentation. The generation mocks all endpoint classes and extracts the routes from these endpoints in order to generate the RAML.
 */
public class RAMLGenerator extends AbstractGenerator {

	private static final Logger log = LoggerFactory.getLogger(RAMLGenerator.class);

	private Raml raml = new Raml();

	/**
	 * Handler which can be invoked to replace the stored schema.
	 */
	private BiConsumer<MimeType, Class<?>> schemaHandler;

	private String fileName;

	private boolean writeExtraFiles = true;

	/**
	 * Create a new generator.
	 * 
	 * @param outputFolder
	 *            Output folder
	 * @param fileName
	 *            Output filename
	 * @throws IOException
	 */
	public RAMLGenerator(File outputFolder, String fileName, BiConsumer<MimeType, Class<?>> schemaHandler, boolean writeExtraFiles)
		throws IOException {
		super(new File(outputFolder, "api"), false);
		this.fileName = fileName;
		this.schemaHandler = schemaHandler;
		this.writeExtraFiles = writeExtraFiles;
	}

	public RAMLGenerator() {
		super();
	}

	/**
	 * Run the RAML generation.
	 * 
	 * @return Generated RAML
	 */
	public String generate() {
		log.info("Starting RAML generation...");
		raml.setTitle("Gentics Mesh REST API");
		raml.setVersion(MeshVersion.getBuildInfo().getVersion());
		raml.setBaseUri("http://localhost:8080/api/v" + CURRENT_API_VERSION);
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
	private void addEndpoints(String basePath, Map<String, Resource> resources, AbstractInternalEndpoint verticle) throws IOException {

		String ramlPath = basePath + "/" + verticle.getBasePath();
		// Check whether the resource was already added. Maybe we just need to extend it
		Resource verticleResource = resources.get(ramlPath);
		if (verticleResource == null) {
			verticleResource = new Resource();
			verticleResource.setDisplayName(basePath + "/" + verticle.getBasePath());
			verticleResource.setDescription(verticle.getDescription());
			resources.put(ramlPath, verticleResource);
		}
		for (InternalEndpointRoute endpoint : verticle.getEndpoints().stream().sorted().collect(Collectors.toList())) {

			String fullPath = "api/v" + CURRENT_API_VERSION + basePath + "/" + verticle.getBasePath() + endpoint.getRamlPath();
			if (isEmpty(verticle.getBasePath())) {
				fullPath = "api/v" + CURRENT_API_VERSION + basePath + endpoint.getRamlPath();
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
				// write example response to dedicated file
				if (response.getBody() != null && response.getBody().get("application/json") != null) {
					String filename = "response/" + fullPath + "/" + key + "/example.json";
					MimeType responseMimeType = response.getBody().get("application/json");
					String json = responseMimeType.getExample();
					writeFile(filename, json);

					// Write JSON schema to dedicated file
					String schemaFilename = "response/" + fullPath + "/" + key + "/request-schema.json";

					String schema = responseMimeType.getSchema();
					writeFile(schemaFilename, schema);

					// Check whether a custom schema handler has been set. The schema handler may replace the previously set schema with a different one.
					if (schemaHandler != null) {
						Class<?> clazz = endpoint.getExampleResponseClasses().get(entry.getKey());
						if (clazz != null) {
							schemaHandler.accept(responseMimeType, clazz);
						}
					}
				}

				action.getResponses().put(key, response);
			}

			// Add request example
			if (endpoint.getExampleRequestMap() != null) {
				action.setBody(endpoint.getExampleRequestMap());
				for (String mimeType : endpoint.getExampleRequestMap().keySet()) {
					MimeType request = endpoint.getExampleRequestMap().get(mimeType);
					String body = request.getExample();
					if (mimeType.equalsIgnoreCase("application/json")) {
						// Write example request to dedicated file
						String requestFilename = "request/" + fullPath + "/request-body.json";
						if (writeExtraFiles) {
							writeFile(requestFilename, body);
						}

						// Write JSON schema to dedicated file
						String filename = "request/" + fullPath + "/request-schema.json";
						String schema = request.getSchema();
						if (writeExtraFiles) {
							writeFile(filename, schema);
						}

						if (schemaHandler != null) {
							Class<?> clazz = endpoint.getExampleRequestClass();
							if (clazz != null) {
								schemaHandler.accept(request, clazz);
							}
						}

					} else if (mimeType.equalsIgnoreCase("text/plain")) {
						// Write example request to dedicated file
						String filename = "request/" + fullPath + "/request-body.txt";
						if (writeExtraFiles) {
							writeFile(filename, body);
						}
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
	 *            Name of the file to be written to
	 * @param content
	 *            Content to be written
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

	private void initEndpoint(AbstractInternalEndpoint endpoint) {
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

		BranchEndpoint branchEndpoint = Mockito.spy(new BranchEndpoint());
		initEndpoint(branchEndpoint);
		addEndpoints(projectBasePath, resources, branchEndpoint);

		GraphQLEndpoint graphqlEndpoint = Mockito.spy(new GraphQLEndpoint());
		initEndpoint(graphqlEndpoint);
		addEndpoints(projectBasePath, resources, graphqlEndpoint);

		ProjectSearchEndpointImpl projectSearchEndpoint = Mockito.spy(new ProjectSearchEndpointImpl());
		initEndpoint(projectSearchEndpoint);
		addEndpoints(projectBasePath, resources, projectSearchEndpoint);

		ProjectRawSearchEndpointImpl projectRawSearchEndpoint = Mockito.spy(new ProjectRawSearchEndpointImpl());
		initEndpoint(projectRawSearchEndpoint);
		addEndpoints(projectBasePath, resources, projectRawSearchEndpoint);

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

		HealthEndpoint healthEndpoint = Mockito.spy(new HealthEndpoint());
		initEndpoint(healthEndpoint);
		addEndpoints(coreBasePath, resources, healthEndpoint);

		SearchEndpointImpl searchEndpoint = Mockito.spy(new SearchEndpointImpl());
		initEndpoint(searchEndpoint);
		addEndpoints(coreBasePath, resources, searchEndpoint);

		RawSearchEndpointImpl rawSearchEndpoint = Mockito.spy(new RawSearchEndpointImpl());
		initEndpoint(rawSearchEndpoint);
		addEndpoints(coreBasePath, resources, rawSearchEndpoint);

		UtilityEndpoint utilityEndpoint = Mockito.spy(new UtilityEndpoint());
		initEndpoint(utilityEndpoint);
		addEndpoints(coreBasePath, resources, utilityEndpoint);

		AuthenticationEndpoint authEndpoint = Mockito.spy(new AuthenticationEndpoint());
		initEndpoint(authEndpoint);
		addEndpoints(coreBasePath, resources, authEndpoint);

		EventbusEndpoint eventbusEndpoint = Mockito.spy(new EventbusEndpoint());
		initEndpoint(eventbusEndpoint);
		addEndpoints(coreBasePath, resources, eventbusEndpoint);

		RouterStorageImpl rs = Mockito.mock(RouterStorageImpl.class);
		RootRouterImpl rootRouter = Mockito.mock(RootRouterImpl.class);
		Mockito.when(rs.root()).thenReturn(rootRouter);
		APIRouterImpl apiRouter = Mockito.mock(APIRouterImpl.class);
		Mockito.when(rootRouter.apiRouter()).thenReturn(apiRouter);
		RestInfoEndpoint infoEndpoint = Mockito.spy(new RestInfoEndpoint(""));
		infoEndpoint.init(null, rs);
		initEndpoint(infoEndpoint);
		addEndpoints(coreBasePath, resources, infoEndpoint);

		ProjectInfoEndpoint projectInfoEndpoint = Mockito.spy(new ProjectInfoEndpoint());
		initEndpoint(projectInfoEndpoint);
		addEndpoints(coreBasePath, resources, projectInfoEndpoint);

	}

	/**
	 * Start the generator.
	 * 
	 * @throws IOException
	 */
	public void run() throws IOException {
		String raml = generate();
		writeFile(fileName, raml);
	}
}
