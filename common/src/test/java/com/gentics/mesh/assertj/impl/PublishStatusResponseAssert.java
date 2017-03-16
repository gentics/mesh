package com.gentics.mesh.assertj.impl;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.rest.node.PublishStatusResponse;

public class PublishStatusResponseAssert extends AbstractAssert<PublishStatusResponseAssert, PublishStatusResponse> {

	public PublishStatusResponseAssert(PublishStatusResponse actual) {
		super(actual, PublishStatusResponseAssert.class);
	}

	/**
	 * Assert that the publish status response does not contain an entry for the given language
	 * @param languageCode
	 * @return fluent API
	 */
	public PublishStatusResponseAssert doesNotContain(String languageCode) {
		assertThat(actual.getAvailableLanguages()).doesNotContainKey(languageCode);
		return this;
	}

	/**
	 * Assert that the publish status response contains an entry for the language code, that is published
	 * @param languageCode language code
	 * @return fluent API
	 */
	public PublishStatusResponseAssert isPublished(String languageCode) {
		assertThat(actual.getAvailableLanguages().get(languageCode)).as(descriptionText() + " for " + languageCode)
				.isNotNull().isPublished();
		return this;
	}

	/**
	 * Assert that the publish status response contains an entry for the language code, that is not published
	 * @param languageCode language code
	 * @return fluent API
	 */
	public PublishStatusResponseAssert isNotPublished(String languageCode) {
		assertThat(actual.getAvailableLanguages().get(languageCode)).as(descriptionText() + " for " + languageCode)
				.isNotNull().isNotPublished();
		return this;
	}

	/**
	 * Assert that the publish status repsonse contains an entry for the language code that references the given version
	 * @param languageCode language code
	 * @param version version
	 * @return fluent API
	 */
	public PublishStatusResponseAssert hasVersion(String languageCode, String version) {
		assertThat(actual.getAvailableLanguages().get(languageCode)).as(descriptionText() + " for " + languageCode)
				.isNotNull().hasVersion(version);
		return this;
	}
}
