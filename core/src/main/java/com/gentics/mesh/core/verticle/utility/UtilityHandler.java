package com.gentics.mesh.core.verticle.utility;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.graphdb.spi.Database;

import io.reactivex.Single;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for utility request methods.
 */
public class UtilityHandler extends AbstractHandler {

	private Database db;

	private WebRootLinkReplacer linkReplacer;

	@Inject
	public UtilityHandler(Database db, WebRootLinkReplacer linkReplacer) {
		this.db = db;
		this.linkReplacer = linkReplacer;
	}

	/**
	 * Handle a link resolve request.
	 * 
	 * @param rc
	 */
	public void handleResolveLinks(RoutingContext rc) {
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
		db.operateTx(() -> {
			String projectName = ac.getParameter("project");
			if (projectName == null) {
				projectName = "project";
			}

			return Single.just(linkReplacer.replace(null, null, ac.getBodyAsString(), ac.getNodeParameters().getResolveLinks(), projectName,
					ac.getNodeParameters().getLanguageList()));
		}).subscribe(body -> rc.response().putHeader("Content-Type", "text/plain").setStatusCode(OK.code()).end(body), ac::fail);
	}

}
