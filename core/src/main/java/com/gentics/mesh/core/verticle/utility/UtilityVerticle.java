package com.gentics.mesh.core.verticle.utility;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.POST;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.handler.InternalActionContext;

import rx.Observable;

/**
 * Verticle providing endpoints for various utilities
 */
@Component
@Scope("singleton")
@SpringVerticle
public class UtilityVerticle extends AbstractCoreApiVerticle {

	protected UtilityVerticle() {
		super("utilities");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());

		// TODO add handler for JWT

		addResolveLinkHandler();
	}

	/**
	 * Add the handler for link resolving
	 */
	private void addResolveLinkHandler() {
		route("/linkResolver").method(POST).handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);

			db.asyncNoTrxExperimental(() -> {
				String projectName = ac.getParameter("project");
				if (projectName == null) {
					projectName = "project";
				}

				return Observable.just(WebRootLinkReplacer.getInstance().replace(ac.getBodyAsString(),
						ac.getResolveLinksType(), projectName, ac.getSelectedLanguageTags()));
			}).subscribe(body -> rc.response().putHeader("Content-Type", "text/plain").setStatusCode(OK.code()).end(body));
		});
	}
}
