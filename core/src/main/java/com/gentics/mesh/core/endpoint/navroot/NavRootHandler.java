package com.gentics.mesh.core.endpoint.navroot;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.service.WebRootServiceImpl;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;

public class NavRootHandler {

	private WebRootServiceImpl webrootService;
	private LegacyDatabase db;


	@Inject
	public NavRootHandler(WebRootServiceImpl webRootService, LegacyDatabase db) {
		this.webrootService = webRootService;
		this.db = db;
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

		db.asyncTx(() -> {
			Path nodePath = webrootService.findByProjectPath(ac, decodedPath);
			PathSegment lastSegment = nodePath.getLast();

			if (lastSegment == null) {
				throw error(NOT_FOUND, "node_not_found_for_path", decodedPath);
			}
			NodeGraphFieldContainer container = lastSegment.getContainer();
			if (container == null) {
				throw error(NOT_FOUND, "node_not_found_for_path", decodedPath);
			}
			Node node = container.getParentNode();
			if (!requestUser.hasPermission(node, READ_PERM)) {
				throw error(FORBIDDEN, "error_missing_perm", node.getUuid(), READ_PERM.getRestPerm().getName());
			}
			return node.transformToNavigation(ac);
		}).subscribe(model -> ac.send(model, OK), ac::fail);

	}
}
