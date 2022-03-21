package com.gentics.mesh.core.endpoint.handler;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.util.URIUtils.decodeSegment;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.vertx.core.http.HttpHeaders.CACHE_CONTROL;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.service.WebRootService;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.node.NodeCrudHandler;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.etc.config.AuthenticationOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.http.MeshHeaders;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;
import com.gentics.mesh.path.impl.PathSegmentImpl;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

/**
 * Base class for webroot-based handlers. Contains basic webroot resolving mechanisms.
 * 
 * @author plyhun
 *
 */
public abstract class AbstractWebrootHandler {

	protected static final Logger log = LoggerFactory.getLogger(WebRootService.class);

	protected static final String WEBROOT_LAST_SEGMENT = "WEBROOT_SEGMENT_NAME";

	protected final WebRootService webrootService;

	protected final Database db;

	protected final NodeCrudHandler nodeCrudHandler;

	protected final BootstrapInitializer boot;

	protected final MeshOptions options;

	protected final WriteLock writeLock;

	protected final HandlerUtilities utils;

	public AbstractWebrootHandler(Database database, WebRootService webrootService,
		NodeCrudHandler nodeCrudHandler, BootstrapInitializer boot, MeshOptions options, WriteLock writeLock, HandlerUtilities utils) {
		this.db = database;
		this.webrootService = webrootService;
		this.nodeCrudHandler = nodeCrudHandler;
		this.boot = boot;
		this.options = options;
		this.writeLock = writeLock;
		this.utils = utils;
	}
	
	/**
	 * Transforms project-based webroot path into absolute Mesh path. May throw a runtime exception, if path is invalid.
	 * 
	 * @param ac
	 * @param path without the project segment
	 * @return absolute Mesh {@link Path}
	 */
	protected Path findNodePathByProjectPath(InternalActionContext ac, String path) {
		// Load all nodes for the given path
		ContainerType type = ContainerType.forVersion(ac.getVersioningParameters().getVersion());
		Path nodePath = webrootService.findByProjectPath(ac, path, type);
		if (!nodePath.isFullyResolved()) {
			throw error(NOT_FOUND, "node_not_found_for_path", decodeSegment(nodePath.getTargetPath()));
		}
		return nodePath;
	}
	
	/**
	 * Finds the node by project-based webroot path. May throw a runtime exception, if path is invalid.
	 * 
	 * @param ac action context
	 * @param rc routing context
	 * @param projectPath path without the project segment
	 * @return found {@link HibNode}
	 */
	protected HibNode findNodeByPath(InternalActionContext ac, RoutingContext rc, String projectPath) {
		Path nodePath = findNodePathByProjectPath(ac, projectPath);
		return findNodeByPath(ac, rc, nodePath, projectPath);
	}
	
	/**
	 * Finds the node by node path. May throw a runtime exception, if path is invalid.
	 * 
	 * @param ac action context
	 * @param rc routing context
	 * @param nodePath node {@link Path}
	 * @param projectPath original path without the project segment, used in the error logging
	 * @return found {@link HibNode}
	 */
	protected HibNode findNodeByPath(InternalActionContext ac, RoutingContext rc, Path nodePath, String projectPath) {
		HibUser requestUser = ac.getUser();
		Tx tx = Tx.get();
		UserDao userDao = tx.userDao();
		
		String branchUuid = tx.getBranch(ac).getUuid();
		if (!nodePath.isFullyResolved()) {
			throw error(NOT_FOUND, "node_not_found_for_path", decodeSegment(nodePath.getTargetPath()));
		}

		PathSegment lastSegment = nodePath.getLast();

		// Check whether the path actually points to a valid node
		if (lastSegment == null) {
			throw error(NOT_FOUND, "node_not_found_for_path", decodeSegment(projectPath));
		}
		PathSegmentImpl graphSegment = (PathSegmentImpl) lastSegment;
		HibNodeFieldContainer container = graphSegment.getContainer();
		if (container == null) {
			throw error(NOT_FOUND, "node_not_found_for_path", decodeSegment(projectPath));
		}

		String version = ac.getVersioningParameters().getVersion();
		HibNode node = tx.contentDao().getNode(container);
		addCacheControl(rc, node, version);
		userDao.failOnNoReadPermission(requestUser, container, branchUuid, version);

		rc.response().putHeader(MeshHeaders.WEBROOT_NODE_UUID, node.getUuid());
		// TODO decide whether we want to add also lang, version
		
		return node;
	}

	/**
	 * Add the cache control headers.
	 * 
	 * @param rc
	 * @param node
	 * @param version
	 */
	private void addCacheControl(RoutingContext rc, HibNode node, String version) {
		if (isPublic(node, version)) {
			rc.response().putHeader(CACHE_CONTROL, "public");
		} else {
			rc.response().putHeader(CACHE_CONTROL, "private");
		}

	}

	/**
	 * Checks whether the content is readable via anonymous user.
	 * 
	 * @param node
	 * @param version
	 * @return
	 */

	private boolean isPublic(HibNode node, String version) {
		RoleDao roleDao = Tx.get().roleDao();

		HibRole anonymousRole = boot.anonymousRole();
		AuthenticationOptions authOptions = options.getAuthenticationOptions();
		if (anonymousRole != null && authOptions != null && authOptions.isEnableAnonymousAccess()) {
			if (roleDao.hasPermission(anonymousRole, READ_PERM, node)) {
				return true;
			}
			boolean requestsPublished = "published".equals(version);
			if (requestsPublished && roleDao.hasPermission(anonymousRole, READ_PUBLISHED_PERM, node)) {
				return true;
			}
		}
		return false;
	}
}
