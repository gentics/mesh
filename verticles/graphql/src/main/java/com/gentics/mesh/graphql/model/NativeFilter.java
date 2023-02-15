package com.gentics.mesh.graphql.model;

import java.util.Optional;

public enum NativeFilter {
	NEVER("never"),
	ONLY("only"),
	IF_POSSIBLE("ifPossible");

	private final String value;

	private NativeFilter(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static final Optional<NativeFilter> parse(String value) {
		return Optional.ofNullable(value).map(text -> {
			switch (text) {
			case "never": return NEVER;
			case "only": return ONLY;
			case "ifPossible": return IF_POSSIBLE;
			default: return null;
			}
		}).flatMap(Optional::ofNullable);
	}
}
