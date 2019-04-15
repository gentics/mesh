package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.search.impl.SearchClient;
import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;


public final class RxUtil {
	private static final Logger log = LoggerFactory.getLogger(RxUtil.class);

	private RxUtil() {

	}

	/**
	 * Executes a search request to elasticsearch with the scrolling option. Then continues the scroll and fetches all
	 * documents from the request and returns them in the flow.
	 *
	 * @param client
	 * @param query
	 * @param scrollAge
	 * @param indices
	 * @return
	 */
	public static Flowable<JsonObject> scrollAll(SearchClient client, JsonObject query, String scrollAge, String... indices) {
		return client.searchScroll(query, scrollAge, indices).async()
			.flatMapPublisher(continueScroll(client, scrollAge));
	}

	private static Function<JsonObject, Flowable<JsonObject>> continueScroll(SearchClient client, String scrollAge) {
		return response -> {
			String scrollId = response.getString("_scroll_id");
			if (response.getJsonObject("hits").getJsonArray("hits").isEmpty()) {
				return client.clearScroll(scrollId).async().toCompletable()
					.andThen(Flowable.just(response));
			} else {
				return client.scroll(scrollId, scrollAge).async()
					.flatMapPublisher(continueScroll(client, scrollAge))
					.startWith(response);
			}
		};
	}

	/**
	 * To be used with {@link Flowable#retryWhen(Function)}.
	 *
	 * Resubscribes to the upstream flowable when an error occurs after the given delay. Does so indefinitely
	 * Also logs the number of tries.
	 *
	 * @param delay
	 * @return
	 */
	public static Function<Flowable<Throwable>, Flowable<?>> retryWithDelay(Duration delay) {
		return attempts -> attempts
			.zipWith(
				LongStream.iterate(1, i -> i + 1)::iterator,
				(n, i) -> i
			).doOnNext(i -> log.info("Retry #{} after {}ms", i, delay.toMillis()))
			.delay(delay.toMillis(), TimeUnit.MILLISECONDS);
	}
}
