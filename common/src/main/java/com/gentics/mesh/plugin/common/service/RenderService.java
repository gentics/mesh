package com.gentics.mesh.plugin.common.service;

import com.gentics.mesh.plugin.common.dagger.StorageDir;
import io.reactivex.Single;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.templ.handlebars.HandlebarsTemplateEngine;

import javax.inject.Inject;
import java.io.File;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A wrapper for rendering handlebars templates.
 *
 * <p>
 *     The service can be automatically injected, in that case the base template
 *     path must be {@code STORAGE_DIR + "/templates"}, or the base path can
 *     be specified explicitly.
 * </p>
 *
 * <p>
 *     All render methods will make sure that the template name starts with
 *     the base template name.
 * </p>
 */
public class RenderService {

	private static final Logger log = LoggerFactory.getLogger(RenderService.class);

	private static final Pattern LEADING_SLASHES = Pattern.compile("^/*");
	private static final Pattern TRAILING_SLASHES = Pattern.compile("/*$");

	private final HandlebarsTemplateEngine templateEngine;
	private final String templateBasePath;

	/**
	 * Injection constructor.
	 *
	 * <p>
	 *     Requires the templates to reside in {@code STORAGE_DIR + "/templates/")}.
	 * </p>
	 *
	 * @param storageDir The plugins storage directory
	 * @param templateEngine The template engine instance to use
	 */
	@Inject
	public RenderService(@StorageDir File storageDir, HandlebarsTemplateEngine templateEngine) {
		String storageDirPath = storageDir.getAbsolutePath();

		this.templateEngine = templateEngine;
		this.templateBasePath = storageDirPath + "/templates/";

		log.debug("RenderService initialized with storageDirPath: {}, templateBasePath: {}", storageDirPath, templateBasePath);
	}

	/**
	 * Explicit constructor.
	 *
	 * <p>
	 *     Uses the specified path as base template path.
	 * </p>
	 *
	 * @param templateBasePath The base template path
	 * @param templateEngine The template engine instance to use
	 */
	public RenderService(String templateBasePath, HandlebarsTemplateEngine templateEngine) {
		this.templateBasePath = TRAILING_SLASHES.matcher(templateBasePath).replaceFirst("/");
		this.templateEngine = templateEngine;

		log.debug("RenderService initialized with templateBasePath: {}", templateBasePath);
	}

	/**
	 * Make sure the template name starts with the {@link #templateBasePath}.
	 *
	 * <p>
	 *     There should be no need to call this method directly, all the render
	 *     methods will call it automatically.
	 * </p>
	 *
	 * @see #render(JsonObject, String, Handler)
	 * @see #render(Map, String, Handler)
	 * @see #rxRender(JsonObject, String)
	 * @see #rxRender(Map, String)
	 * @param template The template name
	 * @return Either {@code template} if the string already starts with
	 * 		{@code templateBasePath} or the concatenation of
	 * 		{@code templateBasePath + template} otherwise
	 */
	public String adjustTemplatePath(String template) {
		if (template.startsWith(templateBasePath)) {
			return template;
		}

		String templateFileName = templateBasePath + LEADING_SLASHES.matcher(template).replaceAll("");

		log.trace("Rendering template <{}>", templateFileName);

		return templateFileName;
	}

	/**
	 * Render the given template with the given context.
	 *
	 * <p>
	 *     Convenience method for {@link #render(Map, String, Handler)}.
	 * </p>
	 *
	 * @param ctx The context for rendering
	 * @param template The template to render
	 * @param resultHandler The result handler
	 */
	public void render(JsonObject ctx, String template, Handler<AsyncResult<Buffer>> resultHandler) {
		render(ctx.getMap(), template, resultHandler);
	}

	/**
	 * Render the given template with the given context.
	 *
	 * @param ctx The context for rendering
	 * @param template The template to render
	 * @param resultHandler The result handler
	 */
	public void render(Map<String, Object> ctx, String template, Handler<AsyncResult<Buffer>> resultHandler) {
		templateEngine.render(ctx, adjustTemplatePath(template), resultHandler);
	}

	/**
	 * Render the given template with the given context.
	 *
	 * <p>
	 *     Convenience method for {@link #rxRender(Map, String)}.
	 * </p>
	 *
	 * @param ctx The context for rendering
	 * @param template The template to render
	 */
	public Single<Buffer> rxRender(JsonObject ctx, String template) {
		return rxRender(ctx.getMap(), template);
	}

	/**
	 * Render the given template with the given context.
	 *
	 * @param ctx The context for rendering
	 * @param template The template to render
	 */
	public Single<Buffer> rxRender(Map<String, Object> ctx, String template) {
		return templateEngine.rxRender(ctx, adjustTemplatePath(template));
	}
}
