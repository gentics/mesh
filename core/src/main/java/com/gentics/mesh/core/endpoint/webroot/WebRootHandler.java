package com.gentics.mesh.core.endpoint.webroot;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.util.URIUtils.decodeSegment;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpHeaders.CACHE_CONTROL;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.service.WebRootServiceImpl;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.node.BinaryFieldResponseHandler;
import com.gentics.mesh.core.endpoint.node.NodeCrudHandler;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.NotModifiedException;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.etc.config.AuthenticationOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.http.MeshHeaders;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;
import com.gentics.mesh.path.impl.PathSegmentImpl;
import com.gentics.mesh.util.ETag;
import com.gentics.mesh.util.NumberUtils;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

/**
 * REST handler for webroot requests.
 */
@Singleton
public class WebRootHandler {

	private static final Logger log = LoggerFactory.getLogger(NodeImpl.class);

	private static final String WEBROOT_LAST_SEGMENT = "WEBROOT_SEGMENT_NAME";

	private final WebRootServiceImpl webrootService;

	private final BinaryFieldResponseHandler binaryFieldResponseHandler;

	private final Database db;

	private final NodeCrudHandler nodeCrudHandler;

	private final BootstrapInitializer boot;

	private final MeshOptions options;

	private final WriteLock writeLock;

	private final HandlerUtilities utils;

	@Inject
	public WebRootHandler(Database database, WebRootServiceImpl webrootService, BinaryFieldResponseHandler binaryFieldResponseHandler,
		NodeCrudHandler nodeCrudHandler, BootstrapInitializer boot, MeshOptions options, WriteLock writeLock, HandlerUtilities utils) {
		this.db = database;
		this.webrootService = webrootService;
		this.binaryFieldResponseHandler = binaryFieldResponseHandler;
		this.nodeCrudHandler = nodeCrudHandler;
		this.boot = boot;
		this.options = options;
		this.writeLock = writeLock;
		this.utils = utils;
	}

