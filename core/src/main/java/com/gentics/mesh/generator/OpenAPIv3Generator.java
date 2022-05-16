package com.gentics.mesh.generator;

import static com.gentics.mesh.MeshVersion.CURRENT_API_VERSION;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.keyvalue.UnmodifiableMapEntry;
import org.raml.model.parameter.AbstractParam;
import org.reflections.Reflections;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.servers.Server;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class OpenAPIv3Generator extends AbstractEndpointGenerator<OpenAPI> {

	private static final Logger log = LoggerFactory.getLogger(OpenAPIv3Generator.class);

	public OpenAPIv3Generator() {
	}

	public OpenAPIv3Generator(File outputFolder, boolean cleanup) throws IOException {
		super(outputFolder, cleanup);
	}

	public OpenAPIv3Generator(File outputFolder) throws IOException {
		super(outputFolder);
	}

	@SuppressWarnings("rawtypes")
	public void generate(InternalActionContext ac, String format) {
		log.info("Starting OpenAPIv3 generation...");
		OpenAPI openApi = new OpenAPI();
		Info info = new Info();
		Server server = new Server();
		info.setTitle("Gentics Mesh REST API");
		info.setVersion(MeshVersion.getBuildInfo().getVersion());
		//server.setUrl("http://localhost:8080/api/v" + CURRENT_API_VERSION);
		openApi.servers(Collections.singletonList(server));
        openApi.setInfo(info);
        Components components = new Components();
        Reflections reflections = new Reflections("com.gentics.mesh");
        // TODO parse RestModels into components
		openApi.setComponents(components);
		try {
			addCoreEndpoints(openApi);
			addProjectEndpoints(openApi);
		} catch (IOException e) {
			throw new RuntimeException("Could not add all verticles to raml generator", e);
		}

		String formatted;
		switch (format) {
		case "yaml":
			try {
				formatted = Yaml.pretty().writeValueAsString(openApi);
			} catch (JsonProcessingException e) {
				throw new RuntimeException("Could not generate YAML", e);
			}
			break;
		case "json":
			formatted = Json.pretty(openApi);
			break;
		default:
			throw error(BAD_REQUEST, "Please specify a response format: YAML or JSON");
		}		
		ac.send(formatted, OK, APPLICATION_JSON);
	}

	@Override
	protected void addEndpoints(String basePath, OpenAPI consumer, AbstractInternalEndpoint verticle)
			throws IOException {
		// Check whether the resource was already added. Maybe we just need to extend it
		Paths paths = consumer.getPaths();
		if (paths == null) {
			paths = new Paths();
			consumer.setPaths(paths);
		}
		for (InternalEndpointRoute endpoint : verticle.getEndpoints().stream().sorted().collect(Collectors.toList())) {
			String fullPath = "api/v" + CURRENT_API_VERSION + basePath + "/" + verticle.getBasePath() + endpoint.getRamlPath();
			if (isEmpty(verticle.getBasePath())) {
				fullPath = "api/v" + CURRENT_API_VERSION + basePath + endpoint.getRamlPath();
			}
			PathItem pathItem = paths.get(fullPath);
			if (pathItem == null) {
				pathItem = new PathItem();
				pathItem.setSummary(endpoint.getDisplayName());
				pathItem.setDescription(endpoint.getDescription());
				consumer.path(fullPath, pathItem);
			}
			Operation operation = new Operation();
			HttpMethod method = endpoint.getMethod();
			if (method == null) {
				method = HttpMethod.GET;
			}
			switch (method) {
			case DELETE:
				pathItem.setDelete(operation);
				break;
			case GET:
				pathItem.setGet(operation);
				break;
			case HEAD:
				pathItem.setHead(operation);
				break;
			case OPTIONS:
				pathItem.setOptions(operation);
				break;
			case PATCH:
				pathItem.setPatch(operation);
				break;
			case POST:
				pathItem.setPost(operation);
				break;
			case PUT:
				pathItem.setPut(operation);
				break;
			case TRACE:
				pathItem.setTrace(operation);
				break;
			default:
				break;			
			}
			List<Stream<Parameter>> params = List.of(
					endpoint.getQueryParameters().entrySet().stream().map(e -> parameter(e.getKey(), e.getValue(), InParameter.QUERY)),
					endpoint.getUriParameters().entrySet().stream().map(e -> parameter(e.getKey(), e.getValue(), InParameter.PATH))
			);
			operation.setParameters(params.stream().flatMap(Function.identity()).filter(Objects::nonNull).collect(Collectors.toList()));
			RequestBody requestBody = new RequestBody();
			Content content = new Content();
			if (endpoint.getExampleRequestMap() != null) {
				endpoint.getExampleRequestMap().entrySet().stream().filter(e -> Objects.nonNull(e.getValue()))
				.map(e -> {
					MediaType mediaType = new MediaType();
					mediaType.setExample(e.getValue().getExample());
					JsonObject jschema = new JsonObject(e.getValue().getSchema());
					Schema<String> schema = new Schema<>();
					schema.setType(jschema.getString("type", "string"));
					schema.set$id(jschema.getString("id"));
					mediaType.setSchema(schema);
					return new UnmodifiableMapEntry<String, MediaType>(e.getKey(), mediaType);
				}).filter(Objects::nonNull).forEach(e -> content.addMediaType(e.getKey(), e.getValue()));
			}
			requestBody.setContent(content);
			operation.setRequestBody(requestBody);
			//action.setIs(Arrays.asList(endpoint.getTraits()));
		}
	}


	private final Parameter parameter(String name, AbstractParam param, InParameter inType) {
		Schema schema;
		switch (param.getType()) {
		case BOOLEAN:
			schema = new Schema<Boolean>();
			break;
		case DATE:
			schema = new Schema<Long>();
			break;
		case FILE:
			schema = new Schema<File>();
			break;
		case INTEGER:
			schema = new Schema<Integer>();
			break;
		case NUMBER:
			schema = new Schema<Number>();
			break;
		case STRING:
			schema = new Schema<String>();
			break;
		default:
			return null;				
		}
		schema.setMinimum(param.getMinimum());
		schema.setMaximum(param.getMaximum());
		schema.setMinLength(param.getMinLength());
		schema.setMaxLength(param.getMaxLength());
		schema.setDefault(param.getDefaultValue());
		schema.setEnum(param.getEnumeration());
		schema.setExample(param.getExample());
		schema.setPattern(param.getPattern());
		schema.setDescription(param.getDescription());
		Parameter p = new Parameter();
		p.setRequired(param.isRequired());
		p.setDescription(param.getDescription());
		p.setIn(inType.value);
		p.setSchema(schema);
		p.setName(name);
		return p;
	}

	private enum InParameter {
		PATH("path"),
		QUERY("query"),
		HEADER("header"),
		COOKIE("cookie");
		
		private final String value;

		private InParameter(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}
}
