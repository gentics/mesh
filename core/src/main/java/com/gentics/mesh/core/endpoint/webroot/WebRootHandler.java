package com.gentics.mesh.core.endpoint.webroot;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.util.URIUtils.decodeSegment;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

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
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
import com.gentics.mesh.core.data.service.WebRootService;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.handler.AbstractWebrootHandler;
import com.gentics.mesh.core.endpoint.node.BinaryFieldResponseHandler;
import com.gentics.mesh.core.endpoint.node.NodeCrudHandler;
import com.gentics.mesh.core.endpoint.node.S3BinaryFieldResponseHandler;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.NotModifiedException;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.etc.config.MeshOptions;
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
public class WebRootHandler extends AbstractWebrootHandler {

	private static final Logger log = LoggerFactory.getLogger(WebRootHandler.class);

	private final BinaryFieldResponseHandler binaryFieldResponseHandler;
	private final S3BinaryFieldResponseHandler s3binaryFieldResponseHandler;

	@Inject
	public WebRootHandler(Database database, WebRootService webrootService, BinaryFieldResponseHandler binaryFieldResponseHandler,
	  	S3BinaryFieldResponseHandler s3binaryFieldResponseHandler,
		NodeCrudHandler nodeCrudHandler, BootstrapInitializer boot, MeshOptions options, WriteLock writeLock, HandlerUtilities utils) {
		super(database, webrootService, nodeCrudHandler, boot, options, writeLock, utils);
		this.binaryFieldResponseHandler = binaryFieldResponseHandler;
		this.s3binaryFieldResponseHandler = s3binaryFieldResponseHandler;
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
			NodeDao nodeDao = tx.nodeDao();

			Path nodePath = findNodePathByProjectPath(ac, path);
			PathSegment lastSegment = nodePath.getLast();
			PathSegmentImpl graphSegment = (PathSegmentImpl) lastSegment;
			HibNode node = findNodeByPath(ac, rc, nodePath, path);

			HibField field = graphSegment.getPathField();
			if (field instanceof HibBinaryField) {
				HibBinaryField binaryField = (HibBinaryField) field;
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
			} else if (field instanceof S3HibBinaryField) {
				S3HibBinaryField s3binaryField = (S3HibBinaryField) field;
				String s3ObjectKey = s3binaryField.getBinary().getS3ObjectKey();
				//String version = s3binaryField.getElementVersion();

				// Check the etag
				String etagKey = s3ObjectKey; // + version;
				if (s3binaryField.hasProcessableImage()) {
					etagKey += ac.getImageParameters().getQueryParameters();
				}
				String etag = ETag.hash(etagKey);
				ac.setEtag(etag, false);
				if (ac.matches(etag, false)) {
					throw new NotModifiedException();
				}
				s3binaryFieldResponseHandler.handle(rc, node, s3binaryField);
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
				ac.send(JsonUtil.toJson(model, ac.isMinify(options.getHttpServerOptions())),
					HttpResponseStatus.valueOf(NumberUtils.toInt(rc.data().getOrDefault("statuscode", "").toString(), OK.code())));
			}
		});

	}

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
					return contentDao.getNode(container).getUuid();
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
							parentNode = contentDao.getNode(graphSegment.getContainer());
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
