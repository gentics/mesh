package com.gentics.mesh.etc;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON_UTF8;

import java.util.MissingResourceException;

import com.gentics.mesh.core.data.service.I18NUtil;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.handler.HttpActionContext;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.json.MeshJsonException;

import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

/**
 * Central failure handler for REST API routes.
 */
public class FailureHandler implements Handler<RoutingContext> {

	private static final Logger log = LoggerFactory.getLogger(FailureHandler.class);

	public static Handler<RoutingContext> create() {
		return new FailureHandler();
	}

	private int getResponseStatusCode(Throwable failure) {
		if (failure instanceof HttpStatusCodeErrorException) {
			HttpStatusCodeErrorException error = (HttpStatusCodeErrorException) failure;
			return error.getStatus().code();
		}
		return 500;
	}

	@Override
	public void handle(RoutingContext rc) {
		if (rc.statusCode() == 401) {
			// Assume that it has been handled by the BasicAuthHandlerImpl
			if (log.isDebugEnabled()) {
				log.debug("Got failure with 401 code.");
			}
			rc.next();
		} else {
			log.error("Error for request in path: " + rc.normalisedPath());
			Throwable failure = rc.failure();
			if (failure != null) {
				log.error("Error:", failure);
			}
			// Unwrap wrapped exceptions
			while (failure != null && failure.getCause() != null) {
				if (failure instanceof HttpStatusCodeErrorException) {
					break;
				}
				failure = failure.getCause();
			}

			// TODO wrap this instead into a try/catch and throw the failure
			rc.response().putHeader("Content-Type", APPLICATION_JSON_UTF8);
			if (failure != null && ((failure.getCause() instanceof MeshJsonException) || failure instanceof MeshSchemaException)) {
				rc.response().setStatusCode(400);
				String msg = I18NUtil.get(HttpActionContext.create(rc), "error_parse_request_json_error");
				rc.response().end(JsonUtil.toJson(new GenericMessageResponse(msg, failure.getMessage())));
			} else if (failure != null && failure instanceof HttpStatusCodeErrorException) {
				HttpStatusCodeErrorException httpStatusError = (HttpStatusCodeErrorException) failure;
				rc.response().setStatusCode(httpStatusError.getStatus().code());

				String i18nMsg = httpStatusError.getMessage();
				try {
					i18nMsg = I18NUtil.get(HttpActionContext.create(rc), httpStatusError.getMessage(), httpStatusError.getI18nParameters());
				} catch (MissingResourceException e) {
					log.error("Did not find i18n message for key {" + httpStatusError.getMessage() + "}", e);
				}

				GenericMessageResponse msg = new GenericMessageResponse(i18nMsg, null, httpStatusError.getProperties());
				rc.response().end(JsonUtil.toJson(msg));
			} else if (failure != null) {
				int code = getResponseStatusCode(failure);
				rc.response().setStatusCode(code);
				rc.response().end(JsonUtil.toJson(new GenericMessageResponse(failure.getMessage())));
			} else {
				String msg = I18NUtil.get(HttpActionContext.create(rc), "error_internal");
				rc.response().setStatusCode(500);
				rc.response().end(JsonUtil.toJson(new GenericMessageResponse(msg)));
			}
		}

	}

}
