package com.gentics.mesh.plugin.search.handler;

import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.search.config.SearchConfiguration;
import com.gentics.mesh.plugin.search.extension.CustomHandler;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Base class for search query handlers.
 */
public abstract class AbstractQueryHandler {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	/** The plugin configuration. */
	protected final SearchConfiguration config;
	/** The current plugin context. */
	protected final PluginContext context;
	/** Whether the project receiving the request is configured to search multiple projects. */
	protected final boolean multiProjectSearch;
	/**
	 * Whether the Mesh search request should wait for Elasticsearch to be idle before performing the search.
	 *
	 * May be {@code null}, in wich case Mesh decides based on its configuration.
	 */
	protected final Boolean waitForIdle;

	/** Supplier for the actual Elasticsearch query. */
	protected Supplier<Single<JsonObject>> querySupplier;
	/** Post processing function for the final search result. */
	protected io.reactivex.functions.Function<Object, Completable> resultMapper;

	protected AbstractQueryHandler(SearchConfiguration config, PluginContext context) {
		this.config = config;
		this.context = context;
		this.multiProjectSearch = config.isMultiProjectSearch(context.projectName());
		this.waitForIdle = config.getWaitForIdle();
	}

	/**
	 * Calls the concrete implementation {@link #internalHandle()}.
	 *
	 * @return A <code>Completable</code> for the handler action
	 */
	public Completable handle() {
		if (querySupplier == null || resultMapper == null) {
			String msg = getClass().getSimpleName() + " has not been initialized";

			log.error(msg);

			return Completable.error(new Exception(msg));
		}

		return internalHandle();
	}

	/**
	 * Does the actual handling of the search request.
	 *
	 * @return A <code>Completable</code> for the handler action
	 */
	abstract protected Completable internalHandle();

	/**
	 * Sets up the {@link #querySupplier} and {@link #resultMapper} with the given <code>supplier</code>
	 * and {@link #sendResult(PluginContext, Object)} respectively.
	 *
	 * @param supplier The query supplier to use
	 */
	protected void init(Function<PluginContext, Single<JsonObject>> supplier) {
		querySupplier = () -> supplier.apply(context);
		resultMapper = result -> sendResult(context, result);

		if (log.isDebugEnabled()) {
			log.debug("{} initialized", getClass().getSimpleName());
		}
	}

	/**
	 * Sets up the {@link #querySupplier} and {@link #resultMapper} with the
	 * {@link CustomHandler#getQuery(PluginContext) getQuery()} and
	 * {@link CustomHandler#processResult(PluginContext, Object) processResult()} from the given
	 * <code>customHandler</code>
	 *
	 * @param customHandler The custom handler to use
	 */
	protected void init(CustomHandler customHandler) {
		querySupplier = () -> customHandler.getQuery(context);
		resultMapper = result -> customHandler.processResult(context, result);

		if (log.isDebugEnabled()) {
			log.debug("Custom {} \"{}\" initialized", getClass().getSimpleName(), customHandler.name());
		}
	}

	/**
	 * Log the given <code>query</code> when loglevel TRACE is enabled.
	 *
	 * @param query The query to log
	 */
	protected void traceQuery(JsonObject query) {
		if (log.isTraceEnabled()) {
			log.trace("Using Elasticsearch query:\n{}", query.encodePrettily());
		}
	}

	/**
	 * End the {@link PluginContext#response() response} by sending the given <code>result</code>.
	 *
	 * The result must be either a {@link JsonObject} or a {@link JsonArray} or an empty answer will be sent.
	 *
	 * @param pc The current plugin context
	 * @param result The final result of the Elasticsearch query
	 * @return A <code>Completable</code> for the post processing action
	 */
	private Completable sendResult(PluginContext pc, Object result) {
		return Completable.fromAction(() -> {
			String response;

			if (result instanceof JsonObject) {
				response = ((JsonObject) result).encode();
			} else if (result instanceof JsonArray) {
				response = ((JsonArray) result).encode();
			} else {
				log.warn("Result is not a JSON object, sending empty response");
				response = "{}";
			}

			pc.response()
				.putHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.APPLICATION_JSON_UTF8)
				.end(response);
		});
	}
}
