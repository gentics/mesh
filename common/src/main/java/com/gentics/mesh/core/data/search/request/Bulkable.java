package com.gentics.mesh.core.data.search.request;

import io.reactivex.Single;

import java.util.List;

public interface Bulkable extends SearchRequest {
	Single<List<String>> toBulkActions();
}
