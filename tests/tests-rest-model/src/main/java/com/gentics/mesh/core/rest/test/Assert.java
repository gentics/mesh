package com.gentics.mesh.core.rest.test;

import com.gentics.mesh.core.rest.error.GenericRestException;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Assertion class which extends the {@link com.gentics.mesh.test.Assert} class which additional rest specific asserters.
 */
public class Assert extends com.gentics.mesh.test.Assert {

	/**
	 * Assert that the given exception contains the expected information.
	 * 
	 * @param exception
	 * @param status
	 * @param bodyMessageI18nKey
	 * @param i18nParams
	 */
	public static void assertException(GenericRestException exception, HttpResponseStatus status, String bodyMessageI18nKey, String... i18nParams) {
		assertEquals("The http status code did not match the expected one.", status, exception.getStatus());
		assertEquals("The i18n message was not correct", bodyMessageI18nKey, exception.getI18nKey());
		assertArrayEquals("The i18n parameters did not match the expected values", i18nParams, exception.getI18nParameters());
	}
}
