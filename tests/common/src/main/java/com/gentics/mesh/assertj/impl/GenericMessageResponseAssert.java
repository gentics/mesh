package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.data.i18n.I18NUtil;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;

public class GenericMessageResponseAssert extends AbstractAssert<GenericMessageResponseAssert, GenericMessageResponse> {

	public GenericMessageResponseAssert(GenericMessageResponse actual) {
		super(actual, GenericMessageResponseAssert.class);
	}

	/**
	 * Assert that the message matches up with the given key and params.
	 * 
	 * @param i18nKey
	 * @param i18nParams
	 * @return
	 */
	public GenericMessageResponseAssert matches(String i18nKey, String... i18nParams) {
		Locale en = Locale.ENGLISH;
		String message = I18NUtil.get(en, i18nKey, i18nParams);
		assertEquals("The response message does not match.", message, actual.getMessage());
		return this;
	}
}
