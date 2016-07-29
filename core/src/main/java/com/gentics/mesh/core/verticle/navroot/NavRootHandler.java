package com.gentics.mesh.core.verticle.navroot;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.service.WebRootService;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;

import io.vertx.ext.web.RoutingContext;
import rx.Single;

@Component
public class NavRootHandler {
	@Autowired
	private WebRootService webrootService;

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
		final String decodedPath = "/" + path;
		MeshAuthUser requestUser = ac.getUser();

		db.asyncNoTx(() -> {
			Single<Path> nodePath = webrootService.findByProjectPath(ac, decodedPath);
			PathSegment lastSegment = nodePath.toBlocking().value().getLast();

			if (lastSegment != null) {
				Node node = lastSegment.getNode();
				if (node == null) {
					throw error(NOT_FOUND, "node_not_found_for_path", decodedPath);
				}
				if (requestUser.hasPermissionSync(ac, node, READ_PERM)) {
					return node.transformToNavigation(ac);
				} else {
					throw error(FORBIDDEN, "error_missing_perm", node.getUuid());
				}
			} else {
				throw error(NOT_FOUND, "node_not_found_for_path", decodedPath);
			}
		}).subscribe(model -> ac.respond(model, OK), ac::fail);

	}
}
