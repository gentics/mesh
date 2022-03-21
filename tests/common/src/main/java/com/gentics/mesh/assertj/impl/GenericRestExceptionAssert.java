package com.gentics.mesh.assertj.impl;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.junit.Assert.assertEquals;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.rest.error.GenericRestException;

public class GenericRestExceptionAssert extends AbstractAssert<GenericRestExceptionAssert, GenericRestException> {

	public GenericRestExceptionAssert(GenericRestException actual) {
		super(actual, GenericRestExceptionAssert.class);
	}

	public GenericRestExceptionAssert matches(String i18nMessageKey, String... i18nProperties) {
		assertEquals("The message key of the exception did not match up", i18nMessageKey, actual.getI18nKey());
		assertThat(actual.getI18nParameters()).as("I18n properties").containsExactly(i18nProperties);
		return this;
	}

}
