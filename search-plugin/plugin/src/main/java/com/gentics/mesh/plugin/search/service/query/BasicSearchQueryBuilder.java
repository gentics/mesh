package com.gentics.mesh.plugin.search.service.query;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.search.config.SearchConfiguration;
import com.gentics.mesh.plugin.search.extension.SearchQueryBuilder;
import com.gentics.mesh.plugin.search.util.JsonUtils;
import com.gentics.mesh.plugin.search.util.QueryBuilderUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.Objects;
import java.util.Set;

/**
 * Builds Elasticsearch queries by setting search parameters at specified positions in the raw query.
 *
 * <p>
 *     The paths where the parameters should be put must be specified in the raw query in the special field
 *     "params", which lists all the paths where a certain parameter should be inserted. This is an example
 *     for the "params" field:
 * </p>
 *
 * <pre>
 * {
 *   "params": {
 *     "query": [
 *       "query/bool/must/match/fields.content",
 *       "query/bool/should/match/fields.author"
 *     ],
 *     "lang": [ "query/bool/filter[0]/match/language" ],
 *     "projects": [ "query/bool/filter[1]/match/project" ]
 *   }
 * }
 * </pre>
 *
 * <p>
 *     The parameters "query", "lang", and "projects", will be set in all query types. The "tags" parameter, will
 *     only be set for {@link #buildSearchQuery(PluginContext, String) search queries}.
 * </p>
 *
 * <p>
 *     For search queries, the "from" and "size" fields will also be set accordingly.
 * </p>
 */
public class BasicSearchQueryBuilder implements SearchQueryBuilder {

	private static final Logger log = LoggerFactory.getLogger(BasicSearchQueryBuilder.class);

	protected SearchConfiguration config;

	@Override
	public SearchQueryBuilder init(SearchConfiguration config) {
		this.config = config;
		return this;
	}

	/**
	 * Sets the common parameters "query", "language" and "projects" for all names, as well as
	 * "tags", "size" and "from" when <code>name</code> is <code>search</code>.
	 *
	 * @see #setCommonFields(JsonObject, JsonObject, Set, PluginContext)
	 *
	 * @param context The current plugin context
	 * @param name The name of the query
	 * @param rawQuery Initially loaded search query which can be modified. May be null if you want to create your query from scratch
	 * @return The final query
	 */
	@Override
	public JsonObject buildQuery(PluginContext context, String name, String rawQuery) {
		return buildQuery(context, rawQuery, true);
	}

	/**
	 * Build the search query.
	 *
	 * @see #buildQuery(PluginContext, String, String)
	 *
	 * @param context The current plugin context
	 * @param rawQuery Initially loaded search query which can be modified. May be null if you want to create your query from scratch
	 * @return The loaded and postprocessed search query
	 */
	@Override
	public JsonObject buildSearchQuery(PluginContext context, String rawQuery) {
		return buildQuery(context, "search", rawQuery);
	}

	/**
	 * Build the autocomplete query.
	 *
	 * @see #buildQuery(PluginContext, String, String)
	 *
	 * @param context The current plugin context
	 * @param rawQuery Initially loaded search query which can be modified. May be null if you want to create your query from scratch
	 * @return The loaded and postprocessed autocomplete query
	 */
	@Override
	public JsonObject buildAutocompleteQuery(PluginContext context, String rawQuery) {
		return buildQuery(context, rawQuery, false);
	}

	/**
	 * Build the did you mean query.
	 *
	 * @see #buildQuery(PluginContext, String, String)
	 *
	 * @param context The current plugin context
	 * @param rawQuery Initially loaded search query which can be modified. May be null if you want to create your query from scratch
	 * @param binary Whether to construct a did-you-mean query for binaries
	 * @return The loaded and postprocessed did-you-mean query
	 */
	@Override
	public JsonObject buildDidYouMeanQuery(PluginContext context, String rawQuery, boolean binary) {
		return buildQuery(context, rawQuery, false);
	}

	/**
	 * Sets the "query", "lang" and "projects" parameters.
	 *
	 * @param query The raw query as a JSON object
	 * @param params The JSON object specifying the parameter positions
	 * @param projects The projects the request will search
	 * @param context The current plugin context
	 * @return The <code>query</code> with the "query", "lang" and "projects" parameters inserted
	 */
	protected JsonObject setCommonFields(JsonObject query, JsonObject params, Set<String> projects, PluginContext context) {
		HttpServerRequest request = context.request();
		String lang = request.getParam("l");

		if (StringUtils.isBlank(lang)) {
			lang = "@";
		}

		// Putting a string value in a JsonObject will take care of JSON escaping, so we only need
		// to sanitize the search term.
		setParam(query, params, "query", QueryBuilderUtils.getSanitizedSearchTerm(request));
		setParam(query, params, "lang", StringEscapeUtils.escapeJson(lang));
		setParam(query, params, "projects", QueryBuilderUtils.getProjectsRegexp(projects));

		return query;
	}

	/**
	 * Extract the parameter position information from the raw query.
	 * @param query The raw query
	 * @return The specification for parameters positions
	 */
	protected JsonObject extractParams(JsonObject query) {
		JsonObject params = query.getJsonObject("params", new JsonObject());

		query.remove("params");

		return params;
	}

	/**
	 * Set the specified value at all positions specified for <code>paramName</code>.
	 *
	 * @param query The input query
	 * @param params The specification of parameters positions
	 * @param paramName The parameter to insert
	 * @param value The value of the parameter
	 */
	protected void setParam(JsonObject query, JsonObject params, String paramName, String value) {
		if (params == null) {
			log.warn("No parameters provided, search query is hardcoded");
			return;
		}

		JsonArray paths = params.getJsonArray(paramName, new JsonArray());
		if (paths == null || paths.isEmpty()) {
			log.debug("No paths for parameter \"{}\" provided", paramName);
			return;
		}

		log.trace("Replacing parameter \"{}\"", paramName);

		paths.stream()
			.filter(Objects::nonNull)
			.map(Object::toString)
			.forEach(path -> JsonUtils.setValue(query, path, value));
	}

	/**
	 * Sets the common parameters "query", "language" and "projects" for all names, as well as
	 * "tags", "size" and "from" when {@code addPaging} is {@code true}.
	 *
	 * @see #setCommonFields(JsonObject, JsonObject, Set, PluginContext)
	 *
	 * @param context The current plugin context
	 * @param rawQuery Initially loaded search query which can be modified. May be null if you want to create your query from scratch
	 * @return The final query
	 */
	private JsonObject buildQuery(PluginContext context, String rawQuery, boolean addPaging) {
		JsonObject query = new JsonObject(rawQuery);
		JsonObject params = extractParams(query);

		setCommonFields(query, params, config.getSearchedProjects(context.projectName()), context);

		HttpServerRequest request = context.request();

		setParam(
			query,
			params,
			"tags",
			String.join(" ", QueryBuilderUtils.getTags(request)));

		if (addPaging) {
			int page = NumberUtils.toInt(request.getParam("p"), 0);
			int pageSize = config.getPageSize();

			query.put("size", pageSize);
			query.put("from", page * pageSize);
		}

		return query;
	}
}
