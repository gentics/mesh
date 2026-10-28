package com.gentics.mesh.plugin.common.model;

import com.gentics.mesh.json.JsonUtil;

/**
 * Interface intended for model classes.
 */
public interface JsonSerializable {
	default String toJson() {
		return JsonUtil.toJson(this);
	}
}
