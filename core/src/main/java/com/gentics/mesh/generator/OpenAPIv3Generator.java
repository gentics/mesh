package com.gentics.mesh.generator;

import static com.gentics.mesh.MeshVersion.CURRENT_API_VERSION;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON_UTF8;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_YAML_UTF8;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.keyvalue.UnmodifiableMapEntry;
import org.apache.commons.lang.StringUtils;
import org.raml.model.MimeType;
import org.raml.model.parameter.AbstractParam;
import org.reflections.Reflections;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;
import com.hazelcast.core.HazelcastInstance;

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
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * OpenAPI v3 API definition generator. Outputs JSON and YAML schemas.
 * 
 * @author plyhun
 *
 */
public class OpenAPIv3Generator extends AbstractEndpointGenerator<OpenAPI> {

	private static final Logger log = LoggerFactory.getLogger(OpenAPIv3Generator.class);

	private final HazelcastInstance hz;

	public OpenAPIv3Generator(HazelcastInstance hz) {
		this.hz = hz;
	}

	public OpenAPIv3Generator(HazelcastInstance hz, File outputFolder, boolean cleanup) throws IOException {
		super(outputFolder, cleanup);
		this.hz = hz;
	}

	public OpenAPIv3Generator(HazelcastInstance hz, File outputFolder) throws IOException {
		super(outputFolder);
		this.hz = hz;
	}

	public void generate(InternalActionContext ac, String format) {
		log.info("Starting OpenAPIv3 generation...");
		OpenAPI openApi = new OpenAPI();
		Info info = new Info();
		Server server = new Server();
		info.setTitle("Gentics Mesh REST API");
		info.setVersion(MeshVersion.getBuildInfo().getVersion());
		if (hz != null) {
			hz.getCluster().getMembers().stream().forEach(m -> server.setUrl(m.getAddress().toString()));
		}
		openApi.servers(Collections.singletonList(server));
		openApi.setInfo(info);		
		try {
			addSecurity(openApi);
			addComponents(openApi);
			addCoreEndpoints(openApi);
			addProjectEndpoints(openApi);
		} catch (IOException e) {
			throw new RuntimeException("Could not add all verticles to raml generator", e);
		}

		String formatted;
		String mime;
		switch (format) {
		case "yaml":
			try {
				mime = APPLICATION_YAML_UTF8;
				formatted = Yaml.pretty().writeValueAsString(openApi);
			} catch (JsonProcessingException e) {
				throw new RuntimeException("Could not generate YAML", e);
			}
			break;
		case "json":
			mime = APPLICATION_JSON_UTF8;
			formatted = Json.pretty(openApi);
			break;
		default:
			throw error(BAD_REQUEST, "Please specify a response format: YAML or JSON");
		}
		ac.send(formatted, OK, mime);
	}

	private void addSecurity(OpenAPI openApi) {
		Components components;
		if (openApi.getComponents() == null) {
			components = new Components();
			openApi.setComponents(components);
		} else {
			components = openApi.getComponents();
		}
		SecurityScheme securityBearerAuth = new SecurityScheme();
		securityBearerAuth.setScheme("bearer");
		securityBearerAuth.setType(SecurityScheme.Type.HTTP);
		securityBearerAuth.setBearerFormat("JWT");
		components.addSecuritySchemes("bearerAuth", securityBearerAuth);
		//TODO OAuth2
		SecurityRequirement reqBearerAuth = new SecurityRequirement();
		reqBearerAuth.addList("bearerAuth");
		openApi.addSecurityItem(reqBearerAuth);
	}

