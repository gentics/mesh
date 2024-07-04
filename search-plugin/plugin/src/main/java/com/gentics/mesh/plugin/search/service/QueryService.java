package com.gentics.mesh.plugin.search.service;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.search.config.SearchConfiguration;
import com.gentics.mesh.plugin.search.extension.CachingSearchQueryBuilder;
import com.gentics.mesh.plugin.search.extension.SearchQueryBuilder;
import com.gentics.mesh.plugin.search.service.query.QueryType;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides Elasticsearch and GraphQL queries.
 */
@Singleton
public class QueryService extends AbstractCachingService<QueryService.CacheKey, String> {

	/** Cache key comprised of {@link QueryType}, query name and project name. */
	static class CacheKey {
		final QueryType type;
		final String queryName;
		final String project;

		CacheKey(QueryType type, String queryName, String project) {
			Objects.requireNonNull(type, "CacheKey type must not be null");
			Objects.requireNonNull(queryName, "CacheKey query must not be null");
			Objects.requireNonNull(project, "CacheKey must not be null");

			this.type = type;
			this.queryName = queryName;
			this.project = project;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}

			if (other == null || getClass() != other.getClass()) {
				return false;
			}

			CacheKey cacheKey = (CacheKey) other;

			return type == cacheKey.type &&
				queryName.equals(cacheKey.queryName) &&
				project.equals(cacheKey.project);
		}

		@Override
		public int hashCode() {
			return Objects.hash(type, queryName, project);
		}

