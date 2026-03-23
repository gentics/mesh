package com.gentics.mesh.core.rest.common;

import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.shareddata.Shareable;

/**
 * Marker interface for all rest models.
 */
@GenerateDocumentation
public interface RestModel extends com.gentics.vertx.openapi.model.RestModel, Shareable {

	default String toJson(boolean minify) {
		return JsonUtil.toJson(this, minify);
	}
}
