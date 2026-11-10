package com.gentics.mesh.generator;

import static com.gentics.mesh.MeshVersion.CURRENT_API_BASE_PATH;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;

import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;

/**
 * Generator for the HTML REST API documentation and the request/response example files, based on the OpenAPI specification. The page reuses the markup
 * and CSS classes of the former raml2html based page.
 */
public class OpenAPIHtmlGenerator extends AbstractRenderingGenerator {

	public static final String SPEC_FILENAME = "openapi.yaml";
	public static final String HTML_FILENAME = "index.html";
	private static final String TEMPLATE_FOLDER = "/api-docs";
	private static final String SCHEMA_REF_PREFIX = "#/components/schemas/";

	private static final ObjectMapper EXAMPLE_MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

	/**
	 * Create the generator. The OpenAPI spec is read from and the output is written to the api subfolder of the given folder.
	 *
	 * @param outputRootFolder
	 * @throws IOException
	 */
	public OpenAPIHtmlGenerator(File outputRootFolder) throws IOException {
		super(new File(outputRootFolder, "api"), false);
	}

	/**
	 * Read the OpenAPI spec, then write the HTML page and the example files.
	 *
	 * @throws IOException
	 */
	public void run() throws IOException {
		OpenAPI openApi = Yaml.mapper().readValue(new File(outputFolder, SPEC_FILENAME), OpenAPI.class);
		Map<String, String> resources = new EndpointResourceCollector().collect();
		writeFile(HTML_FILENAME, render(openApi, resources));
		for (Entry<String, String> example : exampleFiles(openApi).entrySet()) {
			writeFile(example.getKey(), example.getValue());
		}
	}

	/**
	 * Render the HTML page.
	 *
	 * @param openApi
	 *            OpenAPI spec
	 * @param resources
	 *            Top level resources (e.g. /users, /{project}/nodes) with their description, in display order
	 * @return HTML
	 * @throws IOException
	 */
	public String render(OpenAPI openApi, Map<String, String> resources) throws IOException {
		Map<String, Map<String, Object>> topResources = new LinkedHashMap<>();
		for (Entry<String, String> resource : resources.entrySet()) {
			topResources.put(resource.getKey(), topResource(resource.getKey(), resource.getValue()));
		}

		for (Entry<String, PathItem> path : openApi.getPaths().entrySet()) {
			String relativePath = StringUtils.removeStart(path.getKey(), CURRENT_API_BASE_PATH);
			String parentUrl = findResource(relativePath, topResources.keySet());
			if (parentUrl == null) {
				parentUrl = fallbackResource(relativePath);
				topResources.computeIfAbsent(parentUrl, key -> topResource(key, null));
			}
			String relativeUri = relativePath.substring(parentUrl.endsWith("/") ? parentUrl.length() - 1 : parentUrl.length());
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> children = (List<Map<String, Object>>) topResources.get(parentUrl).get("resources");
			children.add(childResource(openApi, StringUtils.removeEnd(parentUrl, "/"), relativeUri, path.getValue()));
		}

		List<Map<String, Object>> rendered = new ArrayList<>(topResources.values());
		for (Map<String, Object> resource : rendered) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> children = (List<Map<String, Object>>) resource.get("resources");
			children.sort(Comparator.comparing(child -> (String) child.get("relativeUri")));
		}

