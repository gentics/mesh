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
import com.gentics.mesh.core.data.service.WebRootService;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;

import io.vertx.core.Future;
import rx.Observable;

@Component
public class WebRootHandler {

	@Autowired
	private WebRootService webrootService;

	@Autowired
	private Database db;

	public void handleGetPath(InternalActionContext ac) {

		String path = ac.getParameter("param0");
		try {
			path = URLDecoder.decode(path, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			ac.fail(e);
			return;
		}
		final String decodedPath = path;
		String projectName = ac.getProject().getName();
		MeshAuthUser requestUser = ac.getUser();
		// List<String> languageTags = ac.getSelectedLanguageTags();
		Mesh.vertx().executeBlocking((Future<Node> bch) -> {
			try (Trx tx = db.trx()) {
				Observable<Path> nodePath = webrootService.findByProjectPath(ac, projectName, decodedPath);
				PathSegment lastSegment = nodePath.toBlocking().last().getLast();

				if (lastSegment != null) {
					Node node = lastSegment.getNode();
					if (node == null) {
						throw error(NOT_FOUND, "node_not_found_for_path", decodedPath);
					}

					if (requestUser.hasPermission(ac, node, READ_PERM)) {
						bch.complete(node);
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
				Node node = arh.result();
				node.transformToRest(ac, rh -> {
					if (rh.failed()) {
						ac.fail(rh.cause());
					} else {
						ac.send(JsonUtil.toJson(rh.result()), OK);
					}
				});
			}
		});
	}

}
