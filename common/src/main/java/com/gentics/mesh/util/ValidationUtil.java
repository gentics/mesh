package com.gentics.mesh.util;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * Utility for validating query parameters.
 */
public final class ValidationUtil {

	private ValidationUtil() {
	}

	public static void validate(PagingParameters pagingInfo) {
		if (pagingInfo.getPage() < 1) {
			throw new GenericRestException(BAD_REQUEST, "error_page_parameter_must_be_positive", String.valueOf(pagingInfo.getPage()));
		}
		if (pagingInfo.getPerPage() != null && pagingInfo.getPerPage() < 0) {
			throw new GenericRestException(BAD_REQUEST, "error_pagesize_parameter", String.valueOf(pagingInfo.getPerPage()));
		}
	}
}
