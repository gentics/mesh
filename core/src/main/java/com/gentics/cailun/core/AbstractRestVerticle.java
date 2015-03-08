package com.gentics.cailun.core;

import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.apex.core.Route;
import io.vertx.ext.apex.core.Router;
import io.vertx.ext.apex.core.RoutingContext;
import io.vertx.ext.apex.core.Session;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.entity.ContentType;
import org.codehaus.jackson.map.ObjectMapper;

import com.gentics.cailun.auth.CaiLunAuthServiceImpl;
import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.generic.GenericNode;
import com.gentics.cailun.core.rest.common.response.GenericPermissionDeniedResponse;
import com.gentics.cailun.etc.RouterStorage;
import com.gentics.cailun.etc.config.CaiLunConfigurationException;

public abstract class AbstractRestVerticle extends AbstractSpringVerticle {

	public static final String APPLICATION_JSON = ContentType.APPLICATION_JSON.getMimeType();

	protected Router localRouter = null;
	protected String basePath;
	protected ObjectMapper mapper;
	protected HttpServer server;

	protected AbstractRestVerticle(String basePath) {
		this.basePath = basePath;
	}

	@Override
	public void start() throws Exception {
		this.mapper = new ObjectMapper();
		this.localRouter = setupLocalRouter();
		if (localRouter == null) {
			throw new CaiLunConfigurationException("The local router was not setup correctly. Startup failed.");
		}
		// TODO use global config for port?
		server = vertx.createHttpServer(new HttpServerOptions().setPort(config().getInteger("port")));
		server.requestHandler(routerStorage.getRootRouter()::accept);
		server.listen();
		registerEndPoints();

	}

	public abstract void registerEndPoints() throws Exception;

	public abstract Router setupLocalRouter();

	@Override
	public void stop() throws Exception {
		localRouter.clear();
	}

	public Router getRouter() {
		return localRouter;
	}

	public HttpServer getServer() {
		return server;
	}

	/**
	 * Wrapper for getRouter().route(path)
	 * 
	 * @return
	 */
	protected Route route(String path) {
		return localRouter.route(path);
	}

	/**
	 * Wrapper for getRouter().route()
	 * 
	 * @return
	 */
	protected Route route() {
		return localRouter.route();
	}

	protected <T> String toJson(T obj) {
		try {
			return mapper.writeValueAsString(obj);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "ERROR";
	}

	@SuppressWarnings("unchecked")
	protected <T> T fromJson(RoutingContext rc, Class<?> classOfT) {
		try {
			String body = rc.getBodyAsString();
			return (T) mapper.readValue(body, classOfT);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
		// TODO compare with jackson
		// return (T) GSON.fromJson(rc.getBodyAsString(), classOfT);

	}

	/**
	 * Returns the cailun auth service which can be used to authenticate resources.
	 * 
	 * @return
	 */
	protected CaiLunAuthServiceImpl getAuthService() {
		return springConfig.authService();
	}

	protected boolean checkPermission(RoutingContext rc, GenericNode node, PermissionType type) {
		if (node != null) {
			Session session = rc.session();
			boolean perm = getAuthService().hasPermission(session.getPrincipal(), new CaiLunPermission(node, type));
			if (perm) {
				return true;
			} else {
				rc.response().setStatusCode(403);
				// TODO i18n
				rc.response().end(toJson(new GenericPermissionDeniedResponse("Missing permission on object {" + node.getUuid() + "}")));
				return false;
			}

		}
		return false;
	}

	public Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
		Map<String, String> queryPairs = new LinkedHashMap<String, String>();
		if (query == null) {
			return queryPairs;
		}
		String[] pairs = query.split("&");
		for (String pair : pairs) {
			int idx = pair.indexOf("=");
			queryPairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
		}
		return queryPairs;
	}

}
