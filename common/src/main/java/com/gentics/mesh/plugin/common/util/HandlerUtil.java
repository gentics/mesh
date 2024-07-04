package com.gentics.mesh.plugin.common.util;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.graphql.GraphQLError;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.common.MeshPluginException;
import com.gentics.mesh.plugin.common.handler.MimeTypes;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.google.common.net.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.function.Function;

/**
 * Miscellaneous static helper methods for plugins.
 */
public class HandlerUtil {

	private static final Logger log = LoggerFactory.getLogger(HandlerUtil.class);

	/**
	 * Returns an event handler for {@link io.reactivex.Completable Completables}
	 * that will send a <em>204 No Content</em> response, or invoke the error handler.
	 *
	 * @param rc The current routing context
	 * @param verboseErrorHandler Whether to add the error message as response body
	 * @return An event handler that will send a <code>204 No Content</code>
	 * 		response on success, and call the error handler otherwise
	 */
	public static Consumer<? super Throwable> emptyHandler(RoutingContext rc, boolean verboseErrorHandler) {
		return error -> {
			if (error == null) {
				rc.response()
					.setStatusCode(HttpResponseStatus.NO_CONTENT.code())
					.end();
			} else {
				handleError(rc, error, verboseErrorHandler);
			}
		};
	}

	/**
	 * Returns an event handler for {@link io.reactivex.Completable Completables}
	 * that will send a <em>204 No Content</em> response, or invoke the error handler.
	 *
	 * <p>
	 *     Same as calling {@code emptyHandler(rc, true)}.
	 * </p>
	 *
	 * @param rc The current routing context
	 * @return An event handler that will send a <code>204 No Content</code>
	 * 		response on success, and call the error handler otherwise
	 */
	public static Consumer<? super Throwable> emptyHandler(RoutingContext rc) {
		return emptyHandler(rc, true);
	}

	/**
	 * Convenience method for {@link #graphQlHandler(RoutingContext, Function, boolean)}
	 * which extracts the whole {@code data} object in the response.
	 *
	 * <p>
	 *     Same as calling {@code graphQlHandler(rc, JsonObject::encode, true)}.
	 * </p>
	 *
	 * @param rc The current routing context
	 * @return An event handler which will handle errors, or send the result of
	 * 		the GraphQL query
	 */
	public static BiConsumer<GraphQLResponse, ? super Throwable> graphQlHandler(RoutingContext rc) {
		return graphQlHandler(rc, JsonObject::encode, true);
	}

	/**
	 * Returns an event handler that will handle errors, or send the result
	 * of a GraphQL query.
	 *
	 * <p>
	 *     The optional {@code extractor} is used to extract a certain part of
	 *     the returned {@code data}.
	 * </p>
	 *
	 * <p>
	 *     Same as calling {@code graphQlHandler(rc, extractor, true)}.
	 * </p>
	 *
	 * @param rc The current routing context
	 * @param extractor An extractor function that encodes only relevant parts
	 * 		of the GraphQL result
	 * @return An event handler that will handle errors, or send the result
	 * 		of a GraphQL query
	 */
	public static BiConsumer<GraphQLResponse, ? super Throwable> graphQlHandler(RoutingContext rc, Function<JsonObject, String> extractor) {
		return graphQlHandler(rc, extractor, true);
	}

	/**
	 * Returns an event handler that will handle errors, or send the result
	 * of a GraphQL query.
	 *
	 * <p>
	 *     The optional {@code extractor} is used to extract a certain part of
	 *     the returned {@code data}.
	 * </p>
	 *
	 * @param rc The current routing context
	 * @param extractor An extractor function that encodes only relevant parts
	 * 		of the GraphQL result
	 * @param verboseErrorHandler Whether to add the error message as response body
	 * @return An event handler that will handle errors, or send the result
	 * 		of a GraphQL query
	 */
	public static BiConsumer<GraphQLResponse, ? super Throwable> graphQlHandler(RoutingContext rc, Function<JsonObject, String> extractor, boolean verboseErrorHandler) {
		return (rs, error) -> {
			if (rs == null) {
				handleError(rc, error, verboseErrorHandler);

				return;
			}

			String body;
			JsonObject data = rs.getData();

			if (data != null) {
				if (log.isDebugEnabled()) {
					log.debug("Received GraphQL response: {}", data.encodePrettily());
				}

				body = extractor == null ? data.encode() : extractor.apply(data);
			} else {
				List<GraphQLError> errors = rs.getErrors();

				log.error("GraphQL request contains errors: {}", JsonUtil.toJson(errors));
				body = new JsonObject().put("errors", errors).encode();
			}

			rc.response()
				.putHeader(HttpHeaders.CONTENT_TYPE, MimeTypes.APPLICATION_JSON_UTF8)
				.end(body);
		};
	}

