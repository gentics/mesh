package com.gentics.mesh.plugin.formgenerator.dagger;

import java.io.File;

import javax.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.gentics.mesh.plugin.formgenerator.config.FormGeneratorConfig;
import com.gentics.mesh.plugin.formgenerator.helper.FormHelper;
import com.gentics.mesh.plugin.formgenerator.model.FormElement;
import com.gentics.mesh.plugin.formgenerator.model.FormElementDeserializer;
import com.github.jknack.handlebars.io.FileTemplateLoader;

import dagger.Module;
import dagger.Provides;
import io.vertx.core.Vertx;
import io.vertx.reactivex.ext.web.templ.handlebars.HandlebarsTemplateEngine;

@Module
public class FormGeneratorPluginModule {
	/**
	 * Create the object mapper for deserialization of the form
	 * @param configuration
	 * @return object mapper
	 */
	@Singleton
	@Provides
	public ObjectMapper provideObjectMapper(FormGeneratorConfig configuration) {
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
	public HandlebarsTemplateEngine templateEngine(Vertx vertx, FormGeneratorConfig configuration) {
		io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine engine = io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine.create(vertx);
		engine.getHandlebars().with(new FileTemplateLoader(new File(configuration.getTemplatesPath()).getAbsolutePath()));
		engine.getHandlebars().registerHelpers(new FormHelper());
		return new HandlebarsTemplateEngine(engine);
	}
}
