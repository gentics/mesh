package com.gentics.mesh.plugin.common.util;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.plugin.common.MeshPluginException;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

import javax.ws.rs.core.MediaType;
import java.nio.charset.StandardCharsets;

/**
 * Miscellaneous static helper methods for plugins.
 */
public class HandlerUtil {

	private static final String MIME_TYPE_JSON = MediaType.APPLICATION_JSON_TYPE
		.withCharset(StandardCharsets.UTF_8.name())
		.toString();
	private static final String MIME_TYPE_PLAIN = MediaType.TEXT_PLAIN_TYPE
		.withCharset(StandardCharsets.UTF_8.name())
		.toString();

	private static final Logger log = LoggerFactory.getLogger(HandlerUtil.class);

	/**
	 * Returns an event handler for {@link io.reactivex.Completable Completables}
	 * that will send a <em>204 No Content</em> response, or invoke the error.
	 *
	 * @param rc The current routing context
	 * @return An event handler that will send a <code>204 No Content</code>
	 * 		response on success, and call the error handler otherwise
	 */
	public static Consumer<? super Throwable> emptyHandler(RoutingContext rc) {
		return error -> {
			if (error == null) {
				rc.response()
					.setStatusCode(HttpResponseStatus.NO_CONTENT.code())
					.end();
			} else {
				handleError(rc, error);
			}
		};
	}

	/**
	 * Returns an event handler that will set the response status code
	 * accordingly and end the response by sending the data as JSON.
	 *
	 * @see #handleError(RoutingContext, Throwable)
	 * @param rc The current routing context
	 * @return An event handler that will handle any errors, or send the response as JSON
	 */
	public static BiConsumer<? super RestModel, ? super Throwable> jsonHandler(RoutingContext rc) {
		return (delegate, error) -> {
			if (delegate != null) {
				rc.response()
					.putHeader(HttpHeaders.CONTENT_TYPE, MIME_TYPE_JSON)
					.end(delegate.toJson());
			} else {
				handleError(rc, error);
			}
		};
	}

	/**
	 * Returns an event handler that will set the response status code
	 * accordingly and end the response by sending the data as a string.
	 *
	 * <p>
	 *     The MIME type of the response will be <code>text/plain</code>.
	 * </p>
	 *
	 * @see #stringHandler(RoutingContext, String)
	 * @see #handleError(RoutingContext, Throwable)
	 * @param rc The current routing context
	 * @return An event handler that will handle any errors, or send the response as string
	 */
	public static BiConsumer<? super String, ? super Throwable> stringHandler(RoutingContext rc) {
		return stringHandler(rc, MIME_TYPE_PLAIN);
	}

	/**
	 * Returns an event handler that will set the response status code
	 * accordingly and end the response by sending the data as a string.
	 *
	 * @see #handleError(RoutingContext, Throwable)
	 * @param rc The current routing context
	 * @param mimeType The MIME type of the response
	 * @return An event handler that will handle any errors, or send the response as string
	 */
	public static BiConsumer<? super String, ? super Throwable> stringHandler(RoutingContext rc, String mimeType) {
		return (output, error) -> {
			if (output != null) {
				rc.response()
					.putHeader(HttpHeaders.CONTENT_TYPE, mimeType)
					.end(output);
			} else {
				handleError(rc, error);
			}
		};
	}

	/**
	 * Logs the specified error, sets the status code accordingly and ends
	 * the response.
	 *
	 * <p>
	 *     When <code>error</code> is a {@link MeshRestClientMessageException}
	 *     the response status code will be taken from the <code>error</code>,
	 *     otherwise it will be a generic <em>500 Internal Server Error</em>.
	 * </p>
	 *
	 * <p>
	 *     The error message will be send in the response.
	 * </p>
	 *
	 * @param rc The current routing context
	 * @param error The error to handle
	 */
	private static void handleError(RoutingContext rc, Throwable error) {
		if (log.isTraceEnabled()) {
			log.error("Error in plugin handler: {}", error, error.getMessage());
		} else {
			log.error("Error in plugin handler: {}", error.getMessage());
		}

		int status;
		String message;
		HttpServerResponse response = rc.response();

		if (error instanceof MeshPluginException) {
			status = ((MeshPluginException) error).getStatusCode();
			message = ((MeshPluginException) error).toJson();
		} else {
			if (error instanceof MeshRestClientMessageException) {
				status = ((MeshRestClientMessageException) error).getStatusCode();
			} else {
				status = HttpResponseStatus.INTERNAL_SERVER_ERROR.code();
			}

			message = new JsonObject()
				.put("error", error.getMessage())
				.encode();
		}

		response
			.putHeader(HttpHeaders.CONTENT_TYPE, MIME_TYPE_JSON)
			.setStatusCode(status)
			.end(message);
	}
}
