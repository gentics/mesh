package com.gentics.mesh.plugin.forms.dagger;

import java.io.File;

import javax.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.common.dagger.StorageDir;
import com.gentics.mesh.plugin.common.service.MeshPluginService;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.helper.FormHelper;
import com.gentics.mesh.plugin.forms.model.FormElement;
import com.gentics.mesh.plugin.forms.model.FormElementDeserializer;
import com.gentics.mesh.plugin.common.service.RenderService;
import com.gentics.mesh.rest.client.MeshRestClient;

import com.github.jknack.handlebars.Handlebars;
import dagger.Module;
import dagger.Provides;
import io.vertx.core.Vertx;
import io.vertx.reactivex.ext.web.templ.handlebars.HandlebarsTemplateEngine;

@Module
public class FormsPluginModule {
	/**
	 * Create the object mapper for deserialization of the form
	 * @param configuration The plugin configuration
	 * @return object mapper
	 */
	@Singleton
	@Provides
	public ObjectMapper provideObjectMapper(FormsConfig configuration) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		SimpleModule module = new SimpleModule();
		module.addDeserializer(FormElement.class, new FormElementDeserializer<>(configuration));
		objectMapper.registerModule(module);
		return objectMapper;
	}


	/**
	 * Render service
	 *
	 * @param vertx The Vert.x instance
	 * @param config The plugin configuration
	 * @return The render service
	 */
	@Singleton
	@Provides
	public RenderService renderService(Vertx vertx, FormsConfig config) {
		io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine delegateEngine = io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine.create(vertx);

		Handlebars handlebars = delegateEngine.getHandlebars();

		handlebars.registerHelpers(new FormHelper());
		handlebars.setInfiniteLoops(true);

		return new RenderService(config.getTemplatesPath(), new HandlebarsTemplateEngine(delegateEngine));
	}

	@Singleton
	@Provides
	public io.vertx.reactivex.core.Vertx vertx(Vertx vertx) {
		return new io.vertx.reactivex.core.Vertx(vertx);
	}

	@Singleton
	@Provides
	public MeshPluginService meshPluginService(io.vertx.reactivex.core.Vertx vertx, @AdminClient MeshRestClient adminClient, @StorageDir File storageDir) {
		return new MeshPluginService(vertx, adminClient, storageDir);
	}
}
