package com.gentics.mesh.search.verticle.request;

import java.util.List;

public interface Bulkable extends ElasticSearchRequest {
	List<String> toBulkActions();
}
