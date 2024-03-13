package com.gentics.mesh.core.endpoint.webrootfield;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.service.WebRootService;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.handler.AbstractWebrootHandler;
import com.gentics.mesh.core.endpoint.node.BinaryDownloadHandler;
import com.gentics.mesh.core.endpoint.node.BinaryTransformHandler;
import com.gentics.mesh.core.endpoint.node.BinaryVariantsHandler;
import com.gentics.mesh.core.endpoint.node.NodeCrudHandler;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.error.NotModifiedException;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;
import com.gentics.mesh.path.impl.PathSegmentImpl;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for Webroot field functions.
 * 
 * @author plyhun
 *
 */
@Singleton
public class WebRootFieldHandler extends AbstractWebrootHandler {
	private static final Logger log = LoggerFactory.getLogger(WebRootFieldHandler.class);

	private final BinaryDownloadHandler binaryDownloadHandler;
	private final BinaryVariantsHandler binaryVariantsHandler;

	@Inject
	public WebRootFieldHandler(Database database, WebRootService webrootService,
							   BinaryTransformHandler binaryTransformHandler, BinaryDownloadHandler binaryDownloadHandler, BinaryVariantsHandler binaryVariantsHandler,
							   NodeCrudHandler nodeCrudHandler, BootstrapInitializer boot, MeshOptions options, WriteLock writeLock, HandlerUtilities utils) {
		super(database, webrootService, nodeCrudHandler, boot, options, writeLock, utils);
		this.binaryDownloadHandler = binaryDownloadHandler;
		this.binaryVariantsHandler = binaryVariantsHandler;
	}

	/**
	 * Handle a webrootpath get request.
	 * 
	 * @param rc
	 */
	public void handleGetPathField(RoutingContext rc) {
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
		String fieldName = rc.request().getParam("param0");

		utils.syncTx(ac, tx -> {
			// Cut all the common parts off, obtaining the project-based path.
			String path = rc.request().path();
			path = path.substring(rc.mountPoint().length());
			path = path.substring(fieldName.length() + 1); 
			if (StringUtils.isNotBlank(path) && !path.startsWith("/")) {
				path = "/" + path;
			}

			Path nodePath = findNodePathByProjectPath(ac, path);
			PathSegment lastSegment = nodePath.getLast();
			PathSegmentImpl graphSegment = (PathSegmentImpl) lastSegment;
			HibNode node = findNodeByPath(ac, rc, nodePath, path);

			HibNodeFieldContainer container = graphSegment.getContainer();
			FieldSchema fieldSchema = container.getSchemaContainerVersion().getSchema().getField(fieldName);

			if (fieldSchema == null) {
				throw error(NOT_FOUND, "error_field_not_found_with_name", fieldName);
			}

			FieldTypes fieldType = FieldTypes.valueByName(fieldSchema.getType());

			switch (fieldType) {
			case S3BINARY:
				log.debug("S3 Binary field {} for node with uuid:{} found at {} for reading", fieldName, node.getUuid(), path);
				binaryDownloadHandler.handleReadBinaryField(rc, node.getUuid(), fieldName);
				return null;
			case BINARY:
				log.debug("Binary field {} for node with uuid:{} found at {} for reading", fieldName, node.getUuid(), path);
				binaryDownloadHandler.handleReadBinaryField(rc, node.getUuid(), fieldName);
				return null;
			case HTML:
				rc.response().putHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.TEXT_HTML_UTF8);
				break;
			case STRING:
			case DATE:
			case NUMBER:
			case BOOLEAN:
				rc.response().putHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.TEXT_PLAIN_UTF8);
				break;
			default:
				rc.response().putHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.APPLICATION_JSON_UTF8);
				break;
			}
			String etag = tx.nodeDao().getETag(node, ac);
			ac.setEtag(etag, true);
			if (ac.matches(etag, true)) {
				throw new NotModifiedException();
			}
			// Use the language for which the node was resolved
			List<String> languageTags = new ArrayList<>();
			languageTags.add(lastSegment.getLanguageTag());
			languageTags.addAll(ac.getNodeParameters().getLanguageList(options));

			Field field = container.getRestField(ac, fieldName, fieldSchema, languageTags, 0);
			if (field == null) {
				throw error(NOT_FOUND, "error_field_not_found_with_name", fieldName);
			}

			String fieldTypeName = fieldSchema.getType();
			ac.setWebrootResponseType(fieldTypeName.toLowerCase());

			log.debug("{} field {} for node with uuid:{} found at {} for reading", fieldTypeName, fieldName, node.getUuid(), path);

			return field;
		}, field -> {
			// Check if field is binary and already processed accordingly
			if (field != null) {
				// Check if field is JSON, to set corresponding HTTP header value
				String contentType = rc.response().headers().get(HttpHeaders.CONTENT_TYPE);
				ac.send(
					HttpConstants.APPLICATION_JSON_UTF8.equals(contentType) ? field.toJson(ac.isMinify(options.getHttpServerOptions())) : field.toString(),
					HttpResponseStatus.valueOf(rc.response().getStatusCode()),
					contentType);
			}
		});
	}

	/**
	 * Handle a webrootpath post request. At the moment is only used for upserting the binary field variants.
	 * 
	 * @param rc
	 */
	public void handlePostPathField(RoutingContext rc) {
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
		String fieldName = rc.request().getParam("param0");

		utils.syncTx(ac, tx -> {
			// Cut all the common parts off, obtaining the project-based path.
			String path = rc.request().path();
			path = path.substring(rc.mountPoint().length());
			path = path.substring(fieldName.length() + 1); 
			if (StringUtils.isNotBlank(path) && !path.startsWith("/")) {
				path = "/" + path;
			}

			Path nodePath = findNodePathByProjectPath(ac, path);
			PathSegment lastSegment = nodePath.getLast();
			PathSegmentImpl graphSegment = (PathSegmentImpl) lastSegment;
			HibNode node = findNodeByPath(ac, rc, nodePath, path);

			HibNodeFieldContainer container = graphSegment.getContainer();
			FieldSchema fieldSchema = container.getSchemaContainerVersion().getSchema().getField(fieldName);

			if (fieldSchema == null) {
				throw error(NOT_FOUND, "error_field_not_found_with_name", fieldName);
			}

			FieldTypes fieldType = FieldTypes.valueByName(fieldSchema.getType());

			switch (fieldType) {
			case BINARY:
				break;
			default:
				throw error(BAD_REQUEST, "error_found_field_is_not_binary", fieldName);
			}

			// Use the language for which the node was resolved
			List<String> languageTags = new ArrayList<>();
			languageTags.add(lastSegment.getLanguageTag());
			languageTags.addAll(ac.getNodeParameters().getLanguageList(options));

			Field field = container.getRestField(ac, fieldName, fieldSchema, languageTags, 0);
			if (field == null) {
				throw error(NOT_FOUND, "error_field_not_found_with_name", fieldName);
			}

			String fieldTypeName = fieldSchema.getType();
			log.debug("{} field {} for node with uuid:{} found at {} for reading", fieldTypeName, fieldName, node.getUuid(), path);

			return node.getUuid();
		}, nodeUuid -> binaryVariantsHandler.handleUpsertBinaryFieldVariants(ac, nodeUuid, fieldName));
	}
}
