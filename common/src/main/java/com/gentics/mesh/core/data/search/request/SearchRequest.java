package com.gentics.mesh.core.data.search.request;

import com.gentics.mesh.search.SearchProvider;
import io.reactivex.Completable;

import java.util.function.Function;

public interface SearchRequest {
	int requestCount();
	Completable execute(SearchProvider searchProvider);

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
