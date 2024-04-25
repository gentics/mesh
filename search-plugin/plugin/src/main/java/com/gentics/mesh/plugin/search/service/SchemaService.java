package com.gentics.mesh.plugin.search.service;

import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.plugin.search.config.SearchConfiguration;
import com.gentics.mesh.plugin.search.util.RxUtils;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Provides schema names for Mesh projects.
 */
@Singleton
public class SchemaService extends AbstractCachingService<SchemaService.CacheKey, Template> {

	/**
	 * Cache key for templates used in {@link #replaceSchemaNames(String, String, String)}.
	 *
	 * <p>
	 *     Only the {@link #project} and {@link #queryName} are used for the key. The {@link #source} is only needed
	 *     when the template needs to be generated.
	 * </p>
	 */
	static class CacheKey {
		final String project;
		final String queryName;
		final String source;

		CacheKey(String project, String queryName, String source) {
			Objects.requireNonNull(project, "CacheKey project must not be null");
			Objects.requireNonNull(queryName, "CacheKey queryName must not be null");
			Objects.requireNonNull(source, "CacheKey source must not be null");

			this.project = project;
			this.queryName = queryName;
			this.source = source;
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

			return project.equals(cacheKey.project)
				&& queryName.equals(cacheKey.queryName);
		}

		@Override
		public int hashCode() {
			return Objects.hash(project, queryName);
		}

		@Override
		public String toString() {
			return project + "/" + queryName;
		}
	}

	// The ? makes .* non greedy
	/** Pattern to capture prefixes for Mesh content repository schema names. */
	private static final Pattern CR_SCHEMA_PATTERN = Pattern.compile("(.*?)_(?:(?:binary_)?content|folder)");

	/** Mapping of placeholders to schema name postfixes. */
	private static final Map<String, String> SCHEMA_POSTFIXES = new HashMap<String, String>() {{
		put("binaryContentSchema", "_binary_content");
		put("contentSchema", "_content");
		put("folderSchema", "_folder");
	}};

	private static final Logger log = LoggerFactory.getLogger(SchemaService.class);

	private final Handlebars handlebars;
	private final MeshRestClient adminClient;
	private final AsyncLoadingCache<String, String> schemaCache;

	@Inject
	public SchemaService(SearchConfiguration config, MeshRestClient adminClient) {
		super(config);

		this.handlebars = new Handlebars();
		this.adminClient = adminClient;
		this.schemaCache = Caffeine.newBuilder()
			.expireAfterWrite(config.getExpireTime(), TimeUnit.SECONDS)
			.buildAsync(this::loadForCache);

		log.info("SchemaService initialized");
	}

	/**
	 * Return the schema prefix for the specified projects.
	 *
	 * <p>
	 *     When {@link SearchConfiguration#isCaching() caching} is enabled, this method will try to load it
	 *     from cache.
	 * </p>
	 *
	 * @see #loadProjectSchemaPrefix(String)
	 * @param project The project to get the schema prefix for
	 * @return The schema prefix for the specified project
	 */
	public Single<String> getSchemaPrefix(String project) {
		return config.isCaching()
			? Single.fromFuture(schemaCache.get(project))
			: loadProjectSchemaPrefix(project);
	}

	/**
	 * Replace the {@link #CR_SCHEMA_PATTERN placeholders} in <code>source</code> with the correct schema names
	 * for the specified project.
	 *
	 * <p>
	 *     A Handlebars {@link Template} is generated from the <code>source</code> and cached.
	 * </p>
	 *
	 * @param project The project to get schema names for
	 * @param queryName A name for <code>source</code> which can be used in the {@link CacheKey}
	 * @param source The string containing {@link #CR_SCHEMA_PATTERN placeholders} for schema names
	 * @return The <code>source</code> string where all the schame name placeholders are correctly replaced
	 */
	public Single<String> replaceSchemaNames(String project, String queryName, String source) {
		return getSchemaPrefix(project)
			.flatMap(prefix -> getFromCache(new CacheKey(project, queryName, source))
				.map(template -> template.apply(getSchemaNames(prefix))));
	}

