package com.gentics.mesh.plugin.comment.service;

import com.gentics.mesh.plugin.comment.config.Configuration;
import com.gentics.mesh.plugin.comment.model.CommentPluginSchema;

import javax.inject.Inject;

public class SchemaService {
	private final Configuration configuration;

	@Inject
	public SchemaService(Configuration configuration) {
		this.configuration = configuration;
	}

	public String getMeshSchemaName(CommentPluginSchema schema) {
		if (schema.hasPrefix()) {
			return configuration.getSchemaPrefix() + schema.getName();
		} else {
			return schema.getName();
		}
	}
}