	/**
	 * Returns an event handler that will set the response status code
	 * accordingly, send the binary data and end the response.
	 *
	 * <p>
	 *     The {@code Content-Disposition} header will be set according to
	 *     {@link ContentDispositionMode#SMART} ({@code inline} for images, and
	 *     {@code attachment} for everything else).
	 * </p>
	 *
	 * <p>
	 *     Errors are passed to {@link #handleError(RoutingContext, Throwable, boolean)}.
	 * </p>
	 *
	 * <p>
	 *     Same as calling {@code meshBinaryHandler(rc, ContentDisposition.SMART, true)}.
	 * </p>
	 *
	 * @param rc The current routing context
	 * @return An event handler that will handle any errors, or send the binary response
	 */
	public static BiConsumer<MeshBinaryResponse, ? super Throwable> meshBinaryHandler(RoutingContext rc) {
		return meshBinaryHandler(rc, ContentDispositionMode.SMART, true);
	}

	/**
	 * Returns an event handler that will set the response status code
	 * accordingly, send the binary data and end the response.
	 *
	 * <p>
	 *     The {@code Content-Disposition} header will be set according to the
	 *     {@link ContentDispositionMode dispositionMode}.
	 * </p>
	 *
	 * <p>
	 *     Errors are passed to {@link #handleError(RoutingContext, Throwable, boolean)}.
	 * </p>
	 *
	 * <p>
	 *     Same as calling {@code meshBinaryHandler(rc, dispositionMode, true)}.
	 * </p>
	 *
	 * @param rc The current routing context
	 * @param dispositionMode The content disposition mode
	 * @return An event handler that will handle any erros, or send the binary response
	 */
	public static BiConsumer<MeshBinaryResponse, ? super Throwable> meshBinaryHandler(RoutingContext rc, ContentDispositionMode dispositionMode) {
		return meshBinaryHandler(rc, dispositionMode, true);
	}

	/**
	 * Returns an event handler that will set the response status code
	 * accordingly, send the binary data and end the response.
	 *
	 * <p>
	 *     The {@code Content-Disposition} header will be set according to the
	 *     {@link ContentDispositionMode dispositionMode}.
	 * </p>
	 *
	 * <p>
	 *     Errors are passed to {@link #handleError(RoutingContext, Throwable, boolean)}.
	 * </p>
	 *
	 * @param rc The current routing context
	 * @param dispositionMode The content disposition mode
	 * @param verboseErrorHandler Whether to add the error message as response body
	 * @return An event handler that will handle any erros, or send the binary response
	 */
	public static BiConsumer<MeshBinaryResponse, ? super Throwable> meshBinaryHandler(
			RoutingContext rc,
			ContentDispositionMode dispositionMode,
			boolean verboseErrorHandler) {
		return (binaryResponse, error) -> {
			if (binaryResponse != null) {
				HttpServerResponse response = rc.response();
				String contentType = binaryResponse.getContentType();
				String contentDisposition = dispositionMode.contentDisposition(
					contentType,
					binaryResponse.getFilename());

				log.debug(
					"Received binary response from Mesh: Content-Type: {}, Content-Disposition: {}",
					contentType,
					contentDisposition);

				prepareBinaryResponse(response, HttpResponseStatus.OK.code(), contentType, contentDisposition);

				binaryResponse.getFlowable()
					.map(Buffer::buffer)
					.subscribe(response::write, errorHandler(rc, verboseErrorHandler), response::end);

				/* TODO analyse why using the Pump does not emit the onComplete event.

					.doOnSubscribe(sub -> log.info("Binary download subscribed"))
					.doOnNext(buf -> log.info("Received binary chunk with size {}", buf.length()))
					.doOnComplete(() -> log.info("Binary transfer finished, ending response"));

				io.vertx.reactivex.core.http.HttpServerResponse rxResponse =
					io.vertx.reactivex.core.http.HttpServerResponse.newInstance(response);

				Pump.pump(download, rxResponse)
					.setWriteQueueMaxSize(8192)
					.start();
				 */
			} else {
				handleError(rc, error, verboseErrorHandler);
			}
		};
	}

