package com.gentics.mesh.core.endpoint.webrootfield;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.service.WebRootServiceImpl;
import com.gentics.mesh.core.endpoint.handler.AbstractWebrootHandler;
import com.gentics.mesh.core.endpoint.node.BinaryDownloadHandler;
import com.gentics.mesh.core.endpoint.node.BinaryTransformHandler;
import com.gentics.mesh.core.endpoint.node.NodeCrudHandler;
import com.gentics.mesh.core.rest.error.NotModifiedException;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;
import com.gentics.mesh.util.NumberUtils;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class WebRootFieldHandler extends AbstractWebrootHandler {
	private static final Logger log = LoggerFactory.getLogger(WebRootFieldHandler.class);
	
	private BinaryDownloadHandler binaryDownloadHandler;
	
	@Inject
	public WebRootFieldHandler(Database database, WebRootServiceImpl webrootService,
			BinaryTransformHandler binaryTransformHandler, BinaryDownloadHandler binaryDownloadHandler,
		NodeCrudHandler nodeCrudHandler, BootstrapInitializer boot, MeshOptions options, WriteLock writeLock, HandlerUtilities utils) {
		super(database, webrootService, nodeCrudHandler, boot, options, writeLock, utils);
		this.binaryDownloadHandler = binaryDownloadHandler;
	}

	/**
	 * Handle a webrootpath get request.
	 * 
	 * @param rc
	 */
	public void handleGetPathField(RoutingContext rc) {
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
		String fieldName = rc.request().getParam("param0");
		String path = rc.request().getParam("param1");
		
		utils.syncTx(ac, tx -> {
			Path nodePath = findNodePathByProjectPath(ac, path);
			PathSegment lastSegment = nodePath.getLast();
			Node node = findNodeByPath(ac, rc, nodePath, path);
			
			NodeGraphFieldContainer container = lastSegment.getContainer();
			if (container.getBinary(fieldName) != null) {
				log.debug("Binary field {} for node with uuid:{} found at {} for reading", fieldName, node.getUuid(), path);
				binaryDownloadHandler.handleReadBinaryField(rc, node.getUuid(), fieldName);
				return null;
			} else {
				String etag = node.getETag(ac);
				ac.setEtag(etag, true);
				if (ac.matches(etag, true)) {
					throw new NotModifiedException();
				}
				// Use the language for which the node was resolved
				List<String> languageTags = new ArrayList<>();
				languageTags.add(lastSegment.getLanguageTag());
				languageTags.addAll(ac.getNodeParameters().getLanguageList(options));

				FieldSchema fieldSchema = container.getSchemaContainerVersion().getSchema().getField(fieldName);
				Field field = container.getRestFieldFromGraph(ac, fieldName, fieldSchema, languageTags, 0);
				String fieldTypeName = fieldSchema.getType();
				ac.setWebrootResponseType(fieldTypeName.toLowerCase());

				log.debug("{} field {} for node with uuid:{} found at {} for reading", fieldTypeName, fieldName, node.getUuid(), path);
				return field;
			}			
		}, model -> {
			if (model != null) {
				ac.send(model.toJson(),
					HttpResponseStatus.valueOf(NumberUtils.toInt(rc.data().getOrDefault("statuscode", "").toString(), OK.code())));
			}
		});
	}
}
