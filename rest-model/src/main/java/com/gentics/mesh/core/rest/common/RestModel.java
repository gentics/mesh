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
	 * Transforms the model into a JSON string, with pretty formatting.
	 * 
	 * @return
	 */
	default String toJson() {
		return toJson(true);
	}

	/**
	 * Transforms the model into a JSON string.
	 * 
	 * @return
	 */
	default String toJson(boolean minify) {
		return JsonUtil.toJson(this, minify);
	}
}
