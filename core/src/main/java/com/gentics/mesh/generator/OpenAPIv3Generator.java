package com.gentics.mesh.generator;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

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
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.collections4.keyvalue.UnmodifiableMapEntry;
import org.apache.commons.lang.StringUtils;
import org.raml.model.MimeType;
import org.raml.model.parameter.AbstractParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.rest.InternalEndpointRoute;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.core.util.OpenAPI30To31;
import io.swagger.v3.core.util.Yaml;
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
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

/**
 * OpenAPI v3 API definition generator. Outputs JSON and YAML schemas.
 * 
 * @author plyhun
 *
 */
public class OpenAPIv3Generator {

	private static final Logger log = LoggerFactory.getLogger(OpenAPIv3Generator.class);

	private final Optional<? extends Collection<Pattern>> maybePathBlacklist;
	private final Optional<? extends Collection<Pattern>> maybePathWhitelist;

	private final List<String> servers;

	/**
	 * Ctor
	 * 
	 * @param servers a list of available servers; may be empty.
	 * @param maybePathBlacklist optional regex blacklist
	 * @param maybePathWhitelist optional regex whitelist
	 */
	public OpenAPIv3Generator(List<String> servers, @Nonnull Optional<? extends Collection<Pattern>> maybePathBlacklist, 
			@Nonnull Optional<? extends Collection<Pattern>> maybePathWhitelist) {
		this.maybePathBlacklist = maybePathBlacklist;
		this.maybePathWhitelist = maybePathWhitelist;
		this.servers = servers;
	}

	/**
	 * Generate the spec out of the given routes and format
	 * 
	 * @param routers
	 * @param format
	 * @param pretty
	 * @param maybePathItemTransformer an optional custon path and path item transformer
	 * @return
	 */
	public String generate(Map<Router, String> routers, Format format, boolean pretty, 
			@Nonnull Optional<BiFunction<String, PathItem, String>> maybePathItemTransformer,
			@Nonnull Optional<Supplier<Collection<Class<?>>>> maybeExtraComponentSupplier) {
		return generate(routers, format, pretty, false, maybePathItemTransformer, maybeExtraComponentSupplier);
	}

	/**
	 * Generate the spec out of given routes and parameters
	 * 
	 * @param routers
	 * @param format
	 * @param pretty
	 * @param useVersion31
	 * @param maybePathItemTransformer an optional custon path and path item transformer
	 * @return
	 */
	public String generate(Map<Router, String> routers, Format format, boolean pretty, boolean useVersion31, 
			@Nonnull Optional<BiFunction<String, PathItem, String>> maybePathItemTransformer,
			@Nonnull Optional<Supplier<Collection<Class<?>>>> maybeExtraComponentSupplier) {
		log.info("Starting OpenAPIv3 generation...");
		OpenAPI openApi = new OpenAPI();
		openApi.setPaths(new Paths());
		Info info = new Info();
		info.setTitle("Gentics Mesh REST API");
		info.setVersion(MeshVersion.getBuildInfo().getVersion());

		openApi.servers(servers.stream().map(url -> {
			Server server = new Server();
			server.setUrl(url);
			return server;
		}).collect(Collectors.toList()));

		openApi.setInfo(info);		
		try {
			addSecurity(openApi);
			maybeExtraComponentSupplier.ifPresent(componentSupplier -> {
				componentSupplier.get().forEach(componentClass -> fillComponent(componentClass, openApi));
			});
			for (Entry<Router, String> routerAndParent : routers.entrySet()) {
				addRouter(routerAndParent.getValue(), routerAndParent.getKey(), openApi, maybePathItemTransformer);
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not add all verticles to raml generator", e);
		}

		OpenAPIVersionWriter writer = useVersion31 ? new V31Writer() : new V30Writer();
		return writer.write(openApi, format, pretty);
	}

	/**
	 * Add a security to the spec
	 * 
	 * @param openApi
	 */
	protected void addSecurity(OpenAPI openApi) {
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
	protected void fillComponent(Class<?> cls, OpenAPI openApi) {
		if (StringUtils.isBlank(cls.getSimpleName())) {
			return;
		}
		Components components;
		if (openApi.getComponents() == null) {
			components = new Components();			
			openApi.setComponents(components);
		} else {
			components = openApi.getComponents();
		}
		if (components.getSchemas() == null) {
			components.setSchemas(new HashMap<>(Map.of("AnyJson", new Schema<String>())));
		}
		Schema<?> schema = components.getSchemas().getOrDefault(cls.getSimpleName(), new Schema<String>());
		schema.setType("object");
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
					fillComponent(t, openApi);
				}
			})
			.map(f -> {
				String name = f.getName();
				log.debug(" - Field: " + f);
				Schema<?> fieldSchema = new Schema<String>();
				fieldSchema.setName(name);
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
				fieldSchema.setTypes(Collections.singleton(fieldSchema.getType()));
				return new UnmodifiableMapEntry<>(name, fieldSchema);
			}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		schema.setProperties(properties);
		components.addSchemas(cls.getSimpleName(), schema);
	}

