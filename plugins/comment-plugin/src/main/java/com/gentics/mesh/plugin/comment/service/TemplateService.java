package com.gentics.mesh.plugin.comment.service;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.comment.config.Configuration;
import com.gentics.mesh.plugin.comment.util.RxUtil;
import com.gentics.mesh.plugin.common.dagger.StorageDir;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.FileTemplateLoader;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;

@Singleton
public class TemplateService {
	public static final String TEMPLATE_DIR_NAME = "templates";

	private final Configuration configuration;
	private final File storageDir;
	private final Vertx vertx;

	private String cachedJs; // TODO Consider saving JS to a file instead of keeping it in RAM

	@Inject
	public TemplateService(Configuration configuration, @StorageDir File storageDir, Vertx vertx) {
		this.storageDir = storageDir;
		this.vertx = vertx;
		this.configuration = configuration;
	}

	/**
	 * Gets the compiled javascript templates
	 */
	public Single<String> getJavascript() {
		if (cachedJs != null) {
			return Single.just(cachedJs);
		} else {
			return compileJs();
		}
	}

	/**
	 * Precompiles the configured templates.
	 */
	public Completable initialize() {
		if (!configuration.isCacheTemplates()) {
			return Completable.complete();
		} else {
			return compileJs()
				.doOnSuccess(js -> this.cachedJs = js)
				.ignoreElement();
		}
	}

	/**
	 * Compiles all templates in the storage/templates folder to a single javascript file
	 */
	private Single<String> compileJs() {
		File templateDir = new File(storageDir, TEMPLATE_DIR_NAME);
		FileSystem fs = vertx.fileSystem();
		FileTemplateLoader loader = new FileTemplateLoader(templateDir);
		Handlebars handlebars = new Handlebars(loader);

		return vertx.fileSystem().rxReadDir(templateDir.getPath())
			.flatMapObservable(Observable::fromIterable)
			.filter(this::isTemplate)
			.flatMapSingle(path -> toCompiledJs(handlebars, path))
			.reduce(RxUtil.joining("\n\n"))
			.toSingle("");
	}

	/**
	 * Compiles a single template
	 */
	private Single<String> toCompiledJs(Handlebars handlebars, String templatePath) {
		return vertx.<String>rxExecuteBlocking(() -> handlebars.compileInline(String.format("{{precompile \"%s\"}}", getName(templatePath))).apply(null)).toSingle();
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
