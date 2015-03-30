package com.gentics.cailun.core;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.apex.Route;
import io.vertx.ext.apex.Router;
import io.vertx.ext.apex.RoutingContext;
import io.vertx.ext.apex.Session;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.cailun.auth.CaiLunAuthServiceImpl;
import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.generic.AbstractPersistable;
import com.gentics.cailun.core.data.model.generic.GenericNode;
import com.gentics.cailun.error.EntityNotFoundException;
import com.gentics.cailun.error.HttpStatusCodeErrorException;
import com.gentics.cailun.error.InvalidPermissionException;
import com.gentics.cailun.etc.config.CaiLunConfigurationException;
import com.gentics.cailun.path.PagingInfo;

public abstract class AbstractRestVerticle extends AbstractSpringVerticle {

	private static final Logger log = LoggerFactory.getLogger(AbstractRestVerticle.class);

	public static final String APPLICATION_JSON = ContentType.APPLICATION_JSON.getMimeType();

	public static final long DEFAULT_PER_PAGE = 25;

	protected Router localRouter = null;
	protected String basePath;
	protected HttpServer server;

	protected AbstractRestVerticle(String basePath) {
		this.basePath = basePath;
	}

	@Override
	public void start() throws Exception {
		this.localRouter = setupLocalRouter();
		if (localRouter == null) {
			throw new CaiLunConfigurationException("The local router was not setup correctly. Startup failed.");
		}
		log.info("Starting http server..");
		server = vertx.createHttpServer(new HttpServerOptions().setPort(config().getInteger("port")));
		server.requestHandler(routerStorage.getRootRouter()::accept);
		server.listen();
		log.info("Started http server.. Port: " + config().getInteger("port"));
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

	/**
	 * Returns the cailun auth service which can be used to authenticate resources.
	 * 
	 * @return
	 */
	protected CaiLunAuthServiceImpl getAuthService() {
		return springConfiguration.authService();
	}

	/**
	 * Extract the given uri parameter and load the object. Permissions and load verification will also be done by this method.
	 * 
	 * @param rc
	 * @param param
	 *            Name of the uri parameter which hold the uuid
	 * @param perm
	 *            Permission type which will be checked
	 * @return
	 */
	public <T> T getObject(RoutingContext rc, String param, PermissionType perm) {
		String uuid = rc.request().params().get(param);
		if (StringUtils.isEmpty(uuid)) {
			throw new HttpStatusCodeErrorException(400, i18n.get(rc, "error_request_parameter_missing", param));
		}
		return getObjectByUUID(rc, uuid, perm);

	}

	public <T> T getObjectByUUID(RoutingContext rc, String projectName, String uuid, PermissionType perm) {
		if (StringUtils.isEmpty(uuid)) {
			// TODO i18n, add info about uuid source?
			throw new HttpStatusCodeErrorException(400, "missing uuid");
		}
		GenericNode object = genericNodeService.findByUUID(projectName, uuid);
		if (object == null) {
			throw new EntityNotFoundException(i18n.get(rc, "object_not_found_for_uuid", uuid));
		}
		failOnMissingPermission(rc, object, perm);
		// TODO type check
		return (T) object;
	}

	/**
	 * Load the object with the given uuid and check the given permissions.
	 * 
	 * @param rc
	 * @param uuid
	 * @param perm
	 * @return
	 */
	public <T> T getObjectByUUID(RoutingContext rc, String uuid, PermissionType perm) {
		if (StringUtils.isEmpty(uuid)) {
			// TODO i18n, add info about uuid source?
			throw new HttpStatusCodeErrorException(400, "missing uuid");
		}
		GenericNode object = genericNodeService.findByUUID(uuid);
		if (object == null) {
			throw new EntityNotFoundException(i18n.get(rc, "object_not_found_for_uuid", uuid));
		}
		failOnMissingPermission(rc, object, perm);
		// TODO type check
		return (T) object;
	}

	/**
	 * Check the permission and throw an invalid permission exception when no matching permission could be found.
	 * 
	 * @param rc
	 * @param node
	 * @param type
	 * @return
	 */
	protected void failOnMissingPermission(RoutingContext rc, AbstractPersistable node, PermissionType type) throws InvalidPermissionException {
		if (!hasPermission(rc, node, type)) {
			// TODO i18n
			throw new InvalidPermissionException("Missing permission on object {" + node.getUuid() + "}");
		}
	}

	protected boolean hasPermission(RoutingContext rc, AbstractPersistable node, PermissionType type) {
		if (node != null) {
			Session session = rc.session();
			boolean perm = getAuthService().hasPermission(session.getLoginID(), new CaiLunPermission(node, type));
			if (perm) {
				return true;
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

	protected PagingInfo getPagingInfo(RoutingContext rc) {
		MultiMap params = rc.request().params();
		long page = NumberUtils.toLong(params.get("page"), 0);
		long perPage = Long.valueOf(NumberUtils.toLong(params.get("per_page"), DEFAULT_PER_PAGE));
		return new PagingInfo(page, perPage);
	}

}
