package com.gentics.mesh.core.data.search;

import io.vertx.core.json.JsonObject;

import com.gentics.mesh.core.data.MeshVertex;

public interface SearchQueueEntry extends MeshVertex {

	public String getElementUuid();

	SearchQueueEntry setElementUuid(String uuid);

	public String getElementType();

	SearchQueueEntry setElementType(String type);

	public SearchQueueEntryAction getAction();

	SearchQueueEntry setAction(String action);
	
	JsonObject getMessage();

}
