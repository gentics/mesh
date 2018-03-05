package com.gentics.mesh.rest.impl;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON_UTF8;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jettison.json.JSONObject;
import org.raml.model.MimeType;
import org.raml.model.Response;
import org.raml.model.parameter.FormParameter;
import org.raml.model.parameter.Header;
import org.raml.model.parameter.QueryParameter;
import org.raml.model.parameter.UriParameter;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.InternalEndpointRoute;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * @see InternalEndpointRoute
 */
public class InternalEndpointRouteImpl implements InternalEndpointRoute {

	private static final Logger log = LoggerFactory.getLogger(InternalEndpointRoute.class);

	private Route route;

	private String displayName;

	private String description;

	/**
	 * Uri Parameters which map to the used path segments
	 */
	private Map<String, UriParameter> uriParameters = new HashMap<>();

	/**
	 * Map of example responses for the corresponding status code.
	 */
	private Map<Integer, Response> exampleResponses = new HashMap<>();

	private Map<Integer, Class<?>> exampleResponseClasses = new HashMap<>();

	private String[] traits = new String[] {};

	private HashMap<String, MimeType> exampleRequestMap = null;

	private Class<? extends RestModel> exampleRequestClass = null;

	private String pathRegex;

	private HttpMethod method;

	private String ramlPath;

	private final Set<String> consumes = new LinkedHashSet<>();
	private final Set<String> produces = new LinkedHashSet<>();

	private Map<String, QueryParameter> parameters = new HashMap<>();

	/**
	 * Create a new endpoint wrapper using the provided router to create the wrapped route instance.
	 * 
	 * @param router
	 */
	public InternalEndpointRouteImpl(Router router) {
		this.route = router.route();
	}

	@Override
	public InternalEndpointRoute path(String path) {
		route.path(path);
		return this;
	}

	@Override
	public InternalEndpointRoute method(HttpMethod method) {
		if (this.method != null) {
			throw new RuntimeException(
					"The method for the endpoint was already set. The endpoint wrapper currently does not support more than one method per route.");
		}
		this.method = method;
		route.method(method);
		return this;
	}

	@Override
	public InternalEndpointRoute pathRegex(String path) {
		this.pathRegex = path;
		route.pathRegex(path);
		return this;
	}

	@Override
	public InternalEndpointRoute produces(String contentType) {
		produces.add(contentType);
		route.produces(contentType);
		return this;
	}

	@Override
	public InternalEndpointRoute consumes(String contentType) {
		consumes.add(contentType);
		route.consumes(contentType);
		return this;
	}

	@Override
	public InternalEndpointRoute order(int order) {
		route.order(order);
		return this;
	}

	@Override
	public InternalEndpointRoute last() {
		route.last();
		return this;
	}

	@Override
	public InternalEndpointRoute handler(Handler<RoutingContext> requestHandler) {
		validate();
		route.handler(requestHandler);
		return this;
	}

	@Override
	public InternalEndpointRoute validate() {
		if (!produces.isEmpty() && exampleResponses.isEmpty()) {
			log.error("Endpoint {" + getRamlPath() + "} has no example response.");
			throw new RuntimeException("Endpoint {" + getRamlPath() + "} has no example responses.");
		}
		if ((consumes.contains(APPLICATION_JSON) || consumes.contains(APPLICATION_JSON_UTF8)) && exampleRequestMap == null) {
			log.error("Endpoint {" + getPath() + "} has no example request.");
			throw new RuntimeException("Endpoint has no example request.");
		}
		if (isEmpty(description)) {
			log.error("Endpoint {" + getPath() + "} has no description.");
			throw new RuntimeException("No description was set");
		}

		// Check whether all segments have a description.
		List<String> segments = getNamedSegments();
		for (String segment : segments) {
			if (!getUriParameters().containsKey(segment)) {
				throw new RuntimeException("Missing URI description for path {" + getRamlPath() + "} segment {" + segment + "}");
			}
		}
		return this;
	}

	@Override
	public List<String> getNamedSegments() {
		List<String> allMatches = new ArrayList<String>();
		Matcher m = Pattern.compile("\\{[^}]*\\}").matcher(getRamlPath());
		while (m.find()) {
			allMatches.add(m.group().substring(1, m.group().length() - 1));
		}
		return allMatches;
	}

	@Override
	public InternalEndpointRoute blockingHandler(Handler<RoutingContext> requestHandler) {
		route.blockingHandler(requestHandler);
		return this;
	}

	@Override
	public InternalEndpointRoute blockingHandler(Handler<RoutingContext> requestHandler, boolean ordered) {
		route.blockingHandler(requestHandler, ordered);
		return this;
	}

	@Override
	public InternalEndpointRoute failureHandler(Handler<RoutingContext> failureHandler) {
		route.failureHandler(failureHandler);
		return this;
	}

	@Override
	public InternalEndpointRoute remove() {
		route.remove();
		return this;
	}

	@Override
	public InternalEndpointRoute disable() {
		route.disable();
		return this;
	}

	@Override
	public InternalEndpointRoute enable() {
		route.enable();
		return this;
	}

	@Override
	public InternalEndpointRoute useNormalisedPath(boolean useNormalisedPath) {
		route.useNormalisedPath(useNormalisedPath);
		return this;
	}

	@Override
	public String getPath() {
		return route.getPath();
	}

	@Override
	public String getRamlPath() {
		if (ramlPath == null) {
			return convertPath(route.getPath());
		}
		return ramlPath;
	}

	@Override
	public InternalEndpointRoute displayName(String name) {
		this.displayName = name;
		return this;
	}

