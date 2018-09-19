package com.gentics.mesh.assertj.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.data.Language;

public class LanguageAssert extends AbstractAssert<LanguageAssert, Language> {
	public LanguageAssert(Language actual) {
		super(actual, LanguageAssert.class);
	}

	/**
	 * Assert that the language as the given name
	 * @param name name
	 * @return fluent API
	 */
	public LanguageAssert hasName(String name) {
		assertThat(actual.getName()).as(descriptionText() + " name").isEqualTo(name);
		return this;
	}

	/**
	 * Assert that the language as the given native name
	 * @param nativeName native name
	 * @return fluent API
	 */
	public LanguageAssert hasNativeName(String nativeName) {
		assertThat(actual.getNativeName()).as(descriptionText() + " native name").isEqualTo(nativeName);
		return this;
	}

	/**
	 * Assert that the language as the given tag
	 * @param tag tag
	 * @return fluent API
	 */
	public LanguageAssert hasTag(String tag) {
		assertThat(actual.getLanguageTag()).as(descriptionText() + " tag").isEqualTo(tag);
		return this;
	}
}
