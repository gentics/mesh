package com.gentics.mesh.core.rest.error;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * The element with the specified ID and type is not available.
 *
 * <p>
 * What the ID exactly is and the reason must be defined in concrete subclasses.
 * </p>
 */
public abstract class AbstractUnavailableException extends AbstractRestException {

	private static final long serialVersionUID = -7489334531936926971L;

	protected final String elementType;
	protected final String elementId;

	/**
	 * Creates an exception with the given message and message parameters and without a HTTP status.
	 *
	 * <p>
	 * It is assumed that the <code>elementId</code> is the first parameter in the <code>i18nMessage</code>.
	 * </p>
	 *
	 * @param i18nMessage The internationalized message for the exception
	 * @param elementType The type of the unavailable object
	 * @param elementId The ID of the unavailable object
	 */
	public AbstractUnavailableException(String i18nMessage, String elementType, String elementId) {
		this(null, i18nMessage, elementType, elementId);
	}

	/**
	 * Creates an exception with the given HTTP status, message and message parameters.
	 *
	 * <p>
	 * It is assumed that the <code>elementId</code> is the first parameter in the <code>i18nMessage</code>.
	 * </p>
	 *
	 * @param status The HTTP status to be used in the final response
	 * @param i18nMessage The internationalized message for the exception
	 * @param elementType The type of the unavailable object
	 * @param elementId The ID of the unavailable object
	 */
	public AbstractUnavailableException(HttpResponseStatus status, String i18nMessage, String elementType, String elementId) {
		super(status, i18nMessage, elementId, elementType);

		this.elementType = elementType;
		this.elementId = elementId;
	}

	/**
	 * The type of he unavailable element.
	 *
	 * @return The type of the unavailable element
	 */
	public String getElementType() {
		return elementType;
	}

	/**
	 * The ID of the unavailable element.
	 *
	 * @return The ID of the unavailable element.
	 */
	public String getElementId() {
		return elementId;
	}
}