	@Override
	public InternalEndpointRoute description(String description) {
		this.description = description;
		return this;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public InternalEndpointRoute exampleResponse(HttpResponseStatus status, String description, String headerName, String example, String headerDescription) {
		Response response = new Response();
		response.setDescription(description);
		exampleResponses.put(status.code(), response);
		if (headerName != null) {
			Header header = new Header();
			header.setDescription(headerDescription);
			header.setExample(example);
			Map<String, Header> headers = new HashMap<>();
			headers.put(headerName, header);
			response.setHeaders(headers);
		}
		return this;
	}

	@Override
	public InternalEndpointRoute exampleResponse(HttpResponseStatus status, String description) {
		return exampleResponse(status, description, null, null, null);
	}

	@Override
	public InternalEndpointRoute exampleResponse(HttpResponseStatus status, Object model, String description) {
		Response response = new Response();
		response.setDescription(description);

		HashMap<String, MimeType> map = new HashMap<>();
		response.setBody(map);

		MimeType mimeType = new MimeType();
		if (model instanceof RestModel) {
			String json = ((RestModel) model).toJson();
			mimeType.setExample(json);
			mimeType.setSchema(JsonUtil.getJsonSchema(model.getClass()));
			map.put("application/json", mimeType);
		} else {
			mimeType.setExample(model.toString());
			map.put("text/plain", mimeType);
		}

		exampleResponses.put(status.code(), response);
		exampleResponseClasses.put(status.code(), model.getClass());
		return this;
	}

	@Override
	public Map<Integer, Class<?>> getExampleResponseClasses() {
		return exampleResponseClasses;
	}

	@Override
	public InternalEndpointRoute exampleRequest(String bodyText) {
		HashMap<String, MimeType> bodyMap = new HashMap<>();
		MimeType mimeType = new MimeType();
		mimeType.setExample(bodyText);
		bodyMap.put("text/plain", mimeType);
		this.exampleRequestMap = bodyMap;
		return this;
	}

	@Override
	public InternalEndpointRoute exampleRequest(Map<String, List<FormParameter>> parameters) {
		HashMap<String, MimeType> bodyMap = new HashMap<>();
		MimeType mimeType = new MimeType();
		mimeType.setFormParameters(parameters);
		bodyMap.put("multipart/form-data", mimeType);
		this.exampleRequestMap = bodyMap;
		return this;
	}

	@Override
	public InternalEndpointRoute exampleRequest(RestModel model) {
		HashMap<String, MimeType> bodyMap = new HashMap<>();
		MimeType mimeType = new MimeType();
		String json = model.toJson();
		mimeType.setExample(json);
		mimeType.setSchema(JsonUtil.getJsonSchema(model.getClass()));
		bodyMap.put("application/json", mimeType);
		this.exampleRequestMap = bodyMap;
		this.exampleRequestClass = model.getClass();
		return this;
	}

	@Override
	public InternalEndpointRoute exampleRequest(JSONObject jsonObject) {
		HashMap<String, MimeType> bodyMap = new HashMap<>();
		MimeType mimeType = new MimeType();
		String json = jsonObject.toString();
		mimeType.setExample(json);
		bodyMap.put("application/json", mimeType);
		this.exampleRequestMap = bodyMap;
		return this;
	}

	@Override
	public InternalEndpointRoute traits(String... traits) {
		this.traits = traits;
		return this;
	}

	@Override
	public String[] getTraits() {
		return traits;
	}

	@Override
	public Map<Integer, Response> getExampleResponses() {
		return exampleResponses;
	}

	@Override
	public HashMap<String, MimeType> getExampleRequestMap() {
		return exampleRequestMap;
	}

	@Override
	public String getPathRegex() {
		return pathRegex;
	}

	@Override
	public HttpMethod getMethod() {
		return method;
	}

	@Override
	public Map<String, QueryParameter> getQueryParameters() {
		return parameters;
	}

	@Override
	public InternalEndpointRoute addQueryParameters(Class<? extends ParameterProvider> clazz) {
		try {
			ParameterProvider provider = clazz.newInstance();
			parameters.putAll(provider.getRAMLParameters());
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return this;
	}

	@Override
	public InternalEndpointRoute setRAMLPath(String path) {
		this.ramlPath = path;
		return this;
	}

	@Override
	public Map<String, UriParameter> getUriParameters() {
		return uriParameters;
	}

	@Override
	public InternalEndpointRoute addUriParameter(String key, String description, String example) {
		UriParameter param = new UriParameter(key);
		param.setDescription(description);
		param.setExample(example);
		uriParameters.put(key, param);
		return this;
	}

	@Override
	public int compareTo(InternalEndpointRoute o) {
		return getRamlPath().compareTo(o.getRamlPath());
	}

	/**
	 * Convert the provided vertx path to a RAML path.
	 * 
	 * @param path
	 * @return RAML Path which contains '{}' instead of ':' characters
	 */
	private String convertPath(String path) {
		StringBuilder builder = new StringBuilder();
		String[] segments = path.split("/");
		for (int i = 0; i < segments.length; i++) {
			String segment = segments[i];
			if (segment.startsWith(":")) {
				segment = "{" + segment.substring(1) + "}";
			}
			builder.append(segment);
			if (i != segments.length - 1) {
				builder.append("/");
			}
		}
		if (path.endsWith("/")) {
			builder.append("/");
		}
		return builder.toString();
	}

	@Override
	public Class<? extends RestModel> getExampleRequestClass() {
		return exampleRequestClass;
	}

}
