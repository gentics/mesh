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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.keyvalue.UnmodifiableMapEntry;
import org.apache.commons.lang.StringUtils;
import org.raml.model.MimeType;
import org.raml.model.parameter.AbstractParam;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.etc.config.HttpServerConfig;
import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;
import com.hazelcast.core.HazelcastInstance;

import io.swagger.v3.core.util.Json31;
import io.swagger.v3.core.util.Yaml31;
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

/**
 * OpenAPI v3 API definition generator. Outputs JSON and YAML schemas.
 * 
 * @author plyhun
 *
 */
public class OpenAPIv3Generator extends AbstractEndpointGenerator<OpenAPI> {

	private static final String API_VERSION_PATH_PREFIX = "/api/v";

	private static final Logger log = LoggerFactory.getLogger(OpenAPIv3Generator.class);

	private final HazelcastInstance hz;
	private final HttpServerConfig httpServerConfig;

	public OpenAPIv3Generator(HazelcastInstance hz, HttpServerConfig httpServerConfig) {
		this.hz = hz;
		this.httpServerConfig = httpServerConfig;
	}

	public OpenAPIv3Generator(HazelcastInstance hz, HttpServerConfig httpServerConfig, File outputFolder, boolean cleanup) throws IOException {
		super(outputFolder, cleanup);
		this.hz = hz;
		this.httpServerConfig = httpServerConfig;
	}

	public OpenAPIv3Generator(HazelcastInstance hz, HttpServerConfig httpServerConfig, File outputFolder) throws IOException {
		super(outputFolder);
		this.hz = hz;
		this.httpServerConfig = httpServerConfig;
	}

