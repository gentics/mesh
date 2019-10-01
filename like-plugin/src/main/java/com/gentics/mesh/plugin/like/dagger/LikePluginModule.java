package com.gentics.mesh.plugin.like.dagger;

import javax.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import dagger.Module;
import dagger.Provides;

@Module
public class LikePluginModule {
	/**
	 * Create the object mapper
	 */
	@Singleton
	@Provides
	public ObjectMapper provideObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return objectMapper;
	}
}
