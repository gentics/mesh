package com.gentics.mesh.core.rest.error;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * The element with the specified UUID was not found.
 *
 * <p>
 * The {@link #getElementId() elementId} is the UUID of the element.
 * </p>
 */
public class UuidNotFoundException extends AbstractUnavailableException {

	private static final long serialVersionUID = -4473062775278276226L;

	private static final String TYPE = "uuid_not_found";
	private static final String i18nKey = "object_not_found_for_uuid";

	public UuidNotFoundException(String elementType, String elementId) {
		super(HttpResponseStatus.NOT_FOUND, i18nKey, elementType, elementId);
	}

	@Override
	public String getType() {
		return TYPE;
	}
}