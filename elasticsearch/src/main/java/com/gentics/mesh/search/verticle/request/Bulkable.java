package com.gentics.mesh.search.verticle.request;

import java.util.List;

public interface Bulkable extends ElasticsearchRequest {
	List<String> toBulkActions();
}