		@Override
		public String toString() {
			return type.name() + "/" + project + "/" + queryName;
		}
	}

	/** Pattern to remove comments in the source file. The inline modified (?m) enables Pattern.MULTILINE mode. */
	private static final Pattern PATTERN_COMMENT = Pattern.compile("(?m)#.*$");

	/** Pattern to find occurrences of fragments in a GraphQL query. */
	private static final Pattern PATTERN_FRAGMENT_USAGE = Pattern.compile("\\.{3}\\s*(\\w+)");

	/** Pattern to find fragment definitions. */
	private static final Pattern PATTERN_FRAGMENT_DEFINITION = Pattern.compile("fragment\\s+(\\w+)");

	private final SearchConfiguration config;

	/** The absolute path to the query files. */
	private final String absoluteQueryPath;

	private final SchemaService schemaService;

	/** The injected search query builder. */
	private final Provider<SearchQueryBuilder> queryBuilderProvider;

	/**
	 * Default constructor.
	 * @param config The portal configuration
	 * @param storagePath The path to the plugin storage
	 * @param schemaService The injected schema service
	 * @param queryBuilderProvider The provider for the search query builder extension
	 */
	@Inject
	public QueryService(
			SearchConfiguration config,
			File storagePath,
			SchemaService schemaService,
			Provider<SearchQueryBuilder> queryBuilderProvider) {
		super(config);

		this.config = config;
		this.absoluteQueryPath = new File(storagePath, "/queries").getAbsolutePath();
		this.schemaService = schemaService;
		this.queryBuilderProvider = queryBuilderProvider;

		log.info("QueryService initialized");
	}

	/**
	 * Get the GraphQL query to load additional information about search hits.
	 *
	 * @param projectName The name of the searched project
	 * @return A <code>Single></code> containing the GraphQL query to load additional information about search hits
	 */
	public Single<String> getSearchResultQuery(String projectName) {
		return getFromCache(new CacheKey(QueryType.GraphQL, config.getResultQuery(), projectName));
	}

	/**
	 * Get the GraphQL query to load additional information about search hits.
	 *
	 * @param projectName The name of the searched project
	 * @param queryName The name of the GraphQL query to load
	 * @return A <code>Single></code> containing the GraphQL query to load additional information about search hits
	 */
	public Single<String> getSearchResultQuery(String projectName, String queryName) {
		return getFromCache(new CacheKey(QueryType.GraphQL, queryName, projectName));
	}

	/**
	 * Get the custom query with the given name.
	 *
	 * @param pc The current plugin context
	 * @param name The name of the custom query to load
	 * @return A <code>Single</code> containing the requested query
	 */
	public Single<JsonObject> getCustomQuery(PluginContext pc, String name) {
		SearchQueryBuilder queryBuilder = queryBuilderProvider.get();

		if (queryBuilder instanceof CachingSearchQueryBuilder) {
			JsonObject query = ((CachingSearchQueryBuilder) queryBuilder).buildQuery(pc, name);

			if (query != null) {
				return Single.just(query);
			}
		}

		CacheKey key = new CacheKey(QueryType.ElasticSearch, name, pc.projectName());

		return getFromCache(key).map(query -> queryBuilder.buildQuery(pc, name, query));
	}

	/**
	 * Get the Elasticsearch query to search an object in Mesh.
	 *
	 * <p>
	 * The currently supported parameters are
	 * <ul>
	 * <li><code>query</code>: the search term itself
	 * <li><code>lang</code>: the desired language
	 * <li><code>tags</code>: filter tags for the search
	 * <li><code>projects</code>: a regular expression matching the valid project names
	 * </ul>
	 * </p>
	 *
	 * @param pc The current plugin context
	 * @return A <code>Single</code> with the final Elasticsearch query
	 */
	public Single<JsonObject> getSearchQuery(PluginContext pc) {
		SearchQueryBuilder queryBuilder = queryBuilderProvider.get();

		if (queryBuilder instanceof CachingSearchQueryBuilder) {
			JsonObject query = ((CachingSearchQueryBuilder) queryBuilder).buildSearchQuery(pc);

			if (query != null) {
				return Single.just(query);
			}
		}

		CacheKey key = new CacheKey(QueryType.ElasticSearch, "search", pc.projectName());

		return getFromCache(key).map(query -> queryBuilder.buildSearchQuery(pc, query));
	}

	/**
	 * Get the Elasticsearch query to load suggestions for better search terms
	 *
	 * <p>
	 * The currently supported parameters are
	 * <ul>
	 * <li><code>query</code>: the original search term itself
	 * <li><code>lang</code>: the desired language
	 * <li><code>projects</code>: a regular expression matching the valid project names
	 * </ul>
	 * </p>
	 *
	 * @param pc The current plugin context
	 * @param binary Whether the query is meant for binaries
	 * @return A <code>Single</code> with the final Elasticsearch query
	 */
	public Single<JsonObject> getDidYouMeanQuery(PluginContext pc, boolean binary) {
		SearchQueryBuilder queryBuilder = queryBuilderProvider.get();

		if (queryBuilder instanceof CachingSearchQueryBuilder) {
			JsonObject query = ((CachingSearchQueryBuilder) queryBuilder).buildDidYouMeanQuery(pc, binary);

			if (query != null) {
				return Single.just(query);
			}
		}

		CacheKey key = new CacheKey(
			QueryType.ElasticSearch,
			"didYouMean".concat(binary ? "Binary" : "Content"),
			pc.projectName());

		return getFromCache(key).map(query -> queryBuilder.buildDidYouMeanQuery(pc, query, binary));
	}

	/**
	 * Get the Elasticsearch query to load autocomplete suggestions.
	 *
	 * <p>
	 * The currently supported parameters are
	 * <ul>
	 * <li><code>query</code>: the original search term itself
	 * <li><code>lang</code>: the desired language
	 * <li><code>projects</code>: a regular expression matching the valid project names
	 * </ul>
	 * </p>
	 *
	 * @param pc The current plugin context
	 * @return A <code>Single</code> with the final Elasticsearch query
	 */
	public Single<JsonObject> getAutocompleteQuery(PluginContext pc) {
		SearchQueryBuilder queryBuilder = queryBuilderProvider.get();

		if (queryBuilder instanceof CachingSearchQueryBuilder) {
			JsonObject query = ((CachingSearchQueryBuilder) queryBuilder).buildAutocompleteQuery(pc);

			if (query != null) {
				return Single.just(query);
			}
		}

		CacheKey key = new CacheKey(QueryType.ElasticSearch, "autocomplete", pc.projectName());

		return getFromCache(key).map(query -> queryBuilder.buildAutocompleteQuery(pc, query));
	}

	/**
	 * Clear the query cache.
	 */
	@Override
	public void clearCache() {
		super.clearCache();

		SearchQueryBuilder queryBuilder = queryBuilderProvider.get();

		if (queryBuilder instanceof CachingSearchQueryBuilder) {
			((CachingSearchQueryBuilder) queryBuilder).clearCache();
		}
	}

	/**
	 * Load the query with the given name and utilize the cache if available.
	 *
	 * <p>
	 * For GraphQL queries the <code>%contentSchema%</code>, <code>%folderSchema%</code>, and
	 * <code>%binaryContentSchema%</code> placeholders are replaced with the respective schema names
	 * for the specified project.
	 * </p>
	 *
	 * <p>
	 * Also this method will try to load missing fragment definitions in a GraphQL query from the
	 * <em>.../projectName/fragments</em> or <em>.../fragments</em> subdirectories.
	 * </p>
	 *
	 * @see #loadFromFile(String, String)
	 *
	 * @param key The cache key containing query type, name and project name
	 * @return The loaded query
	 */
	@Override
	@Nonnull
	protected Single<String> loadValue(CacheKey key) {
		String query = loadFromFile(key.type.filename(key.queryName), key.project);

		if (query == null) {
			throw new RuntimeException("Could not load query \"" + key.queryName + "\"");
		}

		if (key.type == QueryType.GraphQL) {
			query = addAllFragments(key.project, query);

			return schemaService.replaceSchemaNames(key.project, key.queryName, query)
				.doOnSuccess(q -> log.trace("Loaded query from filesystem:\n{}", q));
		}

		log.trace("Loaded query from filesystem:\n{}", query);

		return Single.just(query);
	}

	/**
	 * Returns the query with the given name.
	 *
	 * <p>
	 * All query files are searched in the directory {@link #absoluteQueryPath}. The method
	 * first tries to open the file <code><em>projectName/queryName.typeExtension</em></code>.
	 * When this file cannot be opened the method will try again with the filename
	 * <code><em>queryName.typeExtension</em></code>. When this also fails, the method tries to
	 * load a default query from the classpath.
	 * </p>
	 *
	 * @param queryFilename The filename of the query
	 * @param projectName The name of the Mesh project to use
	 * @return The requested query or <code>null</code> if it could not be found
	 */
	private String loadFromFile(String queryFilename, String projectName) {
		log.trace("Trying to load from file {}", queryFilename);

		String path = absoluteQueryPath + File.separatorChar + projectName;
		InputStream input = openFile(path, queryFilename);

		if (input == null) {
			log.trace("No project specific query found in {}, trying generic version", path);

			path = absoluteQueryPath;
			input = openFile(path, queryFilename);

			if (input == null) {
				log.trace("No query found in {}, trying classpath", path);

				path = "classpath:/queries/";
				input = getClass().getResourceAsStream("/queries/" + queryFilename);

				if (input == null) {
					log.error("Could not find query file {}", queryFilename);
					return null;
				}

			}

		}

		log.info("Loading {} from {}", queryFilename, path);

		try {
			return IOUtils.toString(input, StandardCharsets.UTF_8);
		} catch (IOException e) {
			log.error("Could not load query " + queryFilename, e);
		} finally {
			try {
				input.close();
			} catch (IOException e) {
				log.error("Could not close input properly: " + path, e);
			}
		}
		return null;
	}

	/**
	 * Creates an <code>InputStream</code> from the specified file.
	 *
	 * @param path The path of the files parent directory
	 * @param filename The filename
	 * @return <code>null</code> when the specified file cannot be opened
	 * 		for reading, or the respective {@link InputStream} otherwise
	 */
	private InputStream openFile(String path, String filename) {
		File file = new File(path, filename);

		if (!file.canRead()) {
			return null;
		}

		try {
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
			// We checked read access beforehand, so this should not happen (TM).
		}

		return null;
	}

	/**
	 * Tries to load fragment files as long as the query still contains undefined
	 * fragments.
	 *
	 * @see #addFragments(String, String)
	 *
	 * @param projectName The project the query is created for
	 * @param query The base query
	 * @return The query with all fragment definitions
	 */
	private String addAllFragments(String projectName, String query) {
		String extendedQuery = addFragments(projectName, query);

		while (!extendedQuery.equals(query)) {
			query = extendedQuery;
			extendedQuery = addFragments(projectName, query);
		}

		return query;
	}

	/**
	 * Find missing fragment definitions and load them from the filesystem.
	 *
	 * <p>
	 *	 Since the loaded fragments can use undefined fragments themselves,
	 *	 it is possible that this method has to be applied multiple times.
	 * </p>
	 *
	 * @see #addAllFragments(String, String)
	 *
	 * @param projectName The project the query is created for
	 * @param query The base query
	 * @return The query with the currently missing fragment definitions
	 */
	private String addFragments(String projectName, String query) {
		query = PATTERN_COMMENT.matcher(query).replaceAll("");

		// Find fragment uses (e.g. "... my_fragment")
		Set<String> missing = new HashSet<>();
		Matcher matcher = PATTERN_FRAGMENT_USAGE.matcher(query);

		while (matcher.find()) {
			missing.add(matcher.group(1));
		}

		// The keyword "on" is not a fragment name, but also used after "...".
		missing.remove("on");

		// Find fragment definitions.
		matcher = PATTERN_FRAGMENT_DEFINITION.matcher(query);

		while (matcher.find()) {
			missing.remove(matcher.group(1));
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("Missing fragments: [%s]", String.join(", ", missing)));
		}

		StringBuilder builder = new StringBuilder(query);

		for (String fragment : missing) {
			builder.append(loadFromFile(QueryType.GraphQL.filename("fragments" + File.separatorChar + fragment), projectName));
		}

		return builder.toString();
	}
}
