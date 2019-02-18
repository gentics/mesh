package com.gentics.mesh.core.data.search.request;

import com.gentics.mesh.search.SearchProvider;
import io.reactivex.Completable;

public interface SearchRequest {
	Completable execute(SearchProvider searchProvider);
}
