package com.gentics.mesh.core.verticle.utility;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;

import io.vertx.ext.web.RoutingContext;
import rx.Observable;

@Component
public class UtilityHandler extends AbstractHandler {

	/**
	 * Handle a link resolve request.
	 * 
	 * @param rc
	 */
	public void handleResolveLinks(RoutingContext rc) {
		InternalActionContext ac = InternalActionContext.create(rc);
		db.asyncNoTrxExperimental(() -> {
			String projectName = ac.getParameter("project");
			if (projectName == null) {
				projectName = "project";
			}

			return Observable.just(WebRootLinkReplacer.getInstance().replace(ac.getBodyAsString(), ac.getResolveLinksType(), projectName,
					ac.getSelectedLanguageTags()));
		}).subscribe(body -> rc.response().putHeader("Content-Type", "text/plain").setStatusCode(OK.code()).end(body));
	}

}
