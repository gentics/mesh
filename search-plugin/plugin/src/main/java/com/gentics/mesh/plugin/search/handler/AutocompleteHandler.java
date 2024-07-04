package com.gentics.mesh.plugin.search.handler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.search.config.SearchConfiguration;
import com.gentics.mesh.plugin.search.extension.CustomHandler;
import com.gentics.mesh.plugin.search.service.QueryService;
import com.gentics.mesh.plugin.search.util.JsonUtils;
import com.gentics.mesh.plugin.search.util.MiscUtils;
import com.gentics.mesh.plugin.search.util.RequestUtils;
import com.gentics.vertx.http.HttpResponseStatus;

import io.reactivex.Completable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for autocomplete queries.
 *
 * <p>
 *     Loads the {@link QueryService#getAutocompleteQuery(PluginContext) autocomplete query} (when no {@link CustomHandler}
 *     is provided), and extracts suggestions for autocompletion from the returned highlights.
 * </p>
 */
@Singleton
public class AutocompleteHandler extends AbstractQueryHandler {

	private static final Logger log = LoggerFactory.getLogger(AutocompleteHandler.class);

	/** Pattern for the highlight markers used in the Elasticsearch query. */
	private static final Pattern HL_PATTERN = Pattern.compile("%hb%(.*?)%he%");

	@Inject
	public AutocompleteHandler(
			SearchConfiguration config,
			PluginContext context,
			QueryService queryService,
			@Nullable CustomHandler customHandler) {
		super(config, context);

		if (customHandler == null) {
			init(queryService::getAutocompleteQuery);
		} else {
			init(customHandler);
		}
	}

	/**
	 * Uses {@link #parseResult(JsonObject, String) parseResult()} to extract autocompletion suggestions from the
	 * query results highlights.
	 *
	 * @return A <code>completable</code> for the handler action
	 */
	@Override
	protected Completable internalHandle() {
		String queryParam = context.request().getParam("q");

		if (queryParam == null) {
			context.fail(HttpResponseStatus.BAD_REQUEST.code());

			return Completable.error(new Exception("Missing parameter \"q\""));
		}

		return querySupplier.get()
			.doOnSuccess(this::traceQuery)
			.flatMap(query -> RequestUtils.getSearchRequest(context, query.encode(), multiProjectSearch, waitForIdle).toSingle())
			.map(JsonUtils::jsonObjectOf)
			.map(result -> parseResult(result, context.request().getParam("q")))
			.flatMapCompletable(resultMapper)
			.doOnError(context::fail);
	}

	/**
	 * Parse the elastic search result and extract the found terms from the highlight
	 * property. Cross check the found terms with the query terms and build up a set
	 * of auto complete options.
	 *
	 * @param result Elasticsearch query response
	 * @param query Search phrase
	 *
	 * @return A JsonArray with the autocomplete options
	 */
	private JsonArray parseResult(JsonObject result, String query) {
		JsonObject emptyObject = new JsonObject();
		JsonArray emptyArray = new JsonArray();
		List<String> partials = Arrays.asList(query.trim().split("\\s+"));

		if (log.isDebugEnabled()) {
			log.debug("Query response:\n{}", result.encodePrettily());
		}

		JsonArray responses = result.getJsonArray("responses");

		if (responses == null || responses.isEmpty()) {
			log.warn("No responses from Elasticsearch query. Enable DEBUG for more details");
			return emptyArray;
		}

		JsonArray hits = responses.getJsonObject(0)
			.getJsonObject("hits", emptyObject)
			.getJsonArray("hits");

		if (hits == null) {
			log.warn("No hits in Elasticsearch response. Enable DEBUG for more details");
			return emptyArray;
		}

		Set<String> optionsSet = new LinkedHashSet<>();

		for (int i = 0; i < hits.size(); i++) {
			JsonObject hit = hits.getJsonObject(i);
			JsonObject allHighlights = hit.getJsonObject("highlight");
			JsonArray highlights = new JsonArray();

			for (String field : allHighlights.fieldNames()) {
				highlights.addAll(allHighlights.getJsonArray(field, emptyArray));
			}

			Set<String> foundTokens = new HashSet<>();
			for (int e = 0; e < highlights.size(); e++) {
				String firstHighlight = highlights.getString(e);
				// Remove all HTML
				firstHighlight = MiscUtils.stripHtml(firstHighlight);

				final Matcher matcher = HL_PATTERN.matcher(firstHighlight);
				while (matcher.find()) {
					String part = matcher.group(1);
					foundTokens.add(part);
				}
			}
			log.debug("Found tokens: {}", foundTokens);
			constructOptions(optionsSet, foundTokens, partials);
		}

		JsonArray options = new JsonArray();

		for (String option : optionsSet) {
			options.add(new JsonObject().put("name", option));
		}

		return options;
	}

	/**
	 * Construct auto completion options from the found tokens and the initial set of partials.
	 *
	 * Token that have any but the last partial as a prefix (the last partial is the term,
	 * the user is probably still typing) will be removed, since they are already "complete" in
	 * the current form of the search query.
	 *
	 * @param optionsSet The resulting set of options
	 * @param foundTokens The tokens found in pages matching the current query
	 * @param partials The individual search terms
	 */
	private void constructOptions(Set<String> optionsSet, Set<String> foundTokens, List<String> partials) {
		StringJoiner prefixBuilder = new StringJoiner(" ");
		List<String> fixedTerms = partials.subList(0, partials.size() - 1);

		for (String term : fixedTerms) {
			log.debug("Checking found tokens against partial \"{}\"", term);

			prefixBuilder.add(term);

			Iterator<String> it = foundTokens.iterator();

			while (it.hasNext()) {
				String token = it.next();

				if (token.toLowerCase().startsWith(term.toLowerCase())) {
					log.debug("  Pruning \"{}\"", token);
					it.remove();
				}
			}
		}

		String prefix = prefixBuilder.toString() + " ";

		for (String tokenOption : foundTokens) {
			optionsSet.add((prefix + tokenOption).trim());
		}

		log.debug("Newly found tokens: {}", foundTokens);
	}
}
