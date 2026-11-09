package com.gentics.mesh.plugin.search.extension;

import com.gentics.mesh.plugin.PluginContext;
import io.vertx.core.json.JsonObject;

/**
 * A {@link SearchQueryBuilder} that can cache an intermediate form of the queries it generates.
 *
 * <p>
 *     The additional methods only get a {@link PluginContext} to fill in the actual query parameters. When the
 *     requested query is not available in the cache, these methods should return <code>null</code> so that
 *     the {@link com.gentics.mesh.plugin.search.service.QueryService} knows it must provide the raw query.
 * </p>
 */
public interface CachingSearchQueryBuilder extends SearchQueryBuilder {

	/**
	 * Clear all cached queries.
	 */
	void clearCache();

	/**
	 * Get the specified query if it is available in the cache.
	 *
	 * <p>
	 *     This method must return the same result as {@link #buildQuery(PluginContext, String, String)}, when the
	 *     query is available, or <code>null</code> when the query is not cached.
	 * </p>
	 *
	 * @see #buildQuery(PluginContext, String, String)
	 *
	 * @param context The current plugin context
	 * @param name The name of the query to build
	 * @return The specified query or <code>null</code> if it is not in the cache
	 */
	JsonObject buildQuery(PluginContext context, String name);

	/**
	 * Get the search query if it is available in the cache.
	 *
	 * <p>
	 *     This method must return the same result as {@link #buildSearchQuery(PluginContext, String)}, when the
	 *     query is available, or <code>null</code> when the query is not cached.
	 * </p>
	 *
	 * @see #buildSearchQuery(PluginContext, String)
	 *
	 * @param context The current plugin context
	 * @return The search query or <code>null</code> if it is not in the cache
	 */
	JsonObject buildSearchQuery(PluginContext context);

 	/**
	 * Get the autocomplete query if it is available in the cache.
	 *
	 * <p>
	 *     This method must return the same result as {@link #buildAutocompleteQuery(PluginContext, String)}, when the
	 *     query is available, or <code>null</code> when the query is not cached.
	 * </p>
	 *
	 * @see #buildAutocompleteQuery(PluginContext, String)
	 *
	 * @param context The current plugin context
	 * @return The autocomplete query or <code>null</code> if it is not in the cache
	 */
	JsonObject buildAutocompleteQuery(PluginContext context);

	/**
	 * Get the did-you-mean query if it is available in the cache.
	 *
	 * <p>
	 *     This method must return the same result as {@link #buildDidYouMeanQuery(PluginContext, String, boolean)}, when the
	 *     query is available, or <code>null</code> when the query is not cached.
	 * </p>
	 *
	 * @see #buildDidYouMeanQuery(PluginContext, String, boolean)
	 *
	 * @param context The current plugin context
	 * @param binary Whether to construct a did-you-mean query for binaries
	 * @return The did-you-mean query or <code>null</code> if it is not in the cache
	 */
	JsonObject buildDidYouMeanQuery(PluginContext context, boolean binary);
}
