package com.gentics.mesh.plugin.search.extension;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.search.config.SearchConfiguration;
import io.vertx.core.json.JsonObject;
import org.pf4j.ExtensionPoint;

/**
 * Constructs custom search queries based on the {@link PluginContext}.
 */
public interface SearchQueryBuilder extends ExtensionPoint {

	SearchQueryBuilder init(SearchConfiguration configuration);

	/**
	 * Construct the specified query.
	 *
	 * <p>
	 *     This method can be used by {@link com.gentics.mesh.plugin.search.handler.custom.CustomHandler CustomHandlers}
	 *     to load custom search queries.
	 * </p>
	 *
	 * @param context The current plugin context
	 * @param name The name of the query
	 * @param rawQuery Initially loaded search query which can be modified. May be null if you want to create your query from scratch
	 * @return The final query
	 */
	JsonObject buildQuery(PluginContext context, String name, String rawQuery);

	/**
	 * Construct the search query.
	 *
	 * @param context The current plugin context
	 * @param rawQuery Initially loaded search query which can be modified. May be null if you want to create your query from scratch
	 * @return JsonObject of the final elasticsearch query
	 */
	JsonObject buildSearchQuery(PluginContext context, String rawQuery);

	/**
	 * Construct the query for the autocompletion.
	 *
	 * @param context The current plugin context
	 * @param rawQuery Initially loaded search query which can be modified. May be null if you want to create your query from scratch
	 * @return JsonObject of the final elasticsearch query
	 */
	JsonObject buildAutocompleteQuery(PluginContext context, String rawQuery);

	/**
	 * Construct the did-you-mean query for content or binaries.
	 *
	 * @param context The current plugin context
	 * @param rawQuery Initially loaded search query which can be modified. May be null if you want to create your query from scratch
	 * @param binary Whether to construct a did-you-mean query for binaries
	 * @return JsonObject of the final elasticsearch query
	 */
	JsonObject buildDidYouMeanQuery(PluginContext context, String rawQuery, boolean binary);
}
