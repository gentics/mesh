package com.gentics.mesh.core.data.search.request;

import com.gentics.mesh.search.SearchProvider;
import io.reactivex.Completable;
import io.reactivex.functions.Action;

import java.util.function.Function;

import static com.gentics.mesh.util.RxUtil.NOOP;

/**
 * Contains all information to execute a search request to elasticsearch.
 */
public interface SearchRequest {
	/**
	 * The amount of requests that will be executed.
	 * @return
	 */
	int requestCount();

	/**
	 * Executes the request.
	 * @param searchProvider
	 * @return
	 */
	Completable execute(SearchProvider searchProvider);

	/**
	 * An action that will be executed after the request was successful
	 * @return
	 */
	default Action onComplete() {
		return NOOP;
	}

	/**
	 * Creates a new search request from the given function.
	 * @param function
	 * @return
	 */
	static SearchRequest create(Function<SearchProvider, Completable> function) {
		return new SearchRequest() {
			@Override
			public int requestCount() {
				return 1;
			}

			@Override
			public Completable execute(SearchProvider searchProvider) {
				return function.apply(searchProvider);
			}
		};
	}
}
