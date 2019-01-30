package com.gentics.mesh.search.verticle;

import io.reactivex.Completable;

public interface ElasticSearchRequest {
	Completable execute();
}
