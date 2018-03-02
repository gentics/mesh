package com.gentics.mesh.router.route;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON_UTF8;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler which will be invoked when no other route matches. This way this handler will return a informative JSON error (404).
 */
public class DefaultNotFoundHandler implements Handler<RoutingContext> {

	public static DefaultNotFoundHandler create() {
		return new DefaultNotFoundHandler();
	}

	@Override
	public void handle(RoutingContext rc) {
		GenericMessageResponse msg = new GenericMessageResponse();
		String internalMessage = "The rest endpoint or resource for given path {" + rc.normalisedPath() + "} could not be found.";
		String contentType = rc.request().getHeader("Content-Type");
		if (contentType == null) {
			switch (rc.request().method()) {
			case PUT:
			case POST:
				internalMessage += " You tried to POST or PUT data but you did not specifiy any Content-Type within your request.";
				break;
			default:
				break;
			}
		}
		String acceptHeaderValue = rc.request().getHeader(HttpHeaders.ACCEPT);
		if (acceptHeaderValue == null) {
			internalMessage += " You did not set any accept header. Please make sure to add {" + APPLICATION_JSON + "} to your accept header.";
		}

		if (acceptHeaderValue != null) {
			// TODO validate it and send a msg if the accept header is wrong.
			internalMessage += " Please verify that your Accept header is set correctly. I got {" + acceptHeaderValue + "}. It must accept {"
					+ APPLICATION_JSON + "}";
		}

		msg.setInternalMessage(internalMessage);
		msg.setMessage("Not Found");
		rc.response().putHeader("Content-Type", APPLICATION_JSON_UTF8);
		rc.response().setStatusCode(404);
		rc.response().setStatusMessage("Not Found");
		rc.response().end(msg.toJson());

	}

}