	/**
	 * Handle a webroot get request.
	 * 
	 * @param rc
	 */
	public void handleGetPath(RoutingContext rc) {
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
		String path = rc.request().path().substring(
			rc.mountPoint().length());

		utils.syncTx(ac, tx -> {
			HibUser requestUser = ac.getUser();
			UserDao userDao = tx.userDao();
			NodeDao nodeDao = tx.nodeDao();

			String branchUuid = tx.getBranch(ac).getUuid();
			// Load all nodes for the given path
			ContainerType type = ContainerType.forVersion(ac.getVersioningParameters().getVersion());
			Path nodePath = webrootService.findByProjectPath(ac, path, type);
			if (!nodePath.isFullyResolved()) {
				throw error(NOT_FOUND, "node_not_found_for_path", decodeSegment(nodePath.getTargetPath()));
			}
			PathSegment lastSegment = nodePath.getLast();

			// Check whether the path actually points to a valid node
			if (lastSegment == null) {
				throw error(NOT_FOUND, "node_not_found_for_path", decodeSegment(path));
			}
			PathSegmentImpl graphSegment = (PathSegmentImpl) lastSegment;
			HibNodeFieldContainer container = graphSegment.getContainer();
			if (container == null) {
				throw error(NOT_FOUND, "node_not_found_for_path", decodeSegment(path));
			}

			String version = ac.getVersioningParameters().getVersion();
			HibNode node = tx.contentDao().getParentNode(container);
			addCacheControl(rc, node, version);
			userDao.failOnNoReadPermission(requestUser, container, branchUuid, version);

			rc.response().putHeader(MeshHeaders.WEBROOT_NODE_UUID, node.getUuid());
			// TODO decide whether we want to add also lang, version

			HibField field = graphSegment.getPathField();
			if (field instanceof BinaryGraphField) {
				BinaryGraphField binaryField = (BinaryGraphField) field;
				String sha512sum = binaryField.getBinary().getSHA512Sum();

				// Check the etag
				String etagKey = sha512sum;
				if (binaryField.hasProcessableImage()) {
					etagKey += ac.getImageParameters().getQueryParameters();
				}
				String etag = ETag.hash(etagKey);
				ac.setEtag(etag, false);
				if (ac.matches(etag, false)) {
					throw new NotModifiedException();
				}
				binaryFieldResponseHandler.handle(rc, binaryField);
				return null;
			} else {
				String etag = nodeDao.getETag(node, ac);
				ac.setEtag(etag, true);
				if (ac.matches(etag, true)) {
					throw new NotModifiedException();
				}
				// Use the language for which the node was resolved
				List<String> languageTags = new ArrayList<>();
				languageTags.add(lastSegment.getLanguageTag());
				languageTags.addAll(ac.getNodeParameters().getLanguageList(options));
				ac.setWebrootResponseType("node");
				return nodeDao.transformToRestSync(node, ac, 0, languageTags.toArray(new String[0]));
			}
		}, model -> {
			if (model != null) {
				ac.send(JsonUtil.toJson(model),
					HttpResponseStatus.valueOf(NumberUtils.toInt(rc.data().getOrDefault("statuscode", "").toString(), OK.code())));
			}
		});

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

	/**
	 * Handle the given request using the webroot create or update code.
	 * 
	 * This handler will automatically invoke update or create if the path could be resolved or not. Please note that partial paths can only be used for update
	 * if only the last segment is missing from the resolved path. The last segment will thus be created with the request. The parent node information may be
	 * omitted from any request to the handler since the information can be deduced via the resolved parent segment.
	 * 
	 * @param rc
	 * @param method
	 */
	public void handleUpdateCreatePath(RoutingContext rc, HttpMethod method) {
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
		String path = rc.request().path().substring(
			rc.mountPoint().length());

		String uuid = null;
		try (WriteLock lock = writeLock.lock(ac)) {
			uuid = db.tx(tx -> {
				ContentDao contentDao = tx.contentDao();

				// Load all nodes for the given path
				ContainerType type = ContainerType.forVersion(ac.getVersioningParameters().getVersion());
				Path nodePath = webrootService.findByProjectPath(ac, path, type);
				if (nodePath.isPrefixMismatch()) {
					throw error(NOT_FOUND, "webroot_error_prefix_invalid", decodeSegment(path), tx.getBranch(ac).getPathPrefix());
				}

				// Check whether path could be resolved at all
				if (nodePath.getResolvedPath() == null) {
					throw error(NOT_FOUND, "node_not_found_for_path", decodeSegment(nodePath.getTargetPath()));
				}

				PathSegment lastSegment = nodePath.getLast();
				List<PathSegment> segments = nodePath.getSegments();

				// Update Node
				if (nodePath.isFullyResolved()) {
					PathSegmentImpl graphSegment = (PathSegmentImpl) lastSegment;
					HibNodeFieldContainer container = graphSegment.getContainer();
					NodeUpdateRequest request = ac.fromJson(NodeUpdateRequest.class);
					// We can deduce a missing the language via the path
					if (request.getLanguage() == null) {
						String lang = container.getLanguageTag();
						log.debug("Using deduced language of container: " + lang);
						request.setLanguage(lang);
					}
					ac.setBody(request);
					return contentDao.getParentNode(container).getUuid();
				} else {
					int diff = nodePath.getInitialStack().size() - nodePath.getSegments().size();
					if (diff > 1) {
						String resolvedPath = nodePath.getResolvedPath();
						throw error(NOT_FOUND, "webroot_error_parent_not_found", resolvedPath);
					}

					// Deduce parent node
					NodeCreateRequest request = ac.fromJson(NodeCreateRequest.class);

					// Deduce parent node
					if (request.getParentNode() == null || request.getParentNode().getUuid() == null) {
						HibNode parentNode = null;
						if (segments.size() == 0) {
							parentNode = tx.getProject(ac).getBaseNode();
						} else {
							PathSegment parentSegment = segments.get(segments.size() - 1);
							PathSegmentImpl graphSegment = (PathSegmentImpl) parentSegment;
							parentNode = contentDao.getParentNode(graphSegment.getContainer());
						}
						String parentUuid = parentNode.getUuid();
						log.debug("Using deduced parent node uuid: " + parentUuid);
						request.setParentNodeUuid(parentUuid);
					}
					ac.put(WEBROOT_LAST_SEGMENT, nodePath.getInitialStack().firstElement());
					ac.setBody(request);
					return null;
				}
			});
		}

		if (uuid != null) {
			nodeCrudHandler.handleUpdate(ac, uuid);
		} else {
			nodeCrudHandler.handleCreate(ac);
		}

	}

}
