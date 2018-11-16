package com.gentics.mesh.router.route;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON_UTF8;

import java.util.MissingResourceException;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.i18n.I18NUtil;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.AbstractRestException;
import com.gentics.mesh.core.rest.error.NotModifiedException;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.json.MeshJsonException;

import io.vertx.core.Handler;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

/**
 * Central failure handler for REST API routes.
 */
public class FailureHandler implements Handler<RoutingContext> {

	private static final Logger log = LoggerFactory.getLogger(FailureHandler.class);

	/**
	 * Create a new failure handler.
	 * 
	 * @return
	 */
	public static Handler<RoutingContext> create() {
		return new FailureHandler();
	}

	/**
	 * Return the response status that may be stored within the exception.
	 * 
	 * @param failure
	 * @param code
	 * @return
	 */
	private int getResponseStatusCode(Throwable failure, int code) {
		if (failure instanceof AbstractRestException) {
			AbstractRestException error = (AbstractRestException) failure;
			return error.getStatus().code();
		}
		return code;
	}

	@Override
	public void handle(RoutingContext rc) {
		if (rc.response().closed() && rc.failed()) {
			log.error("Error in request for path {" + rc.request().method().name() + " " + rc.request().path() + "}", rc.failure());
			return;
		}

		// Handle "callback route is not configured" error from OAuthHandler
		if (rc.failed()) {
			Throwable error = rc.failure();
			if (error instanceof NoStackTraceThrowable) {
				NoStackTraceThrowable s = (NoStackTraceThrowable) error;
				String msg = s.getMessage();
				if ("callback route is not configured.".equalsIgnoreCase(msg)) {
					// Suppress the error and use 401 instead
					rc.response().setStatusCode(401).end();
					return;
				}
			}
		}

		if (rc.statusCode() == 404) {
			rc.next();
			return;
		}
		if (rc.statusCode() == 401) {
			// Assume that it has been handled by the BasicAuthHandlerImpl
			if (log.isDebugEnabled()) {
				log.debug("Got failure with 401 code.");
			}
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String msg = I18NUtil.get(ac, "error_not_authorized");
			rc.response().setStatusCode(401).end(new GenericMessageResponse(msg).toJson());
			return;
		} else {
			Throwable failure = rc.failure();

			// TODO instead of unwrapping we should return all the exceptions we can and use ExceptionResponse to nest those exceptions
			// Unwrap wrapped exceptions
			while (failure != null && failure.getCause() != null) {
				if (failure instanceof AbstractRestException || failure instanceof JsonMappingException) {
					break;
				}
				failure = failure.getCause();
			}

			if (failure != null && failure instanceof NotModifiedException) {
				rc.response().setStatusCode(304);
				rc.response().end();
				return;
			}

			int code = getResponseStatusCode(failure, rc.statusCode());
			String failureMsg = failure != null ? failure.getMessage() : "-";
			switch (code) {
			case 401:
				log.error("Unauthorized - Request for path {" + toPath(rc) + "} was not authorized.");
				break;
			case 404:
				log.error("Could not find resource for path {" + toPath(rc) + "}");
				break;
			case 403:
				log.error("Request for request in path: " + toPath(rc) + " is not authorized.");
				break;
			case 400:
				log.error("Bad request in path: " + toPath(rc) + " with message " + failureMsg);
				break;
			case 413:
				log.error("Entity too large to be processed for path: " + toPath(rc));
				rc.next();
				return;
			default:
				log.error("Error for request in path: " + toPath(rc));
				if (failure != null) {
					log.error("Error:", failure);
				}
			}

			// TODO wrap this instead into a try/catch and throw the failure
			rc.response().putHeader("Content-Type", APPLICATION_JSON_UTF8);
			if (failure != null && ((failure.getCause() instanceof MeshJsonException) || failure instanceof MeshSchemaException)) {
				rc.response().setStatusCode(400);
				InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
				String msg = I18NUtil.get(ac, "error_parse_request_json_error");
				rc.response().end(new GenericMessageResponse(msg, failure.getMessage()).toJson());
			}
			if (failure != null && failure instanceof AbstractRestException) {
				AbstractRestException error = (AbstractRestException) failure;
				rc.response().setStatusCode(code);
				translateMessage(error, rc);
				rc.response().end(JsonUtil.toJson(error));
			} else if (failure != null) {
				rc.response().setStatusCode(code);
				rc.response().end(JsonUtil.toJson(new GenericMessageResponse(failure.getMessage())));
			} else {
				InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
				String msg = I18NUtil.get(ac, "error_internal");
				rc.response().setStatusCode(500);
				rc.response().end(JsonUtil.toJson(new GenericMessageResponse(msg)));
			}
		}

	}

	private String toPath(RoutingContext rc) {
		StringBuilder b = new StringBuilder();
		b.append(rc.normalisedPath());
		String query = rc.request().query();
		if (query != null) {
			b.append("?" + rc.request().query());
		}
		return b.toString();
	}

	/**
	 * Try to translate the nested i18n message key.
	 * 
	 * @param error
	 * @param rc
	 */
	private static void translateMessage(AbstractRestException error, RoutingContext rc) {
		String i18nMsg = error.getI18nKey();
		try {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			i18nMsg = I18NUtil.get(ac, error.getI18nKey(), error.getI18nParameters());
			error.setTranslatedMessage(i18nMsg);
		} catch (MissingResourceException e) {
			log.error("Did not find i18n message for key {" + error.getMessage() + "}", e);
		}
	}

}