	/**
	 * Set response status, {@code Content-Type} and {@code Content-Disposition}
	 * for the given response.
	 *
	 * @param response The response to prepare
	 * @param status The desired response status
	 * @param contentType The content type
	 * @param contentDisposition The content disposition
	 */
	public static void prepareBinaryResponse(HttpServerResponse response, int status, String contentType, String contentDisposition) {
		response
			.setChunked(true)
			.setStatusCode(status)
			.putHeader(HttpHeaders.CONTENT_TYPE, contentType)
			.putHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);
	}

	/**
	 * Returns an event handler that will set the response status code
	 * accordingly and end the response by sending the UUID of the given
	 * node response as JSON.
	 *
	 * @param rc The current routing context.
	 * @param fieldName The name for the JSON field to contain the nodes UUID.
	 * @param verboseErrorHandler Whether to add the error message as response body
	 * @return An event handler that will handle any errors, or send the given nodes UUID as JSON
	 */
	public static BiConsumer<? super NodeResponse, ? super Throwable> nodeUuidHandler(RoutingContext rc, String fieldName, boolean verboseErrorHandler) {
		return (delegate, error)  -> {
			if (delegate != null) {
				rc.response()
					.putHeader(HttpHeaders.CONTENT_TYPE, MimeTypes.APPLICATION_JSON_UTF8)
					.end(new JsonObject().put(fieldName, delegate.getUuid()).encode());
			} else {
				handleError(rc, error, verboseErrorHandler);
			}
		};
	}

	/**
	 * Returns an event handler that will set the response status code
	 * accordingly and end the response by sending the data as JSON.
	 *
	 * @param rc The current routing context.
	 * @param verboseErrorHandler Whether to add the error message as response body
	 * @return An event handler that will handle any errors, or send the response as JSON
	 */
	public static BiConsumer<JsonObject, ? super Throwable> jsonObjectHandler(RoutingContext rc, boolean verboseErrorHandler) {
		return (delegate, error)  -> {
			if (delegate != null) {
				rc.response()
					.putHeader(HttpHeaders.CONTENT_TYPE, MimeTypes.APPLICATION_JSON_UTF8)
					.end(delegate.encode());
			} else {
				handleError(rc, error, verboseErrorHandler);
			}
		};
	}

	/**
	 * Returns an event handler that will set the response status code
	 * accordingly and end the response by sending the data as JSON.
	 *
	 * @see #handleError(RoutingContext, Throwable, boolean)
	 * @param rc The current routing context
	 * @param verboseErrorHandler Whether to add the error message as response body
	 * @return An event handler that will handle any errors, or send the response as JSON
	 */
	public static BiConsumer<? super RestModel, ? super Throwable> jsonHandler(RoutingContext rc, boolean verboseErrorHandler) {
		return (delegate, error) -> {
			if (delegate != null) {
				rc.response()
					.putHeader(HttpHeaders.CONTENT_TYPE, MimeTypes.APPLICATION_JSON_UTF8)
					.end(delegate.toJson());
			} else {
				handleError(rc, error, verboseErrorHandler);
			}
		};
	}

	/**
	 * Returns an event handler that will set the response status code
	 * accordingly and end the response by sending the data as JSON.
	 *
	 * <p>
	 *     Same as calling {@code jsonHandler(rc, true)}.
	 * </p>
	 *
	 * @see #handleError(RoutingContext, Throwable, boolean)
	 * @param rc The current routing context
	 * @return An event handler that will handle any errors, or send the response as JSON
	 */
	public static BiConsumer<? super RestModel, ? super Throwable> jsonHandler(RoutingContext rc) {
		return jsonHandler(rc, true);
	}

	/**
	 * Returns an event handler that will set the response status code
	 * accordingly and end the response by sending the data as a string.
	 *
	 * <p>
	 *     The MIME type of the response will be <code>text/plain</code>.
	 * </p>
	 *
	 * <p>
	 *     Same as calling {@code stringHandler(rc, MimeTypes.TEXT_PLAIN_UTF8, true)}.
	 * </p>
	 *
	 * @see #stringHandler(RoutingContext, String, boolean)
	 * @see #handleError(RoutingContext, Throwable, boolean)
	 * @param rc The current routing context
	 * @return An event handler that will handle any errors, or send the response as string
	 */
	public static BiConsumer<? super String, ? super Throwable> stringHandler(RoutingContext rc) {
		return stringHandler(rc, MimeTypes.TEXT_PLAIN_UTF8, true);
	}

	/**
	 * Returns an event handler that will set the response status code
	 * accordingly and end the response by sending the data as a string.
	 *
	 * <p>
	 *     Same as calling {@code stringHandler(rc, mimeType, true)}.
	 * </p>
	 *
	 * @see #handleError(RoutingContext, Throwable, boolean)
	 * @param rc The current routing context
	 * @param mimeType The MIME type of the response
	 * @return An event handler that will handle any errors, or send the response as string
	 */
	public static BiConsumer<? super String, ? super Throwable> stringHandler(RoutingContext rc, String mimeType) {
		return stringHandler(rc, mimeType, true);
	}

	/**
	 * Returns an event handler that will set the response status code
	 * accordingly and end the response by sending the data as a string.
	 *
	 * @see #handleError(RoutingContext, Throwable, boolean)
	 * @param rc The current routing context
	 * @param mimeType The MIME type of the response
	 * @param verboseErrorHandler Whether to add the error message as response body
	 * @return An event handler that will handle any errors, or send the response as string
	 */
	public static BiConsumer<? super String, ? super Throwable> stringHandler(RoutingContext rc, String mimeType, boolean verboseErrorHandler) {
		return (output, error) -> {
			if (output != null) {
				rc.response()
					.putHeader(HttpHeaders.CONTENT_TYPE, mimeType)
					.end(output);
			} else {
				handleError(rc, error, verboseErrorHandler);
			}
		};
	}

	/**
	 * Returns an error handler that will log the error, set the response status
	 * accordingly and end the response.
	 *
	 * @see #handleError(RoutingContext, Throwable, boolean)
	 * @param rc The current routing context
	 * @param verbose Whether to add the error message as response body
	 * @return An error handler that will log the error and send the response
	 */
	public static Consumer<? super Throwable> errorHandler(RoutingContext rc, boolean verbose) {
		return error -> handleError(rc, error, verbose);
	}

	/**
	 * Returns an error handler that will log the error, set the response status
	 * accordingly and end the response.
	 *
	 * <p>
	 *     Same as calling {@code errorHandler(rc, true)}.
	 * </p>
	 *
	 * @see #handleError(RoutingContext, Throwable, boolean)
	 * @param rc The current routing context
	 * @return An error handler that will log the error and send the response
	 */
	public static Consumer<? super Throwable> errorHandler(RoutingContext rc) {
		return errorHandler(rc, true);
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
	 * @param verbose Whether to add the error message to the response, when
	 * 		this is {@code false} the response will have not body, only the
	 * 		status code
	 */
	private static void handleError(RoutingContext rc, Throwable error, boolean verbose) {
		log.error("Error in plugin handler", error);

		int status;
		String message;
		HttpServerResponse response = rc.response();

		if (error instanceof MeshPluginException) {
			status = ((MeshPluginException) error).getStatusCode();
			message = verbose ? ((MeshPluginException) error).toJson() : null;
		} else {
			if (error instanceof MeshRestClientMessageException) {
				status = ((MeshRestClientMessageException) error).getStatusCode();
			} else {
				status = HttpResponseStatus.INTERNAL_SERVER_ERROR.code();
			}

			message = verbose ? new JsonObject().put("error", error.getMessage()).encode() : null;
		}

		response.setStatusCode(status);

		if (message == null) {
			response.end();
		} else {
			response
				.putHeader(HttpHeaders.CONTENT_TYPE, MimeTypes.APPLICATION_JSON_UTF8)
				.end(message);
		}
	}
}
