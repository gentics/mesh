package com.gentics.mesh.test.junit;

import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;

import io.netty.handler.codec.http.HttpResponseStatus;

public class Assert extends org.junit.Assert {

	/**
	 * Assert that the given exception contains the expected information.
	 * 
	 * @param exception
	 * @param status
	 * @param bodyMessageI18nKey
	 * @param i18nParams
	 */
	public static void assertException(HttpStatusCodeErrorException exception, HttpResponseStatus status, String bodyMessageI18nKey,
			String... i18nParams) {
		assertEquals("The http status code did not match the expected one.", status, exception.getStatus());
		assertEquals("The i18n message was not correct", bodyMessageI18nKey, exception.getMessage());
		assertArrayEquals("The i18n parameters did not match the expected values", i18nParams, exception.getI18nParameters());
	}

}
