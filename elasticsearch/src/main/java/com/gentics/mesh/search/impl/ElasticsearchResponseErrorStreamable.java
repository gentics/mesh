package com.gentics.mesh.search.impl;

import java.util.stream.Stream;

/**
 * Provides method access a stream of {@link ElasticsearchResponseError}. Especially bulk request responses may contain multiple errors which can be streamed.
 */
public interface ElasticsearchResponseErrorStreamable {

	/**
	 * Return the stream of errors.
	 * 
	 * @return
	 */
	Stream<ElasticsearchResponseError> stream();
}