	@Nonnull
	@Override
	protected Single<Template> loadValue(CacheKey key) {
		try {
			return Single.just(handlebars.compileInline(key.source));
		} catch (IOException e) {
			log.error(
				"Could not create template for query {}/: {}",
				e,
				key.project,
				key.queryName,
				e.getMessage());

			return Single.error(e);
		}
	}

	/**
	 * Clears the template and schema caches.
	 */
	@Override
	public void clearCache() {
		super.clearCache();
		schemaCache.synchronous().invalidateAll();
	}

	/**
	 * Create the complete schema names from the given <code>prefix</code> and the {@link #SCHEMA_POSTFIXES}.
	 *
	 * @param prefix The prefix for the schema names
	 * @return A map from schema name placeholders to actual schema names
	 */
	private Map<String, String> getSchemaNames(String prefix) {
		return SCHEMA_POSTFIXES.entrySet().stream()
			.collect(Collectors.toMap(Map.Entry::getKey, e -> prefix + e.getValue()));
	}

	private Single<String> loadProjectSchemaPrefix(String project) {
		if (StringUtils.isBlank(project)) {
			String msg = "Missing project name";

			log.error(msg);

			return Single.error(new Exception(msg));
		}

		log.debug("Loading schemas for project {}", project);

		return adminClient.findSchemas(project).toSingle().map(schemaPrefixExtractor(project));

	}

	/**
	 * Loading function for the schema prefix cache.
	 *
	 * @see #loadProjectSchemaPrefix(String)
	 * @param project The project to load the schema prefix for
	 * @param executor The executor to use
	 * @return A <code>CompletableFuture</code> with the schema prefix for <code>project</code>
	 */
	@Nonnull
	private CompletableFuture<String> loadForCache(String project, Executor executor) {
		return loadProjectSchemaPrefix(project)
			.subscribeOn(Schedulers.from(executor))
			.to(RxUtils::completableFuture);
	}

	/**
	 * Creates an extractor for the correct schema prefix for the specified <code>project</code> from a list of schemas.
	 *
	 * <p>
	 *     When a Mesh project is used as a content repository there must be the three schemas
	 *     <ul>
	 *         <li>PREFIX_binary_content,</li>
	 *         <li>PREFIX_content, and</li>
	 *         <li>PREFIX_folder</li>
	 *     </ul>
	 *     linked to the project.
	 *
	 *     The created extractor will make sure that
	 *     <ul>
	 *         <li>
	 *             there are exactly three schemas in the schema list conforming to the
	 *             {@link #CR_SCHEMA_PATTERN pattern} above, and that
	 *         </li>
	 *         <li>
	 *             there is only one possible value for PREFIX
	 *         </li>
	 *     </ul>
	 * </p>
	 *
	 * @param project The project to get the schema prefix for
	 * @return An extractor for the Mesh content repository schema prefix for <code>project</code>
	 */
	private Function<SchemaListResponse, String> schemaPrefixExtractor(String project) {
		return schemas -> {
			Map<String, List<String>> prefixes = schemas.getData().stream()
				.map(s -> CR_SCHEMA_PATTERN.matcher(s.getName()))
				.filter(Matcher::matches)
				.map(m -> Pair.of(m.group(1), m.group()))
				.collect(Collectors.groupingBy(Pair::getKey, Collectors.mapping(Pair::getValue, Collectors.toList())));

			if (log.isTraceEnabled()) {
				log.trace("Schemas for project {}:\n{}", project, schemas.toJson());
			}

			for (Entry<String, List<String>> entry: prefixes.entrySet()) {
				if (entry.getValue().size() == 3) {
					log.debug("Selected schema prefix: {}", entry.getKey());

					return entry.getKey();
				}
			}
			String error = String.format(
				"Could not determine schema prefix for project %s: [%s]",
				project,
				String.join(", ", prefixes.keySet()));

			log.error(error);

			throw new Exception(error);
		};
	}
}
