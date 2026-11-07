package com.gentics.mesh.plugin.comment.model;

public enum CommentPluginSchema {
	COMMENT("comment"),
	SECTION("section"),
	FOLDER("folder", false);

	private final String name;
	private final boolean hasPrefix;

	CommentPluginSchema(String name) {
		this(name, true);
	}

	CommentPluginSchema(String name, boolean hasPrefix) {
		this.name = name;
		this.hasPrefix = hasPrefix;
	}

	public String getName() {
		return name;
	}

	public boolean hasPrefix() {
		return hasPrefix;
	}
}
