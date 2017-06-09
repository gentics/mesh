package com.gentics.mesh.core.verticle.webroot;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.math.NumberUtils;

import com.gentics.ferma.Tx;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.service.WebRootService;
import com.gentics.mesh.core.image.spi.ImageManipulator;
import com.gentics.mesh.core.rest.error.NotModifiedException;
import com.gentics.mesh.core.verticle.node.BinaryFieldResponseHandler;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;
import com.gentics.mesh.util.ETag;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.ext.web.RoutingContext;
import rx.Single;

@Singleton
public class WebRootHandler {

	private WebRootService webrootService;

	private ImageManipulator imageManipulator;

	private Database db;

	@Inject
	public WebRootHandler(Database database, ImageManipulator imageManipulator, WebRootService webrootService) {
		this.db = database;
		this.imageManipulator = imageManipulator;
		this.webrootService = webrootService;
	}

	/**
	 * Handle a webroot get request.
	 * 
	 * @param rc
	 */
	public void handleGetPath(RoutingContext rc) {
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
		String path = ac.getParameter("param0");
		final String decodedPath = "/" + path;
		MeshAuthUser requestUser = ac.getUser();
		// List<String> languageTags = ac.getSelectedLanguageTags();
		db.operateTx(() -> {

			String releaseUuid = ac.getRelease().getUuid();
			// Load all nodes for the given path
			Path nodePath = webrootService.findByProjectPath(ac, decodedPath);
			PathSegment lastSegment = nodePath.getLast();

			// Check whether the path actually points to a valid node
			if (lastSegment == null) {
				throw error(NOT_FOUND, "node_not_found_for_path", decodedPath);
			}
			NodeGraphFieldContainer container = lastSegment.getContainer();
			if (container == null) {
				throw error(NOT_FOUND, "node_not_found_for_path", decodedPath);
			}

			requestUser.failOnNoReadPermission(container, releaseUuid);

			GraphField field = lastSegment.getPathField();
			if (field instanceof BinaryGraphField) {
				BinaryGraphField binaryField = (BinaryGraphField) field;
				String sha512sum = binaryField.getSHA512Sum();

				// Check the etag
				String etagKey = sha512sum;
				if (binaryField.hasImage() && ac.getImageParameters().isSet()) {
					etagKey += ac.getImageParameters().getQueryParameters();
				}
				String etag = ETag.hash(etagKey);
				ac.setEtag(etag, false);
				if (ac.matches(etag, false)) {
					return Single.error(new NotModifiedException());
				} else {
					try (Tx tx = db.tx()) {
						// TODO move binary handler outside of event loop scope to avoid bogus object creation
						BinaryFieldResponseHandler handler = new BinaryFieldResponseHandler(rc, imageManipulator);
						handler.handle(binaryField);
						return null;
					}
				}
			} else {
				Node node = container.getParentNode();
				String etag = node.getETag(ac);
				ac.setEtag(etag, true);
				if (ac.matches(etag, true)) {
					return Single.error(new NotModifiedException());
				} else {
					// Use the language for which the node was resolved
					List<String> languageTags = new ArrayList<>();
					languageTags.add(lastSegment.getLanguageTag());
					languageTags.addAll(ac.getNodeParameters().getLanguageList());
					return node.transformToRest(ac, 0, languageTags.toArray(new String[0]));
				}
			}

		}).subscribe(model -> {
			if (model != null) {
				ac.send(JsonUtil.toJson(model),
						HttpResponseStatus.valueOf(NumberUtils.toInt(rc.data().getOrDefault("statuscode", "").toString(), OK.code())));
			}
		}, ac::fail);

	}

}
