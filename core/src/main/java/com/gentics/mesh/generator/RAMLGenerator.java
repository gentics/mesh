package com.gentics.mesh.generator;

import static com.gentics.mesh.MeshVersion.CURRENT_API_VERSION;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.raml.emitter.RamlEmitter;
import org.raml.model.Action;
import org.raml.model.ActionType;
import org.raml.model.MimeType;
import org.raml.model.Protocol;
import org.raml.model.Raml;
import org.raml.model.Resource;
import org.raml.model.Response;

import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Generator for RAML documentation. The generation mocks all endpoint classes and extracts the routes from these endpoints in order to generate the RAML.
 */
public class RAMLGenerator extends AbstractEndpointGenerator<Map<String, Resource>> {

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
	protected void addEndpoints(String basePath, Map<String, Resource> resources, AbstractInternalEndpoint verticle) throws IOException {

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
