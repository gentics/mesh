package com.gentics.mesh.core.rest.common;

import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.shareddata.Shareable;

/**
 * Marker interface for all rest models.
 */
@GenerateDocumentation
public interface RestModel extends Shareable {

	/**
	 * Transforms the model into a JSON string.
	 * 
	 * @return
	 */
	default String toJson() {
		return JsonUtil.toJson(this);
	}
}
