package com.gentics.mesh.core.verticle.utility;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.impl.NodeParameters;

import io.vertx.ext.web.RoutingContext;
import rx.Single;

public class UtilityHandler extends AbstractHandler {

	public UtilityHandler(Database db, MeshSpringConfiguration springConfiguration, BootstrapInitializer boot, RouterStorage routerStorage) {
		super(db, springConfiguration, boot, routerStorage);
	}

	/**
	 * Handle a link resolve request.
	 * 
	 * @param rc
	 */
	public void handleResolveLinks(RoutingContext rc) {
		InternalActionContext ac = InternalActionContext.create(rc);
		db.asyncNoTx(() -> {
			String projectName = ac.getParameter("project");
			if (projectName == null) {
				projectName = "project";
			}

			return Single.just(WebRootLinkReplacer.getInstance().replace(null, null, ac.getBodyAsString(), ac.getNodeParameters().getResolveLinks(),
					projectName, new NodeParameters(ac).getLanguageList()));
		}).subscribe(body -> rc.response().putHeader("Content-Type", "text/plain").setStatusCode(OK.code()).end(body), ac::fail);
	}

}
