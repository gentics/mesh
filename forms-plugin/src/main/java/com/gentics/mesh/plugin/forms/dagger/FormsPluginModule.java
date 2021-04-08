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
import com.gentics.mesh.rest.client.MeshRestClient;
import com.github.jknack.handlebars.io.FileTemplateLoader;

import dagger.Module;
import dagger.Provides;
import io.vertx.core.Vertx;
import io.vertx.reactivex.ext.web.templ.handlebars.HandlebarsTemplateEngine;

@Module
public class FormsPluginModule {
	/**
	 * Create the object mapper for deserialization of the form
	 * @param configuration
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
	 * Template engine
	 * @param vertx
	 * @param configuration
	 * @return template engine
	 */
	@Singleton
	@Provides
	public HandlebarsTemplateEngine templateEngine(Vertx vertx, FormsConfig configuration) {
		io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine engine = io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine.create(vertx);
		engine.getHandlebars().with(new FileTemplateLoader(new File(configuration.getTemplatesPath()).getAbsolutePath()));
		engine.getHandlebars().registerHelpers(new FormHelper());
		return new HandlebarsTemplateEngine(engine);
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