		Map<String, Object> context = new HashMap<>();
		context.put("resources", rendered);
		Handlebars handlebars = new Handlebars(new ClassPathTemplateLoader(TEMPLATE_FOLDER, ".hbs"));
		return handlebars.compile("template").apply(context);
	}

	/**
	 * Collect the request and response example files, keyed by their file name relative to the output folder. The file names match the ones referenced by
	 * the documentation, e.g. response/api/v2/users/{userUuid}/200/example.json.
	 *
	 * @param openApi
	 * @return
	 * @throws IOException
	 */
	public Map<String, String> exampleFiles(OpenAPI openApi) throws IOException {
		Map<String, String> files = new LinkedHashMap<>();
		for (Entry<String, PathItem> path : openApi.getPaths().entrySet()) {
			String fullPath = StringUtils.removeStart(path.getKey(), "/");
			for (Operation operation : path.getValue().readOperations()) {
				if (operation.getResponses() != null) {
					for (Entry<String, ApiResponse> response : operation.getResponses().entrySet()) {
						MediaType json = response.getValue().getContent() == null ? null : response.getValue().getContent().get("application/json");
						if (StringUtils.isNumeric(response.getKey()) && json != null && json.getExample() != null) {
							files.put("response/" + fullPath + "/" + response.getKey() + "/example.json", exampleText(json.getExample()));
						}
					}
				}
				if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
					MediaType json = operation.getRequestBody().getContent().get("application/json");
					if (json != null && json.getExample() != null) {
						files.put("request/" + fullPath + "/request-body.json", exampleText(json.getExample()));
					}
					MediaType text = operation.getRequestBody().getContent().get("text/plain");
					if (text != null && text.getExample() != null) {
						files.put("request/" + fullPath + "/request-body.txt", exampleText(text.getExample()));
					}
				}
			}
		}
		return files;
	}

	/**
	 * Return the HTML id of the given path, following the raml2html scheme (all non alphanumeric characters replaced by _, leading _ removed), so that
	 * existing deep links like /docs/api/#users__userUuid__get keep working.
	 *
	 * @param path
	 * @return
	 */
	public static String uniqueId(String path) {
		return StringUtils.stripStart(path.replaceAll("[^A-Za-z0-9]", "_"), "_");
	}

	private Map<String, Object> topResource(String displayName, String description) {
		Map<String, Object> resource = new HashMap<>();
		resource.put("displayName", displayName);
		resource.put("uniqueId", uniqueId(displayName));
		resource.put("description", StringUtils.trimToEmpty(description));
		resource.put("resources", new ArrayList<Map<String, Object>>());
		return resource;
	}

	/**
	 * Find the resource with the longest path, which contains the given path.
	 */
	private String findResource(String relativePath, Iterable<String> resourcePaths) {
		String found = null;
		for (String resourcePath : resourcePaths) {
			boolean matches = resourcePath.endsWith("/")
				? relativePath.startsWith(resourcePath) || relativePath.equals(StringUtils.removeEnd(resourcePath, "/"))
				: relativePath.equals(resourcePath) || relativePath.startsWith(resourcePath + "/");
			if (matches && (found == null || resourcePath.length() > found.length())) {
				found = resourcePath;
			}
		}
		return found;
	}

	/**
	 * Resource of a path, which does not belong to any known endpoint: first segment, or the first two segments for project paths.
	 */
	private String fallbackResource(String relativePath) {
		String[] segments = StringUtils.split(relativePath, '/');
		if (segments.length == 0) {
			return "/";
		}
		if (segments[0].startsWith("{") && segments.length > 1) {
			return "/" + segments[0] + "/" + segments[1];
		}
		return "/" + segments[0];
	}

	private Map<String, Object> childResource(OpenAPI openApi, String parentUrl, String relativeUri, PathItem pathItem) throws IOException {
		String uniqueId = uniqueId(parentUrl + relativeUri);
		Map<String, Object> resource = new HashMap<>();
		resource.put("parentUrl", parentUrl);
		resource.put("relativeUri", relativeUri);
		resource.put("uniqueId", uniqueId);

		List<Map<String, Object>> methods = new ArrayList<>();
		for (Entry<PathItem.HttpMethod, Operation> entry : pathItem.readOperationsMap().entrySet()) {
			methods.add(method(openApi, uniqueId, parentUrl, relativeUri, entry.getKey().name().toLowerCase(), entry.getValue(), pathItem.getParameters()));
		}
		resource.put("methods", methods);
		return resource;
	}

	private Map<String, Object> method(OpenAPI openApi, String resourceId, String parentUrl, String relativeUri, String methodName, Operation operation,
		List<Parameter> pathParameters) throws IOException {
		Map<String, Object> method = new HashMap<>();
		method.put("method", methodName);
		method.put("id", resourceId + "_" + methodName);
		method.put("parentUrl", parentUrl);
		method.put("relativeUri", relativeUri);
		method.put("description", StringUtils.trimToEmpty(operation.getDescription()));

		List<Parameter> parameters = new ArrayList<>();
		if (pathParameters != null) {
			parameters.addAll(pathParameters);
		}
		if (operation.getParameters() != null) {
			parameters.addAll(operation.getParameters());
		}
		List<Map<String, Object>> uriParameters = items(parameters, "path");
		List<Map<String, Object>> headers = items(parameters, "header");
		List<Map<String, Object>> queryParameters = items(parameters, "query");
		method.put("uriParameters", uriParameters);
		method.put("headers", headers);
		method.put("queryParameters", queryParameters);

		List<Map<String, Object>> body = new ArrayList<>();
		if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
			for (Entry<String, MediaType> content : operation.getRequestBody().getContent().entrySet()) {
				body.add(body(openApi, content.getKey(), content.getValue(), true));
			}
		}
		method.put("body", body);
		method.put("hasRequest", !uriParameters.isEmpty() || !headers.isEmpty() || !queryParameters.isEmpty() || !body.isEmpty());

		List<Map<String, Object>> responses = new ArrayList<>();
		if (operation.getResponses() != null) {
			for (Entry<String, ApiResponse> entry : operation.getResponses().entrySet()) {
				if (!StringUtils.isNumeric(entry.getKey())) {
					continue;
				}
				Map<String, Object> response = new HashMap<>();
				response.put("code", entry.getKey());
				response.put("description", StringUtils.trimToEmpty(entry.getValue().getDescription()));
				List<Map<String, Object>> responseBody = new ArrayList<>();
				if (entry.getValue().getContent() != null) {
					for (Entry<String, MediaType> content : entry.getValue().getContent().entrySet()) {
						responseBody.add(body(openApi, content.getKey(), content.getValue(), false));
					}
				}
				response.put("body", responseBody);
				responses.add(response);
			}
		}
		method.put("responses", responses);
		return method;
	}

	private List<Map<String, Object>> items(List<Parameter> parameters, String in) {
		return parameters.stream()
			.filter(p -> in.equals(p.getIn()))
			.map(p -> item(p.getName(), Boolean.TRUE.equals(p.getRequired()), StringUtils.defaultIfBlank(p.getDescription(), schemaDescription(p.getSchema())),
				p.getSchema(), p.getExample()))
			.collect(Collectors.toList());
	}

	private Map<String, Object> item(String name, boolean required, String description, Schema<?> schema, Object example) {
		Map<String, Object> item = new HashMap<>();
		item.put("name", name);
		item.put("required", required);
		item.put("description", StringUtils.trimToEmpty(description));
		String type = typeName(schema);
		item.put("type", type);
		// Handlebars resolves null values from the parent context, so absent values are stored as empty strings
		for (String key : List.of("enum", "default", "minLength", "maxLength", "minimum", "maximum", "pattern")) {
			item.put(key, "");
		}
		if (schema != null) {
			if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
				item.put("enum", schema.getEnum().stream().map(String::valueOf).collect(Collectors.joining(", ")));
			}
			item.put("default", Objects.toString(schema.getDefault(), ""));
			if ("string".equals(type)) {
				item.put("minLength", Objects.toString(schema.getMinLength(), ""));
				item.put("maxLength", Objects.toString(schema.getMaxLength(), ""));
			} else {
				item.put("minimum", Objects.toString(schema.getMinimum(), ""));
				item.put("maximum", Objects.toString(schema.getMaximum(), ""));
			}
			item.put("pattern", StringUtils.defaultString(schema.getPattern()));
			if (example == null) {
				example = schema.getExample();
			}
		}
		item.put("example", Objects.toString(example, ""));
		item.put("stringType", "string".equals(type));
		return item;
	}

	private Map<String, Object> body(OpenAPI openApi, String mimeType, MediaType mediaType, boolean request) throws IOException {
		Map<String, Object> body = new HashMap<>();
		body.put("type", mimeType);
		body.put("formParameters", List.of());
		body.put("properties", List.of());
		body.put("example", "");
		Schema<?> schema = resolve(openApi, mediaType.getSchema());
		if (request && mimeType.startsWith("multipart/form-data") && schema != null && schema.getProperties() != null) {
			List<Map<String, Object>> formParameters = new ArrayList<>();
			for (Entry<String, Schema> property : schema.getProperties().entrySet()) {
				boolean required = schema.getRequired() != null && schema.getRequired().contains(property.getKey());
				formParameters.add(item(property.getKey(), required, property.getValue().getDescription(), property.getValue(), null));
			}
			body.put("formParameters", formParameters);
		} else if (schema != null && schema.getProperties() != null && !schema.getProperties().isEmpty()) {
			List<Map<String, Object>> properties = new ArrayList<>();
			for (Entry<String, Schema> property : schema.getProperties().entrySet()) {
				Map<String, Object> row = new HashMap<>();
				row.put("key", property.getKey());
				row.put("required", schema.getRequired() != null && schema.getRequired().contains(property.getKey()));
				row.put("type", typeName(property.getValue()));
				row.put("description", StringUtils.defaultString(property.getValue().getDescription()));
				properties.add(row);
			}
			properties.sort(Comparator.comparing(row -> (String) row.get("key")));
			body.put("properties", properties);
		}
		if (mediaType.getExample() != null) {
			body.put("example", exampleText(mediaType.getExample()));
		}
		return body;
	}

	private Schema<?> resolve(OpenAPI openApi, Schema<?> schema) {
		if (schema != null && schema.get$ref() != null && schema.get$ref().startsWith(SCHEMA_REF_PREFIX) && openApi.getComponents() != null
			&& openApi.getComponents().getSchemas() != null) {
			return openApi.getComponents().getSchemas().get(schema.get$ref().substring(SCHEMA_REF_PREFIX.length()));
		}
		return schema;
	}

	private String typeName(Schema<?> schema) {
		if (schema == null) {
			return "string";
		}
		if (schema.get$ref() != null) {
			return "object";
		}
		if ("string".equals(schema.getType()) && "binary".equals(schema.getFormat())) {
			return "file";
		}
		return StringUtils.defaultIfBlank(schema.getType(), "object");
	}

	private String schemaDescription(Schema<?> schema) {
		return schema == null ? null : schema.getDescription();
	}

	private String exampleText(Object example) throws IOException {
		if (example instanceof String) {
			return (String) example;
		}
		return EXAMPLE_MAPPER.writeValueAsString(example);
	}

	/**
	 * Collects the top level resources (base path of each endpoint class) and their descriptions from the mocked endpoints.
	 */
	public static class EndpointResourceCollector extends AbstractEndpointGenerator<Map<String, String>> {

		/**
		 * Collect the resources in the order the endpoints are registered.
		 *
		 * @return Map of resource path to description
		 * @throws IOException
		 */
		public Map<String, String> collect() throws IOException {
			Map<String, String> resources = new LinkedHashMap<>();
			addCoreEndpoints(resources);
			addProjectEndpoints(resources);
			addExtraEndpoints(resources);
			return resources;
		}

		@Override
		protected void addEndpoints(String basePath, Map<String, String> resources, AbstractInternalEndpoint verticle, boolean isProject) throws IOException {
			resources.putIfAbsent(basePath + "/" + verticle.getBasePath(), verticle.getDescription());
		}
	}
}
