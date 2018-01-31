package com.gentics.mesh.search.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.concurrent.TimeoutException;

import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.mesh.core.rest.error.GenericRestException;

import io.vertx.core.json.JsonObject;

public final class ElasticsearchErrorHelper {

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
			String errorType = json.getJsonObject("error").getString("type");
			return "version_conflict_engine_exception".equals(errorType);
		}
		return false;
	}

	public static boolean isNotFoundError(Throwable error) {
		if (error instanceof HttpErrorException) {
			HttpErrorException se = (HttpErrorException) error;
			JsonObject errorResp = se.getBodyObject(JsonObject::new);
			if (se.getStatusCode() == 404 && "not_found".equals(errorResp.getString("result"))) {
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

	public static GenericRestException mapToMeshError(Throwable error) {
		if (error instanceof TimeoutException) {
			return error(INTERNAL_SERVER_ERROR, "search_error_timeout");
		} else if (error instanceof HttpErrorException) {
			HttpErrorException he = (HttpErrorException) error;
			JsonObject errorResponse = he.getBodyObject(JsonObject::new);
			JsonObject errorInfo = errorResponse.getJsonObject("error");
			if (errorInfo != null) {
				String reason = errorInfo.getString("reason");
				// TODO use specific error for parsing errors?
				return error(BAD_REQUEST, "search_error_query", reason);
			}
		}
		return error(INTERNAL_SERVER_ERROR, "search_error", error);
	}

}
