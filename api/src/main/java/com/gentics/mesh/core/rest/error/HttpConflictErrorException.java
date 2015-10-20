package com.gentics.mesh.core.rest.error;

import com.gentics.mesh.handler.ActionContext;

import io.netty.handler.codec.http.HttpResponseStatus;

public class HttpConflictErrorException extends HttpStatusCodeErrorException {

	private static final long serialVersionUID = -3129778202887838064L;

	public static HttpConflictErrorException conflict(ActionContext ac, String conflictingUuid, String conflictingName, String i18nMessageKey,
			String... parameters) {
		return new HttpConflictErrorException(ac.i18n(i18nMessageKey, parameters), conflictingUuid, conflictingName);
	}

	public HttpConflictErrorException(String message, String conflictingUuid, String conflictingName) {
		super(message);
		this.code = HttpResponseStatus.CONFLICT.code();
		this.properties.put("conflictingUuid", conflictingUuid);
		this.properties.put("conflictingName", conflictingName);
	}
}
