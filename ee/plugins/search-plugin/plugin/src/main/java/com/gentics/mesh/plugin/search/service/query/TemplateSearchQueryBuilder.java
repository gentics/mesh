package com.gentics.mesh.plugin.search.service.query;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.search.config.SearchConfiguration;
import com.gentics.mesh.plugin.search.extension.CachingSearchQueryBuilder;
import com.gentics.mesh.plugin.search.extension.SearchQueryBuilder;
import com.gentics.mesh.plugin.search.util.QueryBuilderUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Builds queries by replacing search parameters via {@link Handlebars}
 * templates.
 *
 * <p>
 *     The generated templates will be cached for
 *     {@link SearchConfiguration#getExpireTime()} seconds.
 * </p>
 *
 * <p>
 *     The parameters "query", "lang", "projects", and "tags" will be set in all
 *     queries.
 * </p>
 *
 * <p>
 *     For search and custom queries, the "from" and "size" fields will also be
 *     set accordingly.
 * </p>
 *
 * <p>
 *     <strong>IMPORTANT:</strong> the parameters are already sanitized and
 *     escaped, so the raw query template should always use the
 *     "<code>{{{ value }}}</code>" placeholder notation to prevent Handlebars
 *     from escaping HTML markup.
 * </p>
 */
public class TemplateSearchQueryBuilder implements CachingSearchQueryBuilder {

	private static final Logger log = LoggerFactory.getLogger(TemplateSearchQueryBuilder.class);

	protected final Handlebars handlebars = new Handlebars();

	protected SearchConfiguration config;
	protected Cache<String, Object> cache;

	@Override
	public SearchQueryBuilder init(SearchConfiguration config) {
		this.config = config;
		this.cache = Caffeine.newBuilder()
			.expireAfterWrite(config.getExpireTime(), TimeUnit.SECONDS)
			.build();
		return this;
	}

	/**
	 * Tries to load the specified query template from the cache, or creates it
	 * from the {@code rawQuery}.
	 *
	 * <p>
	 *     The fields {@code size} and {@code from} will automatically be added
	 *     to the query.
	 * </p>
	 *
	 * @see #buildQuery(PluginContext, String, String, boolean)
	 *
	 * @param context The current plugin context
	 * @param name The name of the query
	 * @param rawQuery Initially loaded search query which can be modified. May
	 * 		be null if you want to create your query from scratch
	 * @return The final query
	 */
	@Override
	public JsonObject buildQuery(PluginContext context, String name, String rawQuery) {
		return buildQuery(context, name, rawQuery, true);
	}

	@Override
	public JsonObject buildQuery(PluginContext context, String name) {
		return buildQuery(context, name, null);
	}

	@Override
	public JsonObject buildSearchQuery(PluginContext context, String rawQuery) {
		return buildQuery(context, "search", rawQuery, true);
	}

	@Override
	public JsonObject buildSearchQuery(PluginContext context) {
		return buildSearchQuery(context, null);
	}

	@Override
	public JsonObject buildAutocompleteQuery(PluginContext context, String rawQuery) {
		return buildQuery(context, "auto", rawQuery, false);
	}

	@Override
	public JsonObject buildAutocompleteQuery(PluginContext context) {
		return buildAutocompleteQuery(context, null);
	}

	@Override
	public JsonObject buildDidYouMeanQuery(PluginContext context, String rawQuery, boolean binary) {
		return buildQuery(context, "dym" + (binary ? "b" : "c"), rawQuery, false);
	}

	@Override
	public JsonObject buildDidYouMeanQuery(PluginContext context, boolean binary) {
		return buildDidYouMeanQuery(context, null, binary);
	}

	@Override
	public void clearCache() {
		cache.invalidateAll();
	}


	/**
	 * Tries to load the specified query template from the cache, or creates it
	 * from the {@code rawQuery}.
	 *
	 * @see #parseTemplate(String)
	 * @see #buildQuery(PluginContext, boolean, Object)
	 *
	 * @param context The current plugin context
	 * @param name The name of the query
	 * @param rawQuery Initially loaded search query which can be modified. May
	 * 		be null if you want to create your query from scratch
	 * @param addPaging Whether to add the {@code size} and {@code from} fields
	 * 		to the query
	 * @return The final query
	 */
	protected JsonObject buildQuery(PluginContext context, String name, String rawQuery, boolean addPaging) {
		String cacheKey = context.projectName() + "/" + name;
		Object template = cache.get(cacheKey, parseTemplate(rawQuery));

		if (template == null) {
			if (log.isDebugEnabled()) {
				log.debug("Template for query {} not found in cache", cacheKey);
			}

			return null;
		}

		return buildQuery(context, addPaging, template);
	}

	/**
	 * Build the final query from the given template.
	 *
	 * <p>
	 *     When template generation failed, <code>template</code> will be a {@link RuntimeException} which is
	 *     just thrown immediately.
	 * </p>
	 * <p>
	 *     Page and size parameters are added automatically
	 * </p>
	 * @param context The current plugin context
	 * @param addPaging Whether to add the {@code size} and {@code page} fields to the query
	 * @param template The Handlebars template for the query
	 * @return The final query
	 * @throws RuntimeException When the template could not be generated or applied to the search parameters
	 */
	protected JsonObject buildQuery(PluginContext context, boolean addPaging, Object template) {
		Map<String, Object> params = getParameters(context.request(), context.projectName());

		if (template instanceof RuntimeException) {
			throw (RuntimeException) template;
		}
		JsonObject query;

		try {
			 query = new JsonObject(((Template) template).apply(params));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		if (addPaging) {
			int page = NumberUtils.toInt(context.request().getParam("p"), 0);
			int pageSize = config.getPageSize();

			query.put("size", pageSize)
				.put("from", page * pageSize);
		}

		return query;
	}

	/**
	 * Extract the "query", "lang", "projects" and if needed the "tags" parameters from the search request.
	 *
	 * @param request The current search request
	 * @param projectName The project that received the search request
	 * @return A map containing the extracted query parameters
	 */
	protected Map<String, Object> getParameters(HttpServerRequest request, String projectName) {
		Map<String, Object> params = new HashMap<>();
		String param = request.getParam("l");

		if (StringUtils.isBlank(param)) {
			param = "@";
		}

		params.put("lang", StringEscapeUtils.escapeJson(param));
		params.put("query", QueryBuilderUtils.getEscapedSearchTerm(request));
		params.put("projects", QueryBuilderUtils.getProjectsRegexp(config.getSearchedProjects(projectName)));
		params.put("tags", String.join(" ", QueryBuilderUtils.getTags(request)));

		return params;
	}

	/**
	 * Creates a loading function for creating a {@link Template Handlebars template}.
	 *
	 * <p>
	 *     The returned function will return <code>null</code> when the <code>source</code> is empty, or a
	 *     {@link RuntimeException} when the template generation fails.
	 * </p>
	 *
	 * @param source The source for the template
	 * @return A loading function which will create a Handlebars template
	 */
	protected Function<String, Object> parseTemplate(String source) {
		return cacheKey -> {
			if (StringUtils.isBlank(source)) {
				log.debug("No source for template provided");

				return null;
			}

			log.info("Creating template for {}", cacheKey);
			try {
				return handlebars.compileInline(source);
			} catch (IOException e) {
				log.error("Could not parse query template \"" + cacheKey + "\":\n\t" + source);
				return new RuntimeException(e);
			}
		};
	}
}
