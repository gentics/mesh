package com.gentics.mesh.example;

import com.gentics.mesh.core.rest.lang.LanguageListResponse;
import com.gentics.mesh.core.rest.lang.LanguageResponse;

public class LanguageExamples extends AbstractExamples {

	public LanguageResponse getGermanLanguageResponse() {
		LanguageResponse response = new LanguageResponse();
		response.setLanguageTag("de");
		response.setName("German");
		response.setNativeName("Deutsch");
		response.setUuid(ExampleUuids.UUID_1);
		return response;
	}

	public LanguageResponse getJapaneseLanguageResponse() {
		LanguageResponse response = new LanguageResponse();
		response.setLanguageTag("jp");
		response.setName("Japanese");
		response.setNativeName("日本語");
		response.setUuid(ExampleUuids.UUID_2);
		return response;
	}

	public LanguageListResponse getLanguageListResponse() {
		LanguageListResponse response = new LanguageListResponse();
		response.add(getGermanLanguageResponse());
		response.add(getJapaneseLanguageResponse());
		return response;
	}
}
