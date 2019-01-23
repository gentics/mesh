package com.gentics.mesh.core.endpoint.navroot;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.util.URIUtils.decodeSegment;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.service.WebRootServiceImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;
import io.vertx.ext.web.RoutingContext;

public class NavRootHandler {

	private WebRootServiceImpl webrootService;
	private Database db;


	@Inject
	public NavRootHandler(WebRootServiceImpl webRootService, Database db) {
		this.webrootService = webRootService;
		this.db = db;
	}

	/**
	 * Handle navigation request.
	 * 
	 * @param rc
	 */
	public void handleGetPath(RoutingContext rc) {
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
		String path = rc.request().path().substring(
			rc.mountPoint().length()
		);
		MeshAuthUser requestUser = ac.getUser();

		db.asyncTx(() -> {
			Path nodePath = webrootService.findByProjectPath(ac, path);
			PathSegment lastSegment = nodePath.getLast();

			if (lastSegment == null) {
				throw error(NOT_FOUND, "node_not_found_for_path", decodeSegment(path));
			}
			NodeGraphFieldContainer container = lastSegment.getContainer();
			if (container == null) {
				throw error(NOT_FOUND, "node_not_found_for_path", decodeSegment(path));
			}
			Node node = container.getParentNode();
			if (!requestUser.hasPermission(node, READ_PERM)) {
				throw error(FORBIDDEN, "error_missing_perm", node.getUuid(), READ_PERM.getRestPerm().getName());
			}
			return node.transformToNavigation(ac);
		}).subscribe(model -> ac.send(model, OK), ac::fail);

	}
}
