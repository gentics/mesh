package com.gentics.mesh.plugin.common.service;

import java.io.File;
import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.plugin.common.TemplateWrapper;
import com.gentics.mesh.plugin.common.dagger.StorageDir;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.cache.NullTemplateCache;

import io.reactivex.Single;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.templ.handlebars.impl.HandlebarsTemplateEngineImpl;
import io.vertx.reactivex.ext.web.templ.handlebars.HandlebarsTemplateEngine;
import io.vertx.reactivex.impl.AsyncResultSingle;

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
	private static final int MAX_CACHE_SIZE = 100;

	private final Cache<String, Template> inlineTemplateCache;
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
		// When Handlebars already uses a cache, there is no need to cache the templates here. This check assumes that
		// TemplateCache implementations other than NullTemplateCache actually perform caching.
		this.inlineTemplateCache = templateEngine.getDelegate().unwrap().getCache() instanceof NullTemplateCache
			? Caffeine.newBuilder().maximumSize(MAX_CACHE_SIZE).build()
			: null;

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
		// When Handlebars already uses a cache, there is no need to cache the templates here. This check assumes that
		// TemplateCache implementations other than NullTemplateCache actually perform caching.
		this.inlineTemplateCache = templateEngine.getDelegate().unwrap().getCache() instanceof NullTemplateCache
			? Caffeine.newBuilder().maximumSize(MAX_CACHE_SIZE).build()
			: null;

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
	 *     Convenience method for {@link #render(Map, TemplateWrapper, Handler)}.
	 * </p>
	 *
	 * @see #render(Map, TemplateWrapper, Handler)
	 * @param ctx The context for rendering.
	 * @param templatePath The path to the template to render.
	 * @param resultHandler The result handler.
	 */
	public void render(JsonObject ctx, String templatePath, Handler<AsyncResult<Buffer>> resultHandler) {
		render(ctx.getMap(), TemplateWrapper.wrap(null, templatePath), resultHandler);
	}

	/**
	 * Render the given template with the given context.
	 *
	 * <p>
	 *     Convenience method for {@link #render(Map, TemplateWrapper, Handler)}.
	 * </p>
	 *
	 * @see #render(Map, TemplateWrapper, Handler)
	 * @param ctx The context for rendering.
	 * @param templatePath The path to the template to render.
	 * @param resultHandler The result handler.
	 */
	public void render(Map<String, Object> ctx, String templatePath, Handler<AsyncResult<Buffer>> resultHandler) {
		render(ctx, TemplateWrapper.wrap(null, templatePath), resultHandler);
	}

	/**
	 * Render the given template with the given context.
	 *
	 * <p>
	 *     Convenience method for {@link #render(Map, TemplateWrapper, Handler)}.
	 * </p>
	 *
	 * @see #render(Map, TemplateWrapper, Handler)
	 * @param ctx The context for rendering.
	 * @param template The template to render.
	 * @param resultHandler The result handler.
	 */
	public void render(JsonObject ctx, TemplateWrapper template, Handler<AsyncResult<Buffer>> resultHandler) {
		render(ctx.getMap(), template, resultHandler);
	}

	/**
	 * Render the given template with the given context.
	 *
	 * <p>
	 *     When the template is actually a path to a template file, the actual
	 *     rendering is done by
	 *     {@link HandlebarsTemplateEngine#rxRender(Map, String)}. Otherwise
	 *     the template source will be compiled inline by
	 *     {@link #renderSource(Map, String, Handler)}.
	 * </p>
	 *
	 * @see #renderSource(Map, String, Handler)

	 * @param ctx The context for rendering.
	 * @param template The template to render.
	 * @param resultHandler The result handler.
	 */
	public void render(Map<String, Object> ctx, TemplateWrapper template, Handler<AsyncResult<Buffer>> resultHandler) {
		if (template.isTemplatePath()) {
			templateEngine.render(ctx, adjustTemplatePath(template.value())).andThen(resultHandler);
		} else {
			renderSource(ctx, template.value(), resultHandler);
		}
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
		return rxRender(ctx.getMap(), TemplateWrapper.wrap(null, template));
	}

	/**
	 * Render the given template with the given context.
	 *
	 * <p>
	 *     Convenience overload for {@link #rxRender(Map, TemplateWrapper)}.
	 * </p>
	 *
	 * @see #rxRender(Map, TemplateWrapper)
	 * @param ctx The context for rendering.
	 * @param template The template to render.
	 * @return A {@code Single} with the rendered result.
	 */
	public Single<Buffer> rxRender(JsonObject ctx, TemplateWrapper template) {
		return rxRender(ctx.getMap(), template);
	}

	/**
	 * Render the template with the given path using the given context.
	 *
	 * <p>
	 *     Convenience overload for {@link #rxRender(Map, TemplateWrapper)}.
	 * </p>
	 *
	 * @see #rxRender(Map, TemplateWrapper)
	 * @param ctx The context for rendering.
	 * @param templatePath The path to the template to render.
	 */
	public Single<Buffer> rxRender(Map<String, Object> ctx, String templatePath) {
		return rxRender(ctx, TemplateWrapper.wrap(null, templatePath));
	}

	/**
	 * Render the given template with the given context.
	 *
	 * <p>
	 *     When the template is actually a path to a template file, the actual
	 *     rendering is done by
	 *     {@link HandlebarsTemplateEngine#rxRender(Map, String)}. Otherwise
	 *     the template source will be compiled inline by
	 *     {@link #renderSource(Map, String, Handler)}.
	 * </p>
	 *
	 * @see #renderSource(Map, String, Handler)
	 *
	 * @param ctx The template context.
	 * @param template The path to the template file, or the template source.
	 * @return A {@code Single} with the rendered result.
	 */
	public Single<Buffer> rxRender(Map<String, Object> ctx, TemplateWrapper template) {
		if (template.isTemplatePath()) {
			return templateEngine.rxRender(ctx, adjustTemplatePath(template.value()));
		}

		return AsyncResultSingle.toSingle(handler -> renderSource(ctx, template.value(), handler));
	}


	/**
	 * Render the given template source with the given template context.
	 *
	 * <p>
	 *     Created templates will be cached (either by
	 *     {@link com.github.jknack.handlebars.Handlebars} itself or by the
	 *     {@code RenderService}).
	 * </p>
	 *
	 * @param templateCtx The context for rendering the template.
	 * @param source The template source.
	 * @param resultHandler A handler for the rendered result.
	 */
	private void renderSource(Map<String, Object> templateCtx, String source, Handler<AsyncResult<Buffer>> resultHandler) {
		try {
			Template template = inlineTemplateCache != null ? inlineTemplateCache.getIfPresent(source) : null;

			if (template == null) {
				template = templateEngine.getDelegate().unwrap().compileInline(source);

				if (inlineTemplateCache != null) {
					inlineTemplateCache.put(source, template);
				}
			}

			Context engineCtx = Context.newBuilder(templateCtx)
				.resolver(((HandlebarsTemplateEngineImpl) templateEngine.getDelegate()).getResolvers())
				.build();
			Buffer result = Buffer.buffer(template.apply(engineCtx));

			resultHandler.handle(Future.succeededFuture(result));
		} catch (Exception e) {
			resultHandler.handle(Future.failedFuture(e));
		}
	}
}
