package com.gentics.mesh.core.data.search.request;

import java.util.List;

public interface Bulkable extends SearchRequest {
	List<String> toBulkActions();
}
