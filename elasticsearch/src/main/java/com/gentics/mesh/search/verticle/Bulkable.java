package com.gentics.mesh.search.verticle;

import java.util.List;

public interface Bulkable extends ElasticSearchRequest {
	List<String> toBulkActions();
}
