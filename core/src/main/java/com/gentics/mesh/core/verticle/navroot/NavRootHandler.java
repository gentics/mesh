package com.gentics.mesh.core.verticle.navroot;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.verticle.handler.HandlerUtilities.operateNoTx;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.service.WebRootService;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;

public class NavRootHandler {

	private WebRootService webrootService;

	@Inject
	public NavRootHandler(WebRootService webRootService) {
		this.webrootService = webRootService;
	}

	/**
	 * Handle navigation request.
	 * 
	 * @param ac
	 * @param path
	 */
	public void handleGetPath(InternalActionContext ac, String path) {
		final String decodedPath = "/" + path;
		MeshAuthUser requestUser = ac.getUser();

		operateNoTx(() -> {
			Path nodePath = webrootService.findByProjectPath(ac, decodedPath);
			PathSegment lastSegment = nodePath.getLast();

			if (lastSegment == null) {
				throw error(NOT_FOUND, "node_not_found_for_path", decodedPath);
			}
			Node node = lastSegment.getNode();
			if (node == null) {
				throw error(NOT_FOUND, "node_not_found_for_path", decodedPath);
			}
			if (!requestUser.hasPermission(node, READ_PERM)) {
				throw error(FORBIDDEN, "error_missing_perm", node.getUuid());
			}
			return node.transformToNavigation(ac);
		}).subscribe(model -> ac.send(model, OK), ac::fail);

	}
}
