package com.gentics.mesh.plugin.search.extension;

import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.search.HandlerType;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * A custom search handler.
 *
 * <p>
 *	 For each custom handler provided by the <code>customHandlers()</code> method of the <code>ExtensionService</code>
 *	 two handlers are registered for the respective route:
 *	 <ol>
 *	 	<li>
 *	 		one which just calls {@link #handle(PluginContext)}, and
 *	 	</li>
 *	 	<li>
 *	 		one which creates a search or autocomplete handler (according to the {@link #type() type}) with this
 *	 		custom handler
 *	 	</li>
 *	 </ol>
 * </p>
 *
 * <p>
 *	 The first one is relevant, when a custom handler does everything itself. It then must end the
 *	 {@link PluginContext#response() response} in its {@link #handle(PluginContext) handle()} method, and send the
 *	 result.
 * </p>
 *
 * <p>
 *	 In the second case, the respective search or autocomplete handler will use {@link #getQuery(PluginContext)} and
 *	 and {@link #processResult(PluginContext, Object)} to get the actual Elasticsearch query, and process and send the
 *	 final search result respectively. As with {@link #handle(PluginContext) handle()} in the first case,
 *	 {@link #processResult(PluginContext, Object) processResult()} is responsible for ending the
 *	 {@link PluginContext#response()}.
 * </p>
 *
 * <p>
 *	 The default implementation of {@link #handle(PluginContext)} will just call the next handler, and the default
 *	 implementation of {@link #processResult(PluginContext, Object)} will end the response by sending the
 *	 JSON encoded result (which must be a {@link JsonObject}).
 * </p>
 */
public interface CustomHandler {
	/**
	 * The type of query this handler is used for.
	 *
	 * @return The handler type
	 */
	HandlerType type();

	/**
	 * The name used to identify this custom handler.
	 *
	 * @return The name used to identify this custom handler
	 */
	String name();

	/**
	 * Get the Elasticsearch query for this custom handler.
	 * @param context The current plugin context
	 * @return The Elasticsearch query for this handler
	 */
	Single<JsonObject> getQuery(PluginContext context);

	/**
	 * Get the name of the result query to use for loading additional data.
	 *
	 * <p>
	 *	 The result query may be <code>null</code> to indicate, that no additional GraphQL query should
	 *	 be executed.
	 * </p>
	 *
	 * @return The name of the GraphQL query to load additional data
	 */
	String getResultQueryName();

	/**
	 * Postprocess the <code>result</code> and end the {@link PluginContext#response() response} by sending it.
	 *
	 * @param context The current plugin context
	 * @param result The final search result
	 * @return A <code>Completable</code> for the result processing
	 */
	default Completable processResult(PluginContext context, Object result) {
		String response;

		if (result instanceof JsonArray) {
			response = ((JsonArray) result).encode();
		} else {
			response = ((JsonObject) result).encode();
		}

		return Completable.fromAction(() ->
			context.response()
				.setStatusCode(HttpResponseStatus.OK.code())
				.putHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.APPLICATION_JSON_UTF8)
				.end(response));
	}

	/**
	 * Handle the given search request.
	 *
	 * @param context The current plugin context with the search request
	 * @return A <code>Completable</code> for the handler action.
	 */
	default Completable handle(PluginContext context) {
		return Completable.fromAction(context::next);
	}
}
