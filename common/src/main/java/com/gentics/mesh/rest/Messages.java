package com.gentics.mesh.rest;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.handler.ActionContext;

/**
 * REST helper to generate new translated {@link GenericMessageResponse} messages.
 */
public final class Messages {

	/**
	 * Generate a new message response.
	 * 
	 * @param ac
	 * @param i18nMessage
	 * @param i18nParameters
	 * @return
	 */
	public static GenericMessageResponse message(ActionContext ac, String i18nMessage, String... i18nParameters) {
		return new GenericMessageResponse(ac.i18n(i18nMessage, i18nParameters));
	}

}