	@SuppressWarnings("rawtypes")
	private void addComponents(OpenAPI openApi) {
		Components components;
		if (openApi.getComponents() == null) {
			components = new Components();
			openApi.setComponents(components);
		} else {
			components = openApi.getComponents();
		}
		Reflections reflections = new Reflections("com.gentics.mesh");
		final List<Type> generics = new ArrayList<>();
		reflections.getSubTypesOf(RestModel.class).stream().map(cls -> {
			if (Modifier.isInterface(cls.getModifiers()) || Modifier.isAbstract(cls.getModifiers()) || cls.getTypeParameters().length > 0) {
				return null;
			}
			Schema<?> schema = new Schema<String>();
			List<Stream<Field>> fieldStreams = new ArrayList<>();
			Class<?> tclass = cls;
			log.debug("Class: " + tclass.getCanonicalName());
			generics.clear();
			generics.addAll(Arrays.asList(ParameterizedType.class.isInstance(cls) ? ParameterizedType.class.cast(cls).getActualTypeArguments() : new Type[0]));
			while (tclass != null) {
				fieldStreams.add(Arrays.stream(tclass.getDeclaredFields()));
				generics.addAll(Arrays.asList(ParameterizedType.class.isInstance(tclass.getGenericSuperclass()) ? ParameterizedType.class.cast(tclass.getGenericSuperclass()).getActualTypeArguments() : new Type[0]));
				tclass = tclass.getSuperclass();
			}
			if (generics.size() > 0) {
				log.debug(" - Generics: " + Arrays.toString(generics.toArray()));
			}
			Map<String, Schema> properties = fieldStreams.stream().flatMap(Function.identity()).map(f -> {
				String name = f.getName();
				log.debug(" - Field: " + f);
				Schema<?> fieldSchema = new Schema<String>();
				JsonPropertyDescription description = f.getAnnotation(JsonPropertyDescription.class);
				if (description != null) {
					fieldSchema.setDescription(description.value());
				}
				JsonProperty property = f.getAnnotation(JsonProperty.class);
				if (property != null) {
					if (StringUtils.isNotBlank(property.defaultValue())) {
						fieldSchema.setDefault(property.defaultValue());
					}
					if (property.required()) {
						schema.addRequiredItem(name);
					}
				}
				Class<?> t = f.getType();
				JsonDeserialize jdes = f.getAnnotation(JsonDeserialize.class);
				if (jdes != null && jdes.as() != null) {
					t = jdes.as();
					fieldSchema.setType("object");
					fieldSchema.set$ref("#/components/schemas/" + t.getSimpleName());
				} else {
					generics.addAll(Arrays.asList(ParameterizedType.class.isInstance(f.getGenericType()) ? ParameterizedType.class.cast(f.getGenericType()).getActualTypeArguments() : new Type[0]));
					if (generics.size() > 0) {
						log.debug(" - Generics: " + Arrays.toString(generics.toArray()));
					}
					fillType(components, t, fieldSchema, generics);
				}
				return new UnmodifiableMapEntry<>(name, fieldSchema);
			}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			schema.setProperties(properties);
			return new UnmodifiableMapEntry<>(cls.getSimpleName(), schema);
		}).filter(Objects::nonNull).forEach(e -> components.addSchemas(e.getKey(), e.getValue()));
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
			String fullPath = "api/v" + CURRENT_API_VERSION + basePath + "/" + verticle.getBasePath()
					+ endpoint.getRamlPath();
			if (isEmpty(verticle.getBasePath())) {
				fullPath = "api/v" + CURRENT_API_VERSION + basePath + endpoint.getRamlPath();
			}
			PathItem pathItem = paths.get(fullPath);
			if (pathItem == null) {
				log.debug("Path " + fullPath);
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
			if (endpoint.isInsecure()) {
				// Reset the default security requirements
				operation.setSecurity(Collections.emptyList());
			}
			switch (method.name()) {
			case "DELETE":
				pathItem.setDelete(operation);
				break;
			case "GET":
				pathItem.setGet(operation);
				break;
			case "HEAD":
				pathItem.setHead(operation);
				break;
			case "OPTIONS":
				pathItem.setOptions(operation);
				break;
			case "PATCH":
				pathItem.setPatch(operation);
				break;
			case "POST":
				pathItem.setPost(operation);
				break;
			case "PUT":
				pathItem.setPut(operation);
				break;
			case "TRACE":
				pathItem.setTrace(operation);
				break;
			default:
				break;
			}
			List<Stream<Parameter>> params = List.of(
					endpoint.getQueryParameters().entrySet().stream().map(e -> parameter(e.getKey(), e.getValue(), InParameter.QUERY)),
					endpoint.getUriParameters().entrySet().stream().map(e -> parameter(e.getKey(), e.getValue(), InParameter.PATH)));
			operation.setParameters(params.stream().flatMap(Function.identity()).filter(Objects::nonNull).collect(Collectors.toList()));
			if (endpoint.getExampleRequestMap() != null) {
				RequestBody requestBody = new RequestBody();
				Content content = new Content();
				endpoint.getExampleRequestMap().entrySet().stream().filter(e -> Objects.nonNull(e.getValue()))
						.map(e -> fillMediaType(e.getKey(), e.getValue(), endpoint.getExampleRequestClass()))
						.filter(Objects::nonNull).forEach(e -> content.addMediaType(e.getKey(), e.getValue()));
				requestBody.setContent(content);
				operation.setRequestBody(requestBody);
				ApiResponses responses = new ApiResponses();
				endpoint.getProduces().stream().map(e -> {
					ApiResponse response = new ApiResponse();
					Content responseBody = new Content();
					responseBody.addMediaType(e, new MediaType());
					response.setContent(responseBody);
					return new UnmodifiableMapEntry<String, ApiResponse>("default", response);
				}).filter(Objects::nonNull).forEach(e -> responses.addApiResponse(e.getKey(), e.getValue()));
				endpoint.getExampleResponses().entrySet().stream().filter(e -> Objects.nonNull(e.getValue()))
						.map(e -> {
							ApiResponse response = new ApiResponse();
							if (e.getValue().getDescription().startsWith("Generated login token")) {
								e.getValue().getHeaders();
							}
							response.setDescription(e.getValue().getDescription());
							if (endpoint.getExampleResponseClasses() != null && endpoint.getExampleResponseClasses().get(e.getKey()) != null) {
								Class<?> ref = endpoint.getExampleResponseClasses().get(e.getKey());
								if (ref.getCanonicalName().startsWith("com.gentics.mesh")) {
									response.set$ref("#/components/schemas/" + ref.getSimpleName());
								} else {
									Content responseBody = new Content();
								//	response.setContent(responseBody);
									e.getValue().getBody().entrySet().stream().filter(r -> Objects.nonNull(r.getValue()))
										.map(r -> fillMediaType(r.getKey(), r.getValue(), ref))
										.filter(Objects::nonNull).forEach(r -> responseBody.addMediaType(r.getKey(), r.getValue()));
									response.setContent(responseBody);
								}
							}							
							return new UnmodifiableMapEntry<Integer, ApiResponse>(e.getKey(), response);
						}).filter(Objects::nonNull).forEach(e -> responses.addApiResponse(Integer.toString(e.getKey()), e.getValue()));
				operation.setResponses(responses);
			}
			// action.setIs(Arrays.asList(endpoint.getTraits()));
		}
	}

	private Map.Entry<String, MediaType> fillMediaType(String key, MimeType mimeType, Class<?> refClass) {
		MediaType mediaType = new MediaType();
		mediaType.setExample(mimeType.getExample());
		if (mimeType.getFormParameters() != null) {
			Map<String, Schema> props = mimeType.getFormParameters().entrySet().stream().map(p -> parameter(p.getKey(), p.getValue().get(0), null))
					.collect(Collectors.toMap(p -> p.getName(), p -> p.getSchema()));
			Schema<String> schema = new Schema<>();
			schema.setType("object");
			schema.setProperties(props);
			mediaType.setSchema(schema);
			return new UnmodifiableMapEntry<String, MediaType>("multipart/form-data", mediaType);
		} else if (mimeType.getSchema() != null) {
			JsonObject jschema = new JsonObject(mimeType.getSchema());
			Schema<String> schema = new Schema<>();
			schema.setType(jschema.getString("type", "string"));
			schema.set$id(jschema.getString("id"));
			schema.set$ref("#/components/schemas/" + refClass.getSimpleName());
			mediaType.setSchema(schema);
			return new UnmodifiableMapEntry<String, MediaType>(key, mediaType);
		} else if (refClass != null && refClass.getSimpleName().toLowerCase().startsWith("json")) {
			mediaType.setExample(mimeType.getExample());
			return new UnmodifiableMapEntry<String, MediaType>(key, mediaType);
		} else { return null; }	
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void fillType(Components components, Class<?> t, Schema fieldSchema, List<Type> generics) {
		if (t.isPrimitive() || Number.class.isAssignableFrom(t) || Boolean.class.isAssignableFrom(t)) {
			if (int.class.isAssignableFrom(t) || Integer.class.isAssignableFrom(t)) {
				fieldSchema.setType("integer");
				fieldSchema.setFormat("int32");
			} else if (boolean.class.isAssignableFrom(t) || Boolean.class.isAssignableFrom(t)) {
				fieldSchema.setType("boolean");
			} else if (float.class.isAssignableFrom(t) || Float.class.isAssignableFrom(t)) {
				fieldSchema.setType("number");
				fieldSchema.setFormat("float");
			} else if (long.class.isAssignableFrom(t) || Long.class.isAssignableFrom(t)) {
				fieldSchema.setType("integer");
				fieldSchema.setFormat("int64");
			} else if (double.class.isAssignableFrom(t) || Double.class.isAssignableFrom(t)) {
				fieldSchema.setType("number");
				fieldSchema.setFormat("double");
			} else if (BigDecimal.class.isAssignableFrom(t) || Number.class.isAssignableFrom(t)) {
				fieldSchema.setType("number");
			} else {
				fieldSchema.setType("object");
			}
		} else if (CharSequence.class.isAssignableFrom(t)) {
			fieldSchema.setType("string");
		} else if (t.isArray() || List.class.isAssignableFrom(t)) {
			fieldSchema.setType("array");
			Schema<?> itemSchema = new Schema<String>();
			if (t.isArray()) {
				fillType(components, Array.newInstance(t, 0).getClass(), itemSchema, generics);
			} else if (generics.size() > 0 && Class.class.isInstance(generics.get(0))) {
				Class<?> itemClass = Class.class.cast(generics.get(0));
				fillType(components, itemClass, itemSchema, null);
			} else {
				// TODO
				log.error("Unknown array type" + t + " / " + Arrays.toString(generics.toArray()));
			}
			fieldSchema.setItems(itemSchema);
//		} else if (t.isEnum()) {
//			fieldSchema.setType("string");
//			fieldSchema.setEnum(Arrays.stream(t.getEnumConstants()).collect(Collectors.toList()));
		} else {
			fieldSchema.setType("object");
			fieldSchema.set$ref("#/components/schemas/" + t.getSimpleName());
			if (t.isEnum()) {
				Schema enumSchema = new Schema<String>();
				enumSchema.setType("string");
				enumSchema.setEnum(Arrays.stream(t.getEnumConstants()).collect(Collectors.toList()));
				components.addSchemas(t.getSimpleName(), enumSchema);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private final Parameter parameter(String name, AbstractParam param, InParameter inType) {
		Schema schema;
		switch (param.getType()) {
		case BOOLEAN:
			schema = new Schema<Boolean>();
			schema.setType("boolean");
			break;
		case DATE:
			schema = new Schema<Long>();
			schema.setType("integer");
			schema.setFormat("int64");
			break;
		case FILE:
			schema = new Schema<File>();
			schema.setType("string");
			schema.setFormat("binary");
			break;
		case INTEGER:
			schema = new Schema<Integer>();
			schema.setType("integer");
			schema.setFormat("int32");
			break;
		case NUMBER:
			schema = new Schema<Double>();
			schema.setType("number");
			schema.setFormat("double");
			break;
		case STRING:
			schema = new Schema<String>();
			schema.setType("string");
			break;
		default:
			schema = new Schema<Object>();
			schema.setType("object");
			break;
		}
		schema.setMinimum(param.getMinimum());
		schema.setMaximum(param.getMaximum());
		schema.setMinLength(param.getMinLength());
		schema.setMaxLength(param.getMaxLength());
		schema.setDefault(param.getDefaultValue());
		schema.setEnum(param.getEnumeration());
		schema.setPattern(param.getPattern());
		schema.setDescription(param.getDescription());
		if (StringUtils.isNotBlank(param.getExample())) {
			schema.setExample(param.getExample());
		}
		Parameter p = new Parameter();
		p.setRequired(param.isRequired());
		p.setDescription(param.getDescription());
		p.setSchema(schema);
		p.setName(name);
		if (inType != null) {
			p.setIn(inType.value);
		}
		return p;
	}

	private enum InParameter {
		PATH("path"), QUERY("query"), HEADER("header"), COOKIE("cookie");

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