	protected void resolveEndpointRoute(String path, PathItem pathItem, InternalEndpointRoute endpoint) {
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
		resolveMethod(method.name(), pathItem, operation);
		List<Stream<Parameter>> params = List.of(
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

	protected void resolveMethod(String methodName, PathItem pathItem, Operation operation) {
		switch (methodName.toUpperCase()) {
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
	}

	protected void addRouter(String parent, Router router, OpenAPI consumer, Optional<BiFunction<String, PathItem, String>> maybePathItemTransformer)
			throws IOException {
		Paths paths = consumer.getPaths();

		for (Route route : router.getRoutes()) {
			String rawPath = route.getPath();
			if (StringUtils.isBlank(rawPath)) {
				continue;
			}
			String path = (parent + (StringUtils.equals(rawPath, "/") ? "/" : Arrays.stream(rawPath.split("/"))
						.map(segment -> segment.startsWith(":") ? ("{" + segment.substring(1) + "}") : segment)
						.collect(Collectors.joining("/"))))
					.replace("//", "/");

			if(maybePathBlacklist.flatMap(list -> list.stream().filter(blacklisted -> blacklisted.matcher(path).matches()).findAny()).isPresent()
					|| (maybePathWhitelist.isPresent() && maybePathWhitelist.flatMap(list -> list.stream().filter(whitelisted -> whitelisted.matcher(path).matches()).findAny()).isEmpty())) {
				log.debug("Path filtered off: " + path);
				continue;
			}
			PathItem pathItem = Optional.ofNullable(paths.get(path)).orElseGet(() -> {
				log.debug("Raw path: " + path);
				PathItem item = new PathItem();
				item.setSummary(route.getName());
				paths.put(path, item);
				return item;
			});
			Optional.ofNullable(route.getMetadata(InternalEndpointRoute.class.getCanonicalName()))
				.map(InternalEndpointRoute.class::cast)
				.ifPresentOrElse(endpoint -> {
					log.debug("Path with metadata: " + path);
					pathItem.setSummary(endpoint.getDisplayName());
					pathItem.setDescription(endpoint.getDescription());
					endpoint.getModel().forEach(modelComponent -> fillComponent(modelComponent, consumer));
					resolveEndpointRoute(path, pathItem, endpoint);
				}, () -> {
					resolveFallbackRoute(route, pathItem);
				});
			String path1 = maybePathItemTransformer.map(pathItemTransformer -> {
				String newPath = pathItemTransformer.apply(path, pathItem);
				if (!StringUtils.equals(path, newPath)) {
					paths.remove(path, pathItem);
					paths.put(newPath, pathItem);
				}
				return path;
			}).orElse(path);
			if (pathItem.readOperations().isEmpty()) {
				log.debug("Path removed due to having no operations: " + path1);
				paths.remove(path1, pathItem);
			}
			if (route.getSubRouter() != null) {
				addRouter(path, route.getSubRouter(), consumer, maybePathItemTransformer);
			}
		}
	}

	protected void resolveFallbackRoute(Route r, PathItem pathItem) {
		Operation o = new Operation();
		o.setParameters(Arrays.stream(r.getPath().split("/"))
				.filter(segment -> segment.startsWith(":"))
				.map(segment -> segment.substring(1))
				.map(segment -> new Parameter()
						.name(segment)
						.required(true)
						.allowEmptyValue(false)
						.in(InParameter.PATH.toString())
						.schema(new Schema<String>()
								.type("string")
								.description("A path parameter `" + segment + "` of a fallback type `string`")))
				.collect(Collectors.toList()));
		ApiResponses responses = new ApiResponses();
		ApiResponse response = new ApiResponse();
		Content responseBody = new Content();
		responseBody.addMediaType("*/*", new MediaType());
		response.setDescription("Auto generated response description for " + r.getPath());
		response.setContent(responseBody);
		responses.addApiResponse("200", response);
		o.setResponses(responses);
		Optional.ofNullable(r.methods()).ifPresent(methods -> methods.stream().forEach(m -> resolveMethod(m.name(), pathItem, o)));
	}

	@SuppressWarnings("rawtypes")
	protected Map.Entry<String, MediaType> fillMediaType(String key, MimeType mimeType, Class<?> refClass) {
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
		} else {
			if (t.isEnum()) {
				Schema enumSchema = new Schema<String>();
				enumSchema.setType("string");
				enumSchema.setEnum(Arrays.stream(t.getEnumConstants()).map(e -> e.toString().toLowerCase()).collect(Collectors.toList()));
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

	public enum InParameter {
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

	/**
	 * OpenAPI output format
	 */
	public static enum Format {
		YAML,
		JSON;

		public static final Format parse(String text) {
			if (text == null) {
				throw new IllegalArgumentException("Cannot parse null to OpenAPI Format");
			}
			return Arrays.stream(values())
					.filter(v -> v.name().equals(text.trim().toUpperCase()))
					.findAny()
					.orElseThrow(() -> new IllegalStateException("Unsupported OpenAPI Format:" + text));
		}
	}

	public interface OpenAPIVersionWriter {
		String write(OpenAPI api, Format format, boolean pretty);
	}
	private class V30Writer implements OpenAPIVersionWriter {

		@Override
		public String write(OpenAPI openApi, Format format, boolean pretty) {
			switch (format) {
			case YAML:
				try {
					//formatted = pretty ? Yaml31.mapper().writer().writeValueAsString(openApi) : Yaml31.pretty().writeValueAsString(openApi);
					return pretty ? Yaml.pretty().writeValueAsString(openApi) : Yaml.mapper().writer().writeValueAsString(openApi) ;
				} catch (JsonProcessingException e) {
					throw new RuntimeException("Could not generate YAML", e);
				}
			case JSON:
				try {
					//formatted = pretty ? Json31.mapper().writer().writeValueAsString(openApi) : Json31.pretty(openApi);
					return pretty ? Json.pretty(openApi) : Json.mapper().writer().writeValueAsString(openApi);
				} catch (JsonProcessingException e) {
					throw new RuntimeException(e);
				}
			default:
				throw error(BAD_REQUEST, "Please specify a response format: YAML or JSON");
			}
		}
		
	}
	private class V31Writer implements OpenAPIVersionWriter {

		@Override
		public String write(OpenAPI openApi, Format format, boolean pretty) {
			new OpenAPI30To31().process(openApi);
			openApi.jsonSchemaDialect("https://spec.openapis.org/oas/3.1/dialect/base");

			switch (format) {
			case YAML:
				try {
					return pretty ? Yaml31.mapper().writer().writeValueAsString(openApi) : Yaml31.pretty().writeValueAsString(openApi);
				} catch (JsonProcessingException e) {
					throw new RuntimeException("Could not generate YAML", e);
				}
			case JSON:
				try {
					return pretty ? Json31.mapper().writer().writeValueAsString(openApi) : Json31.pretty(openApi);
				} catch (JsonProcessingException e) {
					throw new RuntimeException(e);
				}
			default:
				throw error(BAD_REQUEST, "Please specify a response format: YAML or JSON");
			}
		}
	}
}
