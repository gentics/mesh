package com.gentics.mesh.plugin.comment.model;

import com.gentics.mesh.json.JsonUtil;

public interface JsonSerializable {
	default String toJson() {
		return JsonUtil.toJson(this);
	}
}
