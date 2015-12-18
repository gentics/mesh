package com.gentics.mesh.core.verticle.webroot;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.error;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.node.Node;

import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.impl.NodeImpl;

import com.gentics.mesh.core.data.service.WebRootService;
import com.gentics.mesh.core.image.spi.ImageManipulator;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.verticle.node.BinaryFieldResponseHandler;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import rx.Observable;

@Component
public class WebRootHandler {

	@Autowired
	private WebRootService webrootService;

	@Autowired
	private ImageManipulator imageManipulator;

	@Autowired
	private Database db;

	public void handleGetPath(RoutingContext rc) {
		InternalActionContext ac = InternalActionContext.create(rc);
		String path = ac.getParameter("param0");
		try {
			path = URLDecoder.decode(path, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			ac.fail(e);
			return;
		}
		final String decodedPath = path;
		MeshAuthUser requestUser = ac.getUser();
		// List<String> languageTags = ac.getSelectedLanguageTags();
		Mesh.vertx().executeBlocking((Future<PathSegment> bch) -> {
			try (NoTrx tx = db.noTrx()) {
				Observable<Path> nodePath = webrootService.findByProjectPath(ac, decodedPath);
				PathSegment lastSegment = nodePath.toBlocking().last().getLast();

				if (lastSegment != null) {
					Node node = lastSegment.getNode();
					if (node == null) {
						throw error(NOT_FOUND, "node_not_found_for_path", decodedPath);
					}
					if (requestUser.hasPermissionSync(ac, node, READ_PERM)) {
						bch.complete(lastSegment);
					} else {
						bch.fail(new HttpStatusCodeErrorException(FORBIDDEN, ac.i18n("error_missing_perm", node.getUuid())));
					}
					// requestUser.isAuthorised(node, READ_PERM, rh -> {
					// languageTags.add(lastSegment.getLanguageTag());
					// if (rh.result()) {
					// bch.complete(node);
					// } else {
					// bch.fail(new HttpStatusCodeErrorException(FORBIDDEN, ac.i18n("error_missing_perm", node.getUuid())));
					// }
					// });

				} else {
					throw error(NOT_FOUND, "node_not_found_for_path", decodedPath);
				}
			}
		} , arh -> {
			if (arh.failed()) {
				ac.fail(arh.cause());
			}
			/* TODO copy this to all other handlers. We need to catch async errors as well elsewhere */
			if (arh.succeeded()) {
				PathSegment lastSegment = arh.result();
				Node node = lastSegment.getNode();
				ac.put(NodeImpl.AC_LANGUAGE_KEY, lastSegment.getLanguage());
				GraphField field = lastSegment.getPathField();
				if (field instanceof StringGraphField) {
					node.transformToRest(ac).subscribe(model -> {
						ac.send(JsonUtil.toJson(model), OK);
					}, error -> {
						ac.fail(error);
					});
				}
				if (field instanceof BinaryGraphField) {
					BinaryGraphField binaryField = (BinaryGraphField) field;
					try (NoTrx tx = db.noTrx()) {
						BinaryFieldResponseHandler handler = new BinaryFieldResponseHandler(rc, imageManipulator);
						handler.handle(binaryField);
					}
				}
			}
		});
}

}
