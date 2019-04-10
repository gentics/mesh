package com.gentics.mesh.search.impl;

import java.util.stream.Stream;

public interface ElasticsearchResponseErrorStreamable {
	Stream<ElasticsearchResponseError> stream();
}
