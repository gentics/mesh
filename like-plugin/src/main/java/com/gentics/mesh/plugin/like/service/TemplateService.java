package com.gentics.mesh.plugin.like.service;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.like.config.LikePluginConfig;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.FileTemplateLoader;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.vertx.core.Vertx;

@Singleton
public class TemplateService {
	private final LikePluginConfig configuration;

	private final CachingService cachingService;

	private final Vertx vertx;

	@Inject
	public TemplateService(LikePluginConfig configuration, CachingService cachingService, Vertx vertx) {
		this.configuration = configuration;
		this.cachingService = cachingService;
		this.vertx = vertx;
	}

	public Single<String> getJavascript() {
		return cachingService.get("templates", key -> compileJs());
	}

	private Single<String> compileJs() {
		File templateDir = new File(configuration.getTemplatesPath()).getAbsoluteFile();
		FileTemplateLoader loader = new FileTemplateLoader(templateDir);
		Handlebars handlebars = new Handlebars(loader);

		return new io.vertx.reactivex.core.Vertx(vertx).fileSystem().rxReadDir(templateDir.getAbsolutePath())
			.flatMapObservable(Observable::fromIterable)
			.filter(this::isTemplate)
			.map(this.toCompiledJs(handlebars))
			.reduce((left, right) -> left + "\n\n" + right)
			.toSingle("");
	}

	/**
	 * Compiles a single template
	 * @param handlebars handlebars
	 * @return function that compiles a template
	 */
	private Function<String, String> toCompiledJs(Handlebars handlebars) {
		return path -> handlebars.compileInline(String.format("{{precompile \"%s\"}}", getName(path))).apply(null);
	}

	private String getName(String path) {
		String filename = new File(path).getName();
		return filename.substring(0, filename.lastIndexOf("."));
	}

	/**
	 * Tests if a file is a template
	 */
	private boolean isTemplate(String filename) {
		return filename.endsWith(".hbs");
	}
}