	public void generate(InternalActionContext ac, String format) {
		log.info("Starting OpenAPIv3 generation...");
		OpenAPI openApi = new OpenAPI();
		Info info = new Info();
		info.setTitle("Gentics Mesh REST API");
		info.setVersion(MeshVersion.getBuildInfo().getVersion());
		if (hz != null) {
			List<Server> servers = hz.getCluster().getMembers().stream().map(m -> {
				Server server = new Server();
				server.setUrl(m.getAddress().toString());
				return server;
			}).collect(Collectors.toList());
			openApi.servers(servers);
		} else {
			Server server = new Server();
			server.setUrl((httpServerConfig.isSsl() ? "https://" : "http://") + httpServerConfig.getHost() + ":" + (httpServerConfig.isSsl() ? httpServerConfig.getSslPort() : httpServerConfig.getPort()));
			openApi.servers(Collections.singletonList(null));
		}
		openApi.setInfo(info);		
		try {
			addSecurity(openApi);
			addComponents(openApi);
			addCoreEndpoints(openApi);
			addProjectEndpoints(openApi);
		} catch (IOException e) {
			throw new RuntimeException("Could not add all verticles to raml generator", e);
		}

		//new OpenAPI30To31().process(openApi);
		//openApi.jsonSchemaDialect("https://spec.openapis.org/oas/3.1/dialect/base");
		String formatted;
		String mime;
		switch (format) {
		case "yaml":
			try {
				mime = APPLICATION_YAML_UTF8;
				formatted = ac.isMinify(httpServerConfig) ? Yaml31.mapper().writer().writeValueAsString(openApi) : Yaml31.pretty().writeValueAsString(openApi);
			} catch (JsonProcessingException e) {
				throw new RuntimeException("Could not generate YAML", e);
			}
			break;
		case "json":
			mime = APPLICATION_JSON_UTF8;
			try {
				formatted = ac.isMinify(httpServerConfig) ? Json31.mapper().writer().writeValueAsString(openApi) : Json31.pretty(openApi);
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
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

	private String makeSchemaName(Class<?> cls) {
		return "Mesh" + cls.getSimpleName();
	}

	private void addComponents(OpenAPI openApi) {
		Components components;
		if (openApi.getComponents() == null) {
			components = new Components();
			openApi.setComponents(components);
		} else {
			components = openApi.getComponents();
		}
		components.setSchemas(new HashMap<>(Map.of("AnyJson", new Schema<String>())));
		Reflections reflections = new Reflections("com.gentics.mesh");
		reflections.getSubTypesOf(RestModel.class).stream().forEach(cls -> fillComponent(cls, components));
	}

	@SuppressWarnings("rawtypes")
	private void fillComponent(Class<?> cls, Components components) {
		if (!cls.getPackageName().startsWith("com.gentics.mesh") || StringUtils.isBlank(cls.getSimpleName())) {
			return;
		}
		Schema<?> schema = components.getSchemas().getOrDefault(cls.getSimpleName(), new Schema<String>());
		schema.setName(cls.getSimpleName());
		List<Stream<Field>> fieldStreams = new ArrayList<>();
		final List<Type> generics = new ArrayList<>();
		generics.addAll(Arrays.asList(ParameterizedType.class.isInstance(cls) ? ParameterizedType.class.cast(cls).getActualTypeArguments() : new Type[0]));
		Deque<Class<?>> dq = new ArrayDeque<>(2);
		dq.addLast(cls);
		while(!dq.isEmpty()) {
			Class<?> tclass = dq.pop();
			log.debug("Class: " + tclass.getCanonicalName());
			/*if (tclass != cls) {
				Schema<?> tschema = components.getSchemas().computeIfAbsent(tclass.getSimpleName(), key -> new Schema<String>());
				tschema.setName(tclass.getSimpleName());
				Schema<?> refSchema = new Schema<>();
				refSchema.$ref("#/components/schemas/" + tclass.getSimpleName());
				schema.addAllOfItem(refSchema);
				refSchema = new Schema<>();
				refSchema.$ref("#/components/schemas/" + cls.getSimpleName());
				tschema.addAnyOfItem(refSchema);
			}*/
			fieldStreams.add(Arrays.stream(tclass.getDeclaredFields()));
			generics.addAll(Arrays.asList(ParameterizedType.class.isInstance(tclass.getGenericSuperclass()) ? ParameterizedType.class.cast(tclass.getGenericSuperclass()).getActualTypeArguments() : new Type[0]));
			dq.addAll(Arrays.stream(tclass.getInterfaces()).filter(i -> i.getPackageName().startsWith("com.gentics.mesh")).collect(Collectors.toList()));
			tclass = tclass.getSuperclass();
			if (tclass != null) {
				dq.addLast(tclass);
			}
		}
		if (generics.size() > 0) {
			log.debug(" - Generics: " + Arrays.toString(generics.toArray()));
		}
		Map<String, Schema> properties = fieldStreams.stream().flatMap(Function.identity())
			.filter(f -> !Modifier.isStatic(f.getModifiers())).peek(f -> {
				Class<?> t = f.getType();
				if (!RestModel.class.isAssignableFrom(t) && !t.isPrimitive() && !t.getCanonicalName().startsWith("java.lang")) {
					fillComponent(t, components);
				}
			})
			.map(f -> {
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
					if (StringUtils.isNotBlank(property.value())) {
						name = property.value();
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
		components.addSchemas(cls.getSimpleName(), schema);
	}

	@Override
	protected void addEndpoints(String basePath, OpenAPI consumer, AbstractInternalEndpoint verticle, boolean isProject)
			throws IOException {
		// Check whether the resource was already added. Maybe we just need to extend it
		Paths paths = consumer.getPaths();
		if (paths == null) {
			paths = new Paths();
			consumer.setPaths(paths);
		}
		for (InternalEndpointRoute endpoint : verticle.getEndpoints().stream().sorted().collect(Collectors.toList())) {
			if ("eventbus".equals(verticle.getBasePath())) {
				continue;
			}
			String fullPath = API_VERSION_PATH_PREFIX + CURRENT_API_VERSION + basePath + "/" + verticle.getBasePath()
					+ endpoint.getRamlPath();
			if (isEmpty(verticle.getBasePath())) {
				fullPath = API_VERSION_PATH_PREFIX + CURRENT_API_VERSION + basePath + endpoint.getRamlPath();
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
				operation.setSecurity(Collections.emptyList());
			} else {
				SecurityRequirement reqBearerAuth = new SecurityRequirement();
				reqBearerAuth.addList("bearerAuth");
				operation.setSecurity(List.of(reqBearerAuth));
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
					isProject ? Stream.of(new Parameter().name("project").in(InParameter.PATH.value).schema(new Schema<String>().type("string").description("Uuid of the related project"))) : Stream.of(),
					endpoint.getQueryParameters().entrySet().stream().map(e -> parameter(e.getKey(), e.getValue(), InParameter.QUERY)),
					endpoint.getUriParameters().entrySet().stream().map(e -> parameter(e.getKey(), e.getValue(), InParameter.PATH)));
			operation.setParameters(params.stream().flatMap(Function.identity()).filter(Objects::nonNull).collect(Collectors.toList()));
			ApiResponses responses = new ApiResponses();
			endpoint.getProduces().stream().map(e -> {
				ApiResponse response = new ApiResponse();
				Content responseBody = new Content();
				responseBody.addMediaType(e, new MediaType());
				response.setContent(responseBody);
				response.setDescription(e);
				if (HttpConstants.APPLICATION_OCTET_STREAM.equals(e)) {
					response.setExtensions(Map.of("x-is-file", true));
					Schema<String> schema = new Schema<>();
					schema.setType("string");
					schema.setFormat("binary");
					MediaType mediaType = new MediaType();
					mediaType.setSchema(schema);
					responseBody.addMediaType("application/octet-stream", mediaType);
					response.setContent(responseBody);
				}
				return new UnmodifiableMapEntry<String, ApiResponse>("default", response);
			}).filter(Objects::nonNull).forEach(e -> responses.addApiResponse(e.getKey(), e.getValue()));
			endpoint.getExampleResponses().entrySet().stream().filter(e -> Objects.nonNull(e.getValue()))
				.map(e -> {
					ApiResponse response = new ApiResponse();
					if (e.getValue().getDescription().startsWith("Generated login token")) {
						e.getValue().getHeaders();
					}
					response.setDescription(e.getValue().getDescription());
					Content responseBody = new Content();
					if (endpoint.getExampleResponseClasses() != null && endpoint.getExampleResponseClasses().get(e.getKey()) != null) {
						Class<?> ref = endpoint.getExampleResponseClasses().get(e.getKey());
						if (ref.getCanonicalName().startsWith("com.gentics.mesh")) {
							Schema<String> schema = new Schema<>();
							schema.set$ref("#/components/schemas/" + ref.getSimpleName());
							MediaType mediaType = new MediaType();
							mediaType.setSchema(schema);
							mediaType.setExample(e.getValue());
							responseBody.addMediaType("*/*", mediaType);
							response.setContent(responseBody);
						} else {
							if (e.getValue().getBody() != null) {
								e.getValue().getBody().entrySet().stream().filter(r -> Objects.nonNull(r.getValue()))
									.map(r -> fillMediaType(r.getKey(), r.getValue(), ref))
									.filter(Objects::nonNull).forEach(r -> responseBody.addMediaType(r.getKey(), r.getValue()));
							} else {
								log.warn("Body of " + e.getKey() + " is null!");
							}
							response.setContent(responseBody);
						}
					}							
					return new UnmodifiableMapEntry<Integer, ApiResponse>(e.getKey(), response);
				}).filter(Objects::nonNull).forEach(e -> responses.addApiResponse(Integer.toString(e.getKey()), e.getValue()));
			operation.setResponses(responses);
			if (endpoint.getExampleRequestMap() != null && !HttpMethod.DELETE.equals(method)) {
				RequestBody requestBody = new RequestBody();
				Content content = new Content();
				endpoint.getExampleRequestMap().entrySet().stream().filter(e -> Objects.nonNull(e.getValue()))
						.map(e -> fillMediaType(e.getKey(), e.getValue(), endpoint.getExampleRequestClass()))
						.filter(Objects::nonNull).forEach(e -> content.addMediaType(e.getKey(), e.getValue()));
				requestBody.setContent(content);
				operation.setRequestBody(requestBody);
			}
			// action.setIs(Arrays.asList(endpoint.getTraits()));
		}
	}

	@SuppressWarnings("rawtypes")
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
			Schema<String> schema = new Schema<>();
			schema.setType("object");
			mediaType.setSchema(schema);
			return new UnmodifiableMapEntry<String, MediaType>(key, mediaType);
		} else { 
			return new UnmodifiableMapEntry<String, MediaType>(key, mediaType);
		}	
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
				List<Type> generics1 = Arrays.asList(ParameterizedType.class.isInstance(t) ? ParameterizedType.class.cast(t).getActualTypeArguments() : new Type[0]);
				fillType(components, t.getComponentType(), itemSchema, generics1);
			} else if (generics.size() > 0 && Class.class.isInstance(generics.get(0))) {
				Class<?> itemClass = Class.class.cast(generics.get(0));
				List<Type> generics1 = Arrays.asList(ParameterizedType.class.isInstance(t) ? ParameterizedType.class.cast(t).getActualTypeArguments() : new Type[0]);
				fillType(components, itemClass, itemSchema, generics1);
			} else {
				// TODO
				log.error("Unknown array type" + t + " / " + Arrays.toString(generics.toArray()));
			}
			fieldSchema.setItems(itemSchema);
//		} else if (t.isEnum()) {
//			fieldSchema.setType("string");
//			fieldSchema.setEnum(Arrays.stream(t.getEnumConstants()).collect(Collectors.toList()));
		} else {
			if (t.isEnum()) {
				Schema enumSchema = new Schema<String>();
				enumSchema.setType("string");
				enumSchema.setEnum(Arrays.stream(t.getEnumConstants()).collect(Collectors.toList()));
				components.addSchemas(t.getSimpleName(), enumSchema);
			}
			if (Map.class.isAssignableFrom(t)) {
				if (generics.size() == 2) {
					BiConsumer<Type, Schema> innerTypeMapper = (ty, tfieldSchema) -> {
						Class<?> tt = Class.class.isInstance(ty) ? Class.class.cast(ty) : generics.get(1).getClass();
						if (tt.isPrimitive() || Number.class.isAssignableFrom(tt) || Boolean.class.isAssignableFrom(tt)) {
							if (int.class.isAssignableFrom(tt) || Integer.class.isAssignableFrom(tt)) {
								tfieldSchema.setType("integer");
								tfieldSchema.setFormat("int32");
							} else if (boolean.class.isAssignableFrom(tt) || Boolean.class.isAssignableFrom(tt)) {
								tfieldSchema.setType("boolean");
							} else if (float.class.isAssignableFrom(tt) || Float.class.isAssignableFrom(tt)) {
								tfieldSchema.setType("number");
								tfieldSchema.setFormat("float");
							} else if (long.class.isAssignableFrom(tt) || Long.class.isAssignableFrom(tt)) {
								tfieldSchema.setType("integer");
								tfieldSchema.setFormat("int64");
							} else if (double.class.isAssignableFrom(tt) || Double.class.isAssignableFrom(tt)) {
								tfieldSchema.setType("number");
								tfieldSchema.setFormat("double");
							} else if (BigDecimal.class.isAssignableFrom(tt) || Number.class.isAssignableFrom(tt)) {
								tfieldSchema.setType("number");
							} else {
								tfieldSchema.setType("object");
							}
						} else if (CharSequence.class.isAssignableFrom(tt)) {
							tfieldSchema.setType("string");
						} else {
							tfieldSchema.setType("object");
						}
					};
					fieldSchema.setType("object"); // TODO why object?
					//innerTypeMapper.accept(generics.get(0).getClass(), fieldSchema);
					Schema<?> valueSchema = new Schema<>();
					innerTypeMapper.accept(generics.get(1), valueSchema);
					fieldSchema.setAdditionalProperties(valueSchema);
				} else {
					fieldSchema.setType("object");
					fieldSchema.setAdditionalProperties(new Schema<String>().type("object"));
				}
			} else if (JsonObject.class.isAssignableFrom(t) || JsonSerializable.class.isAssignableFrom(t)) {
				fieldSchema.set$ref("#/components/schemas/AnyJson");
			} else {
				fieldSchema.setType("object");
				fieldSchema.set$ref("#/components/schemas/" + t.getSimpleName());
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
