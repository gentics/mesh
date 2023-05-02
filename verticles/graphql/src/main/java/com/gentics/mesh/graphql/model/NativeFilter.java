package com.gentics.mesh.graphql.model;

import java.util.Optional;

/**
 * GraphQL query parameter for adhoc managing the native query feature.
 * 
 * @author plyhun
 *
 */
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
			switch (text.toLowerCase()) {
			case "never": return NEVER;
			case "only": return ONLY;
			case "ifpossible": return IF_POSSIBLE;
			default: return null;
			}
		}).flatMap(Optional::ofNullable);
	}
}
