package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.search.impl.SearchClient;
import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import io.vertx.core.json.JsonObject;


public final class RxUtil {
	private RxUtil() {

	}

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
}
