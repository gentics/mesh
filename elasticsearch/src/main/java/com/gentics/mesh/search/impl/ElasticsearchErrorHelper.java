package com.gentics.mesh.search.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.mesh.core.rest.error.GenericRestException;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public final class ElasticsearchErrorHelper {

	private static final Logger log = LoggerFactory.getLogger(ElasticsearchErrorHelper.class);

	/**
	 * Check whether the found error is an version conflict error.
	 * 
	 * @param error
	 * @return
	 */
	public static boolean isConflictError(Throwable error) {
		if (error instanceof HttpErrorException) {
			HttpErrorException he = (HttpErrorException) error;
			JsonObject json = he.getBodyObject(JsonObject::new);
			JsonObject errorObj = json.getJsonObject("error");
			if (errorObj == null) {
				json.encodePrettily();
				return false;
			}
			String errorType = errorObj.getString("type");
			return "version_conflict_engine_exception".equals(errorType);
		}
		return false;
	}

	public static boolean isNotFoundError(Throwable error) {
		if (error instanceof HttpErrorException) {
			HttpErrorException se = (HttpErrorException) error;
			JsonObject errorResp = se.getBodyObject(JsonObject::new);
			// boolean isNotFound = "not_found".equals(errorResp.getString("result"));
			if (se.getStatusCode() == 404) {
				return true;
			}
		}
		return false;
	}

	public static boolean isResourceAlreadyExistsError(Throwable error) {
		if (error instanceof HttpErrorException) {
			HttpErrorException re = (HttpErrorException) error;
			JsonObject ob = re.getBodyObject(JsonObject::new);
			String type = ob.getJsonObject("error").getString("type");
			if (type.equals("resource_already_exists_exception")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check whether the given error is a "timeout error" sent from elasticsearch
	 * @param error error
	 * @return true for timeout error
	 */
	public static boolean isTimeoutError(Throwable error) {
		if (error instanceof HttpErrorException) {
			HttpErrorException he = (HttpErrorException) error;
			JsonObject json = he.getBodyObject(JsonObject::new);
			JsonObject errorObj = json.getJsonObject("error");
			if (errorObj == null) {
				json.encodePrettily();
				return false;
			}
			String errorType = errorObj.getString("type");
			return "timeout_exception".equals(errorType);
		} else {
			return false;
		}
	}

	/**
	 * Extract the error from the throwable and return a user friendly error.
	 * 
	 * @param error
	 * @return
	 */
	public static GenericRestException mapToMeshError(Throwable error) {
		if (error instanceof GenericRestException) {
			return (GenericRestException) error;
		}
		if (error instanceof TimeoutException || error instanceof SocketTimeoutException) {
			return error(INTERNAL_SERVER_ERROR, "search_error_timeout");
		} else if (error instanceof HttpErrorException) {
			HttpErrorException he = (HttpErrorException) error;
			JsonObject errorResponse = he.getBodyObject(JsonObject::new);
			if (log.isDebugEnabled()) {
				log.debug("Got response {" + errorResponse.encodePrettily() + "}");
			}
			JsonObject errorInfo = errorResponse.getJsonObject("error");
			if (errorInfo != null) {
				return mapError(errorInfo);
			}
		}
		return error(INTERNAL_SERVER_ERROR, "search_error", error);
	}

	/**
	 * Transform the Elasticsearch error into a mesh error exception.
	 * 
	 * @param errorInfo
	 * @return
	 */
	public static GenericRestException mapError(JsonObject errorInfo) {
		String reason = errorInfo.getString("reason");
		GenericRestException restError = error(BAD_REQUEST, "search_error_query", reason);
		JsonArray causes = errorInfo.getJsonArray("root_cause");
		if (causes != null) {
			for (int i = 0; i < causes.size(); i++) {
				JsonObject cause = causes.getJsonObject(i);
				restError.getProperties().put("cause-" + i, cause.getString("reason"));
			}
		}
		return restError;
	}

}
