package com.gentics.mesh.core.endpoint.navroot;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.util.URIUtils.decodeSegment;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.service.WebRootServiceImpl;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;
import com.gentics.mesh.path.impl.PathSegmentImpl;

import io.vertx.ext.web.RoutingContext;

public class NavRootHandler {

	private WebRootServiceImpl webrootService;
	private HandlerUtilities utils;

	@Inject
	public NavRootHandler(WebRootServiceImpl webRootService, HandlerUtilities utils) {
		this.webrootService = webRootService;
		this.utils = utils;
	}

	/**
	 * Handle navigation request.
	 * 
	 * @param rc
	 */
	public void handleGetPath(RoutingContext rc) {
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
		String path = rc.request().path().substring(
			rc.mountPoint().length());
		HibUser requestUser = ac.getUser();

		utils.syncTx(ac, tx -> {
			ContainerType type = ContainerType.forVersion(ac.getVersioningParameters().getVersion());
			Path nodePath = webrootService.findByProjectPath(ac, path, type);
			PathSegment lastSegment = nodePath.getLast();
			UserDaoWrapper userDao = tx.userDao();
			NodeDaoWrapper nodeDao = tx.nodeDao();

			if (lastSegment == null) {
				throw error(NOT_FOUND, "node_not_found_for_path", decodeSegment(path));
			}
			PathSegmentImpl graphSegment = (PathSegmentImpl) lastSegment;
			NodeGraphFieldContainer container = graphSegment.getContainer();
			if (container == null) {
				throw error(NOT_FOUND, "node_not_found_for_path", decodeSegment(path));
			}
			HibNode node = tx.contentDao().getNode(container);
			if (!userDao.hasPermission(requestUser, node, READ_PUBLISHED_PERM)) {
				throw error(FORBIDDEN, "error_missing_perm", node.getUuid(), READ_PUBLISHED_PERM.getRestPerm().getName());
			}
			return nodeDao.transformToNavigation(node, ac);
		}, model -> ac.send(model, OK));

	}
}
