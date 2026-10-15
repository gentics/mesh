package com.gentics.mesh.rest;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;

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
		GenericMessageResponse response = new GenericMessageResponse(ac.i18n(i18nMessage, i18nParameters), i18nMessage);
		response.setProperties(IntStream.range(0, i18nParameters.length).mapToObj(i -> Pair.of(Integer.toString(i), i18nParameters[i])).collect(Collectors.toMap(Pair::getKey, Pair::getValue)));
		return response;
	}

}
